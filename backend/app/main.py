"""Run: python -m uvicorn app.main:app --host 127.0.0.1 --port 8787."""

import hashlib, json, secrets, time, sqlite3
from contextlib import asynccontextmanager
from datetime import datetime, timezone, timedelta
from fastapi import FastAPI, Depends, HTTPException, Header, Request
from fastapi.responses import JSONResponse
from . import service as farm
from .database import initialize, connect, MODE, rows
from .security import (
    now,
    password_hash,
    password_matches,
    issue_session,
    current_user,
    owner,
    public_user,
)
from .schemas import *


@asynccontextmanager
async def lifespan(app):
    initialize()
    yield


app = FastAPI(title="Balut Incubator API", version="1.0.0", lifespan=lifespan)
login_attempts = {}


@app.exception_handler(sqlite3.IntegrityError)
async def invalid_integrity(request, exc):
    return JSONResponse(
        status_code=409,
        content={
            "detail": "Record conflicts with existing data. Refresh and check quantities or username."
        },
    )


@app.get("/health")
def health():
    with connect() as db:
        return {
            "status": "ok",
            "mode": MODE,
            "api_version": 1,
            "setup_required": db.execute("SELECT COUNT(*) FROM users").fetchone()[0]
            == 0,
            "server_time": now(),
        }


@app.post("/auth/setup")
def setup(data: Account):
    if not data.name.strip() or not data.username.strip():
        raise HTTPException(422, "Name and username cannot be blank.")
    if len(data.password) < 8:
        raise HTTPException(422, "Use a password of at least eight characters.")
    with connect() as db:
        db.execute("BEGIN IMMEDIATE")
        if db.execute("SELECT COUNT(*) FROM users").fetchone()[0]:
            raise HTTPException(409, "Owner already exists. Sign in instead.")
        uid = farm.new_id("USR")
        db.execute(
            "INSERT INTO users(id,username,name,role,password) VALUES(?,?,?,?,?)",
            (
                uid,
                data.username.strip().lower(),
                data.name.strip(),
                "Owner",
                password_hash(data.password),
            ),
        )
        user = dict(db.execute("SELECT * FROM users WHERE id=?", (uid,)).fetchone())
        farm.event(db, user, "Owner created", uid, {})
        return issue_session(db, user)


@app.post("/auth/login")
def login(data: Login, request: Request):
    key = request.client.host if request.client else "local"
    attempts = [t for t in login_attempts.get(key, []) if time.monotonic() - t < 60]
    if len(attempts) >= 8:
        raise HTTPException(429, "Too many attempts. Wait one minute.")
    login_attempts[key] = attempts + [time.monotonic()]
    with connect() as db:
        user = db.execute(
            "SELECT * FROM users WHERE username=? AND active=1",
            (data.username.strip().lower(),),
        ).fetchone()
        if not user or not password_matches(data.password, user["password"]):
            raise HTTPException(401, "Username or password is incorrect.")
        login_attempts[key] = []
        return issue_session(db, dict(user))


@app.post("/auth/logout")
def logout(user=Depends(current_user), authorization: str = Header(default="")):
    with connect() as db:
        db.execute(
            "DELETE FROM sessions WHERE digest=?",
            (
                hashlib.sha256(
                    authorization.removeprefix("Bearer ").encode()
                ).hexdigest(),
            ),
        )
    return {"signed_out": True}


@app.get("/snapshot")
def snapshot(user=Depends(current_user)):
    with connect() as db:
        return farm.snapshot(db, user)


@app.post("/batches")
def create(
    data: BatchInput, user=Depends(current_user), x_request_id: str = Header(default="")
):
    with connect() as db:
        db.execute("BEGIN IMMEDIATE")
        return farm.idempotent(
            db,
            x_request_id,
            user,
            dict(action="create", **data.model_dump()),
            lambda: farm.create_batch(db, user, data),
        )


@app.patch("/batches/{bid}")
def edit(
    bid: str,
    data: BatchEdit,
    user=Depends(current_user),
    x_request_id: str = Header(default=""),
):
    def operation(db):
        batch = db.execute("SELECT * FROM batches WHERE id=?", (bid,)).fetchone()
        if not batch:
            raise HTTPException(404, "Batch not found.")
        if batch["version"] != data.version:
            raise HTTPException(409, "Batch changed. Refresh before editing.")
        started = farm.timestamp(data.started, False)
        if not data.name.strip():
            raise HTTPException(422, "Batch name cannot be blank.")
        if db.execute(
            "SELECT COUNT(*) FROM groups WHERE batch_id=? AND target<>'' AND target<?",
            (bid, started),
        ).fetchone()[0]:
            raise HTTPException(
                422,
                "A review target would precede the corrected start. Adjust that target first.",
            )
        if started != batch["started"] and len(data.reason.strip()) < 3:
            raise HTTPException(422, "Give a reason for correcting the start date.")
        db.execute(
            "UPDATE batches SET name=?,source=?,notes=?,started=?,version=version+1 WHERE id=?",
            (data.name, data.source, data.notes, started, bid),
        )
        farm.event(
            db,
            user,
            "Batch corrected",
            bid,
            dict(before=dict(batch), after=data.model_dump()),
        )
        return {"id": bid}

    with connect() as db:
        db.execute("BEGIN IMMEDIATE")
        return farm.idempotent(
            db,
            x_request_id,
            user,
            dict(action="edit", bid=bid, **data.model_dump()),
            lambda: operation(db),
        )


