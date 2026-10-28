"""Farm rules independent of HTTP and Android screens."""

import hashlib, json, uuid, math, time
from datetime import datetime, timezone, timedelta
from fastapi import HTTPException
from .security import now
from .database import MODE, rows

LOCATIONS = ["C1-1", "C1-2", "C2", "C3-1", "C3-2", "C3-3"]


def new_id(prefix):
    return prefix + "-" + uuid.uuid4().hex[:10].upper()


def timestamp(value, future_allowed=True):
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
        if parsed.tzinfo is None:
            raise ValueError()
        if not future_allowed and parsed > datetime.now(timezone.utc):
            raise ValueError()
        return parsed.astimezone(timezone.utc).isoformat(timespec="seconds")
    except ValueError:
        raise HTTPException(
            422,
            "Use a valid date with timezone; incubation start cannot be in the future.",
        )


def event(db, user, action, entity, detail):
    db.execute(
        "INSERT INTO events VALUES(?,?,?,?,?,?)",
        (new_id("EV"), now(), user["username"], action, entity, json.dumps(detail)),
    )


def get_group(db, gid, version):
    row = db.execute(
        "SELECT * FROM groups WHERE id=? AND quantity>0", (gid,)
    ).fetchone()
    if not row:
        raise HTTPException(404, "This active egg group no longer exists.")
    if row["version"] != version:
        raise HTTPException(
            409, "This record changed. Refresh and review before saving."
        )
    return dict(row)


def capacity(db, location, quantity, row_name="", exclude=""):
    if location not in LOCATIONS:
        raise HTTPException(422, "Unknown location.")
    active = list(
        db.execute(
            "SELECT * FROM groups WHERE location=? AND quantity>0 AND id<>?",
            (location, exclude),
        )
    )
    if sum(g["quantity"] for g in active) + quantity > 30:
        raise HTTPException(409, "This location can hold at most 30 eggs.")
    if not location.startswith("C3") and active:
        raise HTTPException(409, "This tray position is already occupied.")
    if location.startswith("C3"):
        if not row_name:
            raise HTTPException(422, "Choose row A to E for a finishing group.")
        if (
            sum(g["quantity"] for g in active if g["row_name"] == row_name) + quantity
            > 6
        ):
            raise HTTPException(
                409, "A finishing row can hold at most six eggs. Split into more rows."
            )


def idempotent(db, key, user, payload, operation):
    if not key or len(key) > 100:
        raise HTTPException(400, "A unique request ID is required.")
    fingerprint = hashlib.sha256(
        json.dumps(payload, sort_keys=True).encode()
    ).hexdigest()
    prior = db.execute("SELECT * FROM requests WHERE id=?", (key,)).fetchone()
    if prior:
        if prior["actor"] != user["id"] or prior["fingerprint"] != fingerprint:
            raise HTTPException(409, "Request ID reused for a different action.")
        return json.loads(prior["response"])
    response = operation()
    db.execute(
        "INSERT INTO requests VALUES(?,?,?,?)",
        (key, user["id"], fingerprint, json.dumps(response)),
    )
    return response


def refresh_alerts(db):
    for g in db.execute(
        "SELECT groups.*,batches.name FROM groups JOIN batches ON batches.id=groups.batch_id WHERE quantity>0 AND target<>''"
    ):
        aid = "review-" + g["id"] + "-" + g["target"]
        db.execute(
            "INSERT OR IGNORE INTO alerts(id,group_id,title,due) VALUES(?,?,?,?)",
            (aid, g["id"], g["name"] + " — scheduled review", g["target"]),
        )
    db.execute(
        "UPDATE alerts SET state='resolved' WHERE group_id NOT IN (SELECT id FROM groups WHERE quantity>0)"
    )
    db.execute(
        "UPDATE alerts SET state='resolved' WHERE group_id IN (SELECT id FROM groups WHERE target<>alerts.due)"
    )


