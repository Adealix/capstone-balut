"""Small SQLite database. Each request uses one transaction and one connection."""

import os
import sqlite3
from pathlib import Path
from contextlib import contextmanager

MODE = os.getenv("BALUT_MODE", "simulation")
DATABASE = Path(os.getenv("BALUT_DATABASE", "data/" + MODE + ".sqlite3"))
if MODE not in ("simulation", "real"):
    raise RuntimeError("BALUT_MODE must be simulation or real")


@contextmanager
def connect():
    DATABASE.parent.mkdir(parents=True, exist_ok=True)
    db = sqlite3.connect(DATABASE, timeout=15)
    db.row_factory = sqlite3.Row
    db.execute("PRAGMA foreign_keys=ON")
    try:
        yield db
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


def initialize():
    with connect() as db:
        db.executescript("""
        CREATE TABLE IF NOT EXISTS users(id TEXT PRIMARY KEY, username TEXT UNIQUE NOT NULL,
          name TEXT NOT NULL, role TEXT NOT NULL, password TEXT NOT NULL, active INTEGER DEFAULT 1);
        CREATE TABLE IF NOT EXISTS sessions(digest TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id), expires TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS batches(id TEXT PRIMARY KEY, name TEXT NOT NULL, source TEXT NOT NULL,
          total INTEGER NOT NULL, started TEXT NOT NULL, notes TEXT NOT NULL, version INTEGER NOT NULL DEFAULT 1,
          archived INTEGER NOT NULL DEFAULT 0, created TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS groups(id TEXT PRIMARY KEY, batch_id TEXT NOT NULL REFERENCES batches(id),
          quantity INTEGER NOT NULL CHECK(quantity>=0), location TEXT NOT NULL, row_name TEXT NOT NULL,
          purpose TEXT NOT NULL, candidate TEXT NOT NULL, target TEXT NOT NULL, version INTEGER NOT NULL DEFAULT 1);
        CREATE TABLE IF NOT EXISTS outcomes(id TEXT PRIMARY KEY, batch_id TEXT NOT NULL REFERENCES batches(id),
          group_id TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity>0), outcome TEXT NOT NULL, at TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS events(id TEXT PRIMARY KEY, at TEXT NOT NULL, actor TEXT NOT NULL,
          action TEXT NOT NULL, entity TEXT NOT NULL, detail TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS alerts(id TEXT PRIMARY KEY, group_id TEXT NOT NULL, title TEXT NOT NULL,
          due TEXT NOT NULL, state TEXT NOT NULL DEFAULT 'open', snoozed_until TEXT NOT NULL DEFAULT '',
          acknowledged_by TEXT NOT NULL DEFAULT '', acknowledged_at TEXT NOT NULL DEFAULT '');
        CREATE TABLE IF NOT EXISTS settings(key TEXT PRIMARY KEY, value TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS requests(id TEXT PRIMARY KEY, actor TEXT NOT NULL, fingerprint TEXT NOT NULL,
          response TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS telemetry(id INTEGER PRIMARY KEY AUTOINCREMENT, compartment TEXT NOT NULL,
          temperature REAL NOT NULL, humidity REAL NOT NULL, at TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS trays(id TEXT PRIMARY KEY, state TEXT NOT NULL, group_id TEXT NOT NULL DEFAULT '');
        """)
        db.execute("INSERT OR IGNORE INTO settings VALUES('farm_name','Balut farm')")
        marker = db.execute(
            "SELECT value FROM settings WHERE key='database_mode'"
        ).fetchone()
        if marker and marker[0] != MODE:
            raise RuntimeError("Cannot open a database from a different operating mode")
        db.execute("INSERT OR IGNORE INTO settings VALUES('database_mode',?)", (MODE,))
        for code in ("T01", "T02", "T03"):
            db.execute("INSERT OR IGNORE INTO trays VALUES(?,'Available','')", (code,))


def rows(db, table):
    # Table names are internal constants, never user input.
    return [dict(r) for r in db.execute("SELECT * FROM " + table)]