@app.post("/batches/{bid}/archive")
def archive(bid: str, user=Depends(current_user)):
    with connect() as db:
        if not db.execute("SELECT id FROM batches WHERE id=?", (bid,)).fetchone():
            raise HTTPException(404, "Batch not found.")
        if db.execute(
            "SELECT COUNT(*) FROM groups WHERE batch_id=? AND quantity>0", (bid,)
        ).fetchone()[0]:
            raise HTTPException(409, "Finish or remove active eggs before archiving.")
        db.execute("UPDATE batches SET archived=1,version=version+1 WHERE id=?", (bid,))
        farm.event(db, user, "Batch archived", bid, {})
    return {"archived": True}


@app.post("/groups/{gid}/transfer")
def transfer(
    gid: str,
    data: Transfer,
    user=Depends(current_user),
    x_request_id: str = Header(default=""),
):
    with connect() as db:
        db.execute("BEGIN IMMEDIATE")
        return farm.idempotent(
            db,
            x_request_id,
            user,
            dict(action="transfer", gid=gid, **data.model_dump()),
            lambda: farm.transfer(db, user, gid, data),
        )


@app.post("/groups/{gid}/complete")
def complete(
    gid: str,
    data: Complete,
    user=Depends(current_user),
    x_request_id: str = Header(default=""),
):
    with connect() as db:
        db.execute("BEGIN IMMEDIATE")
        return farm.idempotent(
            db,
            x_request_id,
            user,
            dict(action="complete", gid=gid, **data.model_dump()),
            lambda: farm.complete(db, user, gid, data),
        )


@app.post("/alerts/{aid}/action")
def alert_action(aid: str, data: AlertAction, user=Depends(current_user)):
    with connect() as db:
        row = db.execute("SELECT * FROM alerts WHERE id=?", (aid,)).fetchone()
        if not row:
            raise HTTPException(404, "Alert not found.")
        if data.action == "snooze":
            until = farm.timestamp(data.until)
            if until <= now():
                raise HTTPException(422, "Snooze must end in the future.")
            db.execute("UPDATE alerts SET snoozed_until=? WHERE id=?", (until, aid))
        else:
            db.execute(
                "UPDATE alerts SET state=?,acknowledged_by=?,acknowledged_at=? WHERE id=?",
                (
                    "acknowledged" if data.action == "acknowledge" else "resolved",
                    user["username"],
                    now(),
                    aid,
                ),
            )
        farm.event(db, user, "Alert " + data.action, aid, data.model_dump())
    return {"saved": True}


@app.post("/device/commands")
def command(
    data: Command, user=Depends(current_user), x_request_id: str = Header(default="")
):
    if MODE != "simulation":
        raise HTTPException(
            503, "Real hardware adapter is unavailable. No command was executed."
        )
    if ("Candling" in data.command) != (data.location == "C2"):
        raise HTTPException(
            422, "Choose C2 for candling and an incubation level for turning."
        )

    def operation(db):
        cid = farm.new_id("CMD")
        if "turning" in data.command:
            db.execute(
                "INSERT OR REPLACE INTO settings VALUES(?,?)",
                (
                    "turning-" + data.location,
                    "Paused" if data.command.startswith("Pause") else "Scheduled",
                ),
            )
        farm.event(db, user, "Simulated command", cid, data.model_dump())
        return {
            "id": cid,
            "state": "completed",
            "simulation": True,
            "message": "Simulation completed. No hardware was operated.",
        }

    with connect() as db:
        db.execute("BEGIN IMMEDIATE")
        return farm.idempotent(
            db,
            x_request_id,
            user,
            dict(action="command", **data.model_dump()),
            lambda: operation(db),
        )


@app.get("/users")
def users(user=Depends(current_user)):
    owner(user)
    with connect() as db:
        return [public_user(u) for u in rows(db, "users")]


@app.post("/users")
def add_user(data: Account, user=Depends(current_user)):
    owner(user)
    if not data.name.strip() or not data.username.strip():
        raise HTTPException(422, "Name and username cannot be blank.")
    if len(data.password) < 8:
        raise HTTPException(422, "Use at least eight password characters.")
    if data.role != "Operator":
        raise HTTPException(422, "Additional accounts are Operators.")
    with connect() as db:
        uid = farm.new_id("USR")
        db.execute(
            "INSERT INTO users(id,username,name,role,password) VALUES(?,?,?,?,?)",
            (
                uid,
                data.username.strip().lower(),
                data.name,
                "Operator",
                password_hash(data.password),
            ),
        )
        farm.event(db, user, "Operator created", uid, {"username": data.username})
        return {"id": uid}