def device_status(db):
    supported = MODE == "simulation"
    readings = []
    for i, name in enumerate(["C1", "C2", "C3"]):
        readings.append(
            {
                "compartment": name,
                "temperature": round(37.5 + 0.12 * math.sin(time.time() / 25 + i), 2)
                if supported and name != "C2"
                else None,
                "humidity": round(
                    (63 if name == "C3" else 52) + math.sin(time.time() / 30), 1
                )
                if supported and name != "C2"
                else None,
                "at": now() if supported else None,
                "state": "Simulated" if supported else "Hardware unavailable",
            }
        )
    if supported:
        latest = db.execute("SELECT MAX(at) FROM telemetry").fetchone()[0]
        if (
            not latest
            or (
                datetime.now(timezone.utc) - datetime.fromisoformat(latest)
            ).total_seconds()
            >= 30
        ):
            for r in readings:
                if r["temperature"] is not None:
                    db.execute(
                        "INSERT INTO telemetry(compartment,temperature,humidity,at) VALUES(?,?,?,?)",
                        (r["compartment"], r["temperature"], r["humidity"], r["at"]),
                    )
            db.execute(
                "DELETE FROM telemetry WHERE id NOT IN (SELECT id FROM telemetry ORDER BY id DESC LIMIT 2880)"
            )
    return {
        "mode": MODE,
        "connected": supported,
        "readings": readings,
        "at": now(),
        "capabilities": {
            "turning": supported,
            "candling_light": supported,
            "ml": False,
        },
        "turning": {
            loc: db.execute(
                "SELECT value FROM settings WHERE key=?", ("turning-" + loc,)
            ).fetchone()[0]
            if db.execute(
                "SELECT value FROM settings WHERE key=?", ("turning-" + loc,)
            ).fetchone()
            else "Scheduled"
            for loc in LOCATIONS
            if loc != "C2"
        },
        "note": "Simulated readings only. No physical device is controlled."
        if supported
        else "Real hardware adapter has not been installed.",
    }


def snapshot(db, user):
    refresh_alerts(db)
    device = device_status(db)
    return {
        "mode": MODE,
        "server_time": now(),
        "user": {k: user[k] for k in ("id", "username", "name", "role", "active")},
        "farm_name": db.execute(
            "SELECT value FROM settings WHERE key='farm_name'"
        ).fetchone()[0],
        "batches": rows(db, "batches"),
        "groups": rows(db, "groups"),
        "outcomes": rows(db, "outcomes"),
        "events": [
            dict(r)
            for r in db.execute(
                "SELECT * FROM events ORDER BY at DESC,rowid DESC LIMIT 1000"
            )
        ],
        "alerts": rows(db, "alerts"),
        "trays": rows(db, "trays"),
        "device": device,
        "telemetry": [
            dict(r)
            for r in db.execute(
                "SELECT * FROM (SELECT * FROM telemetry ORDER BY id DESC LIMIT 120) ORDER BY id"
            )
        ],
    }


def create_batch(db, user, data):
    if not data.name.strip():
        raise HTTPException(422, "Batch name cannot be blank.")
    started = timestamp(data.started, False)
    capacity(db, data.location, data.quantity)
    bid = new_id("BT")
    gid = new_id("GR")
    if data.tray_id:
        tray = db.execute("SELECT * FROM trays WHERE id=?", (data.tray_id,)).fetchone()
        if not tray or tray["state"] != "Available":
            raise HTTPException(409, "Choose an available physical tray.")
    db.execute(
        "INSERT INTO batches(id,name,source,total,started,notes,created) VALUES(?,?,?,?,?,?,?)",
        (
            bid,
            data.name.strip(),
            data.source.strip(),
            data.quantity,
            started,
            data.notes,
            now(),
        ),
    )
    db.execute(
        "INSERT INTO groups(id,batch_id,quantity,location,row_name,purpose,candidate,target) VALUES(?,?,?,?,?,?,?,?)",
        (gid, bid, data.quantity, data.location, "", "Unassigned", "Unreviewed", ""),
    )
    if data.tray_id:
        db.execute(
            "UPDATE trays SET state='In use',group_id=? WHERE id=?", (gid, data.tray_id)
        )
    event(db, user, "Batch registered", bid, data.model_dump())
    return {"id": bid, "group_id": gid}


