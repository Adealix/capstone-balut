"""Password hashing and expiring opaque sessions; no bundled credentials."""

import hashlib
import hmac
import secrets
from datetime import datetime, timezone, timedelta
from fastapi import Header, HTTPException
from .database import connect


def now():
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def password_hash(password):
    salt = secrets.token_hex(16)
    value = hashlib.pbkdf2_hmac(
        "sha256", password.encode(), bytes.fromhex(salt), 600_000
    ).hex()
    return salt + ":" + value


def password_matches(password, stored):
    salt, value = stored.split(":")
    actual = hashlib.pbkdf2_hmac(
        "sha256", password.encode(), bytes.fromhex(salt), 600_000
    ).hex()
    return hmac.compare_digest(actual, value)


def issue_session(db, user):
    token = secrets.token_urlsafe(32)
    expires = (datetime.now(timezone.utc) + timedelta(days=7)).isoformat(
        timespec="seconds"
    )
    db.execute(
        "INSERT INTO sessions VALUES(?,?,?)",
        (hashlib.sha256(token.encode()).hexdigest(), user["id"], expires),
    )
    return {"token": token, "user": public_user(user), "expires": expires}


def public_user(user):
    return {k: user[k] for k in ("id", "username", "name", "role", "active")}


def current_user(authorization: str = Header(default="")):
    token = authorization.removeprefix("Bearer ")
    with connect() as db:
        user = db.execute(
            "SELECT users.* FROM sessions JOIN users ON users.id=sessions.user_id WHERE digest=? AND expires>? AND active=1",
            (hashlib.sha256(token.encode()).hexdigest(), now()),
        ).fetchone()
        if not user:
            raise HTTPException(401, "Your session expired. Sign in again.")
        return dict(user)


def owner(user):
    if user["role"] != "Owner":
        raise HTTPException(403, "Only the Owner can do this.")