@app.patch("/users/{uid}")
def user_state(uid: str, data: UserState, user=Depends(current_user)):
    owner(user)
    with connect() as db:
        target = db.execute("SELECT * FROM users WHERE id=?", (uid,)).fetchone()
        if not target:
            raise HTTPException(404, "User not found.")
        if target["role"] == "Owner":
            raise HTTPException(422, "Owner cannot be deactivated.")
        db.execute("UPDATE users SET active=? WHERE id=?", (int(data.active), uid))
        db.execute("DELETE FROM sessions WHERE user_id=?", (uid,))
        farm.event(db, user, "Operator access changed", uid, {"active": data.active})
    return {"saved": True}


@app.patch("/profile")
def profile(data: Profile, user=Depends(current_user)):
    if not data.name.strip():
        raise HTTPException(422, "Display name cannot be blank.")
    with connect() as db:
        db.execute("UPDATE users SET name=? WHERE id=?", (data.name, user["id"]))
        if data.new_password:
            if len(data.new_password) < 8 or not password_matches(
                data.current_password, user["password"]
            ):
                raise HTTPException(
                    422, "Current password is incorrect or new password is too short."
                )
            db.execute(
                "UPDATE users SET password=? WHERE id=?",
                (password_hash(data.new_password), user["id"]),
            )
            db.execute("DELETE FROM sessions WHERE user_id=?", (user["id"],))
        farm.event(
            db,
            user,
            "Profile updated",
            user["id"],
            {"name": data.name, "password_changed": bool(data.new_password)},
        )
    return {"saved": True, "sign_in_required": bool(data.new_password)}


@app.patch("/settings/farm")
def settings(data: Farm, user=Depends(current_user)):
    owner(user)
    with connect() as db:
        db.execute("UPDATE settings SET value=? WHERE key='farm_name'", (data.name,))
        farm.event(db, user, "Farm name changed", "settings", {"name": data.name})
    return {"saved": True}


@app.get("/backup")
def backup(user=Depends(current_user)):
    owner(user)
    with connect() as db:
        return farm.backup(db)


@app.post("/trays")
def add_tray(data: TrayInput, user=Depends(current_user)):
    owner(user)
    with connect() as db:
        db.execute("INSERT INTO trays VALUES(?,'Available','')", (data.id.upper(),))
        farm.event(db, user, "Physical tray registered", data.id.upper(), {})
    return {"saved": True}


@app.patch("/trays/{tid}")
def tray_state(tid: str, data: TrayState, user=Depends(current_user)):
    with connect() as db:
        tray = db.execute("SELECT * FROM trays WHERE id=?", (tid,)).fetchone()
        if not tray:
            raise HTTPException(404, "Tray not found.")
        if tray["group_id"]:
            raise HTTPException(
                409,
                "This tray is assigned to active eggs. Finish its transfer/removal first.",
            )
        db.execute("UPDATE trays SET state=? WHERE id=?", (data.state, tid))
        farm.event(db, user, "Tray state changed", tid, {"state": data.state})
    return {"saved": True}


@app.post("/restore")
def restore(data: dict, user=Depends(current_user)):
    owner(user)
    with connect() as db:
        db.execute("BEGIN IMMEDIATE")
        return farm.restore(db, data, user)


@app.post("/demo/seed")
def seed(user=Depends(current_user)):
    owner(user)
    if MODE != "simulation":
        raise HTTPException(403, "Sample data is only available in simulation.")
    with connect() as db:
        db.execute("BEGIN IMMEDIATE")
        if db.execute("SELECT COUNT(*) FROM batches").fetchone()[0]:
            raise HTTPException(409, "Only seed an empty simulation database.")
        for name, loc, days in [
            ("River flock", "C1-1", 3),
            ("Morning collection", "C1-2", 1),
        ]:
            farm.create_batch(
                db,
                user,
                BatchInput(
                    name=name,
                    source="Demonstration farm",
                    quantity=30,
                    started=(
                        datetime.now(timezone.utc) - timedelta(days=days)
                    ).isoformat(),
                    location=loc,
                    notes="Sample record — not a client batch.",
                ),
            )
        bid = farm.new_id("BT")
        started = (datetime.now(timezone.utc) - timedelta(days=18)).isoformat()
        db.execute(
            "INSERT INTO batches(id,name,source,total,started,notes,created) VALUES(?,?,?,?,?,?,?)",
            (
                bid,
                "Finishing sample",
                "Demonstration farm",
                12,
                started,
                "Sample record — not a client batch.",
                now(),
            ),
        )
        for row, purpose, delta in [
            ("A", "Balut production", -1),
            ("B", "Duckling hatching", 8),
        ]:
            gid = farm.new_id("GR")
            db.execute(
                "INSERT INTO groups(id,batch_id,quantity,location,row_name,purpose,candidate,target) VALUES(?,?,?,?,?,?,?,?)",
                (
                    gid,
                    bid,
                    6,
                    "C3-1",
                    row,
                    purpose,
                    "Developing",
                    (datetime.now(timezone.utc) + timedelta(days=delta)).isoformat(),
                ),
            )
        farm.event(db, user, "Simulation samples created", "demo", {})
    return {"seeded": True}