def transfer(db, user, gid, data):
    group = get_group(db, gid, data.version)
    if data.quantity > group["quantity"]:
        raise HTTPException(409, "Quantity exceeds the active group.")
    source = group["location"]
    dest = data.destination
    if source.startswith("C1") and dest != "C2":
        raise HTTPException(422, "Move the initial tray to C2 for review first.")
    if source == "C2" and not dest.startswith("C3"):
        raise HTTPException(422, "C2 groups move to a finishing level.")
    if source.startswith("C3") and not dest.startswith("C3"):
        raise HTTPException(
            422, "Finishing groups can only move between finishing positions."
        )
    if dest.startswith("C3") and data.candidate == "Unreviewed":
        raise HTTPException(
            422, "Record a human review decision before finishing assignment."
        )
    if dest.startswith("C3") and (data.purpose == "Unassigned" or not data.target):
        raise HTTPException(422, "Choose the purpose and scheduled review target.")
    target = timestamp(data.target) if data.target else ""
    batch = db.execute(
        "SELECT started FROM batches WHERE id=?", (group["batch_id"],)
    ).fetchone()
    if target and target < batch["started"]:
        raise HTTPException(
            422, "Review target must be after original incubation start."
        )
    capacity(
        db,
        dest,
        data.quantity,
        data.row_name,
        exclude=gid if data.quantity == group["quantity"] else "",
    )
    if data.quantity == group["quantity"]:
        target_id = gid
        db.execute(
            "UPDATE groups SET location=?,row_name=?,purpose=?,candidate=?,target=?,version=version+1 WHERE id=?",
            (dest, data.row_name, data.purpose, data.candidate, target, gid),
        )
    else:
        db.execute(
            "UPDATE groups SET quantity=quantity-?,version=version+1 WHERE id=?",
            (data.quantity, gid),
        )
        target_id = new_id("GR")
        db.execute(
            "INSERT INTO groups(id,batch_id,quantity,location,row_name,purpose,candidate,target) VALUES(?,?,?,?,?,?,?,?)",
            (
                target_id,
                group["batch_id"],
                data.quantity,
                dest,
                data.row_name,
                data.purpose,
                data.candidate,
                target,
            ),
        )
    if source == "C2" and data.quantity == group["quantity"]:
        db.execute(
            "UPDATE trays SET state='Cleaning',group_id='' WHERE group_id=?", (gid,)
        )
    event(
        db,
        user,
        "Manual transfer / review",
        group["batch_id"],
        dict(
            source=source, group=gid, destination_group=target_id, **data.model_dump()
        ),
    )
    return {"id": target_id}


def complete(db, user, gid, data):
    group = get_group(db, gid, data.version)
    if data.quantity > group["quantity"]:
        raise HTTPException(409, "Quantity exceeds the active group.")
    if not group["location"].startswith("C3") and data.outcome in (
        "Harvested",
        "Hatched",
    ):
        raise HTTPException(
            422, "Record finishing in C3 before harvest/hatch completion."
        )
    db.execute(
        "UPDATE groups SET quantity=quantity-?,version=version+1 WHERE id=?",
        (data.quantity, gid),
    )
    if data.quantity == group["quantity"]:
        db.execute(
            "UPDATE trays SET state='Cleaning',group_id='' WHERE group_id=?", (gid,)
        )
    oid = new_id("OUT")
    db.execute(
        "INSERT INTO outcomes VALUES(?,?,?,?,?,?)",
        (oid, group["batch_id"], gid, data.quantity, data.outcome, now()),
    )
    event(db, user, "Outcome recorded", group["batch_id"], data.model_dump())
    return {"id": oid}


def backup(db):
    return {
        "schema_version": 1,
        "mode": MODE,
        "created": now(),
        "tables": {
            t: rows(db, t)
            for t in [
                "batches",
                "groups",
                "outcomes",
                "events",
                "alerts",
                "settings",
                "trays",
            ]
        },
    }


def restore(db, data, user):
    if data.get("schema_version") != 1 or data.get("mode") != MODE:
        raise HTTPException(422, "Backup version or operating mode differs.")
    tables = data.get("tables", {})
    expected = [
        "batches",
        "groups",
        "outcomes",
        "events",
        "alerts",
        "settings",
        "trays",
    ]
    if set(tables) != set(expected):
        raise HTTPException(422, "Backup tables are incomplete.")
    for t in reversed(expected):
        db.execute("DELETE FROM " + t)
    # Groups reference batches; insertion order is intentional.
    for table in expected:
        columns = [r["name"] for r in db.execute("PRAGMA table_info(" + table + ")")]
        for row in tables[table]:
            if set(row) != set(columns):
                raise HTTPException(422, "Invalid backup record fields.")
            db.execute(
                "INSERT INTO "
                + table
                + " ("
                + ",".join(columns)
                + ") VALUES("
                + ",".join("?" for _ in columns)
                + ")",
                [row[c] for c in columns],
            )
    for batch in rows(db, "batches"):
        active = db.execute(
            "SELECT COALESCE(SUM(quantity),0) FROM groups WHERE batch_id=?",
            (batch["id"],),
        ).fetchone()[0]
        done = db.execute(
            "SELECT COALESCE(SUM(quantity),0) FROM outcomes WHERE batch_id=?",
            (batch["id"],),
        ).fetchone()[0]
        if active + done != batch["total"] or batch["total"] <= 0:
            raise HTTPException(422, "Backup quantities do not reconcile.")
        timestamp(batch["started"], False)
    for loc in LOCATIONS:
        groups = [
            g for g in rows(db, "groups") if g["location"] == loc and g["quantity"] > 0
        ]
        if sum(g["quantity"] for g in groups) > 30 or (
            not loc.startswith("C3") and len(groups) > 1
        ):
            raise HTTPException(422, "Backup exceeds location capacity.")
        for row in "ABCDE":
            if sum(g["quantity"] for g in groups if g["row_name"] == row) > 6:
                raise HTTPException(422, "Backup exceeds row capacity.")
    for g in rows(db, "groups"):
        if g["location"] not in LOCATIONS or (
            g["location"].startswith("C3")
            and g["row_name"] not in ("A", "B", "C", "D", "E")
        ):
            raise HTTPException(422, "Backup contains an invalid location.")
    for tray in rows(db, "trays"):
        if tray["state"] not in ("Available", "In use", "Cleaning", "Unavailable"):
            raise HTTPException(422, "Invalid tray state.")
        if (
            tray["state"] == "In use"
            and not db.execute(
                "SELECT id FROM groups WHERE id=? AND quantity>0 AND location IN ('C1-1','C1-2','C2')",
                (tray["group_id"],),
            ).fetchone()
        ):
            raise HTTPException(422, "Tray assignment references an invalid group.")
    db.execute("DELETE FROM telemetry")
    marker = db.execute(
        "SELECT value FROM settings WHERE key='database_mode'"
    ).fetchone()
    if (
        not marker
        or marker[0] != MODE
        or not db.execute("SELECT value FROM settings WHERE key='farm_name'").fetchone()
    ):
        raise HTTPException(
            422, "Backup farm settings or database mode marker is invalid."
        )
    db.execute("DELETE FROM requests")
    event(db, user, "Backup restored", "system", {"created": data["created"]})
    return {"restored": True}
