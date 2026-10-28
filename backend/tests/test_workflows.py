"""Evidence for farm invariants, permissions and failure recovery, not implementation mirrors."""

import unittest, tempfile, uuid, json
from pathlib import Path
from datetime import datetime, timezone, timedelta
from fastapi.testclient import TestClient
from app import database
from app.main import app
import app.main as api


class FarmTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(
            dir=Path(__file__).parents[1] / "data"
        )
        database.DATABASE = Path(self.directory.name) / "test.db"
        self.context = TestClient(app)
        self.client = self.context.__enter__()
        response = self.client.post(
            "/auth/setup",
            json={
                "name": "Test owner",
                "username": "owner",
                "password": "test-only-password",
            },
        )
        self.assertEqual(response.status_code, 200)
        self.auth = {"Authorization": "Bearer " + response.json()["token"]}

    def tearDown(self):
        self.context.__exit__(None, None, None)
        self.directory.cleanup()

    def post(self, path, data, request_id=None, auth=None):
        return self.client.post(
            path,
            json=data,
            headers={
                **(auth or self.auth),
                "X-Request-ID": request_id or str(uuid.uuid4()),
            },
        )

    def snapshot(self):
        return self.client.get("/snapshot", headers=self.auth).json()

    def create(self, quantity=12, location="C1-1", request_id=None):
        return self.post(
            "/batches",
            {
                "name": "Client eggs",
                "source": "Farm",
                "quantity": quantity,
                "started": (datetime.now(timezone.utc) - timedelta(days=7)).isoformat(),
                "location": location,
            },
            request_id,
        )

    def group(self):
        return next(g for g in self.snapshot()["groups"] if g["quantity"] > 0)

    def transfer(self, g, destination, quantity=None, row="", request_id=None):
        return self.post(
            "/groups/" + g["id"] + "/transfer",
            {
                "quantity": quantity or g["quantity"],
                "destination": destination,
                "row_name": row,
                "purpose": "Balut production",
                "candidate": "Developing",
                "target": (datetime.now(timezone.utc) + timedelta(days=3)).isoformat(),
                "version": g["version"],
            },
            request_id,
        )

    def test_setup_can_only_happen_once(self):
        self.assertEqual(
            self.client.post(
                "/auth/setup",
                json={
                    "name": "Other",
                    "username": "other",
                    "password": "test-password",
                },
            ).status_code,
            409,
        )

    def test_auth_required(self):
        self.assertEqual(self.client.get("/snapshot").status_code, 401)

    def test_no_password_in_snapshot(self):
        self.assertNotIn("password", self.snapshot()["user"])

    def test_create_retains_original_start(self):
        self.create()
        before = self.snapshot()["batches"][0]["started"]
        self.transfer(self.group(), "C2")
        self.assertEqual(self.snapshot()["batches"][0]["started"], before)

    def test_capacity_collision(self):
        self.create()
        self.assertEqual(self.create().status_code, 409)

    def test_thirty_one_eggs_rejected(self):
        self.assertEqual(self.create(31).status_code, 422)

    def test_zero_eggs_rejected(self):
        self.assertEqual(self.create(0).status_code, 422)

    def test_future_incubation_rejected(self):
        self.assertEqual(
            self.post(
                "/batches",
                {
                    "name": "Future",
                    "quantity": 10,
                    "location": "C1-1",
                    "started": (
                        datetime.now(timezone.utc) + timedelta(days=2)
                    ).isoformat(),
                },
            ).status_code,
            422,
        )

    def test_duplicate_create_retry(self):
        data = {
            "name": "Retry",
            "quantity": 12,
            "location": "C1-1",
            "started": (datetime.now(timezone.utc) - timedelta(days=2)).isoformat(),
        }
        key = str(uuid.uuid4())
        a = self.post("/batches", data, key)
        b = self.post("/batches", data, key)
        self.assertEqual(a.json(), b.json())
        self.assertEqual(len(self.snapshot()["batches"]), 1)

    def test_same_request_id_different_action_rejected(self):
        key = str(uuid.uuid4())
        self.create(request_id=key)
        self.assertEqual(self.create(location="C1-2", request_id=key).status_code, 409)

    def test_skip_scan_stage_rejected(self):
        self.create()
        self.assertEqual(self.transfer(self.group(), "C3-1", 6, "A").status_code, 422)

    def test_split_accounting(self):
        self.create()
        self.transfer(self.group(), "C2")
        self.transfer(self.group(), "C3-1", 6, "A")
        snapshot = self.snapshot()
        self.assertEqual(sum(g["quantity"] for g in snapshot["groups"]), 12)
        self.assertEqual(len(snapshot["groups"]), 2)

    def test_finishing_row_capacity(self):
        self.create()
        self.transfer(self.group(), "C2")
        self.assertEqual(self.transfer(self.group(), "C3-1", 7, "A").status_code, 409)

    def test_stale_group_version_rejected(self):
        self.create()
        g = self.group()
        self.transfer(g, "C2")
        self.assertEqual(self.transfer(g, "C2").status_code, 409)

    def test_partial_outcome_accounting(self):
        self.create()
        g = self.group()
        self.post(
            "/groups/" + g["id"] + "/complete",
            {"quantity": 3, "outcome": "Removed", "version": g["version"]},
        )
        s = self.snapshot()
        self.assertEqual(
            sum(g["quantity"] for g in s["groups"])
            + sum(o["quantity"] for o in s["outcomes"]),
            12,
        )

    def test_harvest_before_finishing_rejected(self):
        self.create()
        g = self.group()
        self.assertEqual(
            self.post(
                "/groups/" + g["id"] + "/complete",
                {"quantity": 1, "outcome": "Harvested", "version": g["version"]},
            ).status_code,
            422,
        )

    def test_archive_active_rejected(self):
        self.create()
        bid = self.snapshot()["batches"][0]["id"]
        self.assertEqual(self.post("/batches/" + bid + "/archive", {}).status_code, 409)

    def test_archive_finished_allowed(self):
        self.create()
        g = self.group()
        self.post(
            "/groups/" + g["id"] + "/complete",
            {"quantity": 12, "outcome": "Removed", "version": g["version"]},
        )
        self.assertEqual(
            self.post("/batches/" + g["batch_id"] + "/archive", {}).status_code, 200
        )

    def test_date_correction_requires_reason(self):
        self.create()
        b = self.snapshot()["batches"][0]
        b["started"] = (datetime.now(timezone.utc) - timedelta(days=8)).isoformat()
        self.assertEqual(
            self.client.patch(
                "/batches/" + b["id"],
                headers={**self.auth, "X-Request-ID": str(uuid.uuid4())},
                json=b,
            ).status_code,
            422,
        )

    def test_operator_cannot_backup_or_add_accounts(self):
        self.post(
            "/users",
            {
                "name": "Operator",
                "username": "operator",
                "password": "test-password",
                "role": "Operator",
            },
        )
        token = self.client.post(
            "/auth/login", json={"username": "operator", "password": "test-password"}
        ).json()["token"]
        auth = {"Authorization": "Bearer " + token}
        self.assertEqual(self.client.get("/backup", headers=auth).status_code, 403)
        self.assertEqual(
            self.post(
                "/users",
                {"name": "Bad", "username": "bad", "password": "test-password"},
                auth=auth,
            ).status_code,
            403,
        )

    def test_deactivated_operator_session_revoked(self):
        uid = self.post(
            "/users",
            {"name": "Operator", "username": "operator", "password": "test-password"},
        ).json()["id"]
        token = self.client.post(
            "/auth/login", json={"username": "operator", "password": "test-password"}
        ).json()["token"]
        self.client.patch("/users/" + uid, headers=self.auth, json={"active": False})
        self.assertEqual(
            self.client.get(
                "/snapshot", headers={"Authorization": "Bearer " + token}
            ).status_code,
            401,
        )

    def test_backup_excludes_credentials(self):
        backup = self.client.get("/backup", headers=self.auth).json()
        self.assertNotIn("users", backup["tables"])
        self.assertNotIn("sessions", backup["tables"])

    def test_bad_restore_rolls_back(self):
        self.create()
        backup = self.client.get("/backup", headers=self.auth).json()
        backup["tables"]["groups"][0]["quantity"] = 2
        self.assertEqual(self.post("/restore", backup).status_code, 422)
        self.assertEqual(self.group()["quantity"], 12)

    def test_backup_restore_roundtrip(self):
        self.create()
        backup = self.client.get("/backup", headers=self.auth).json()
        self.assertEqual(self.post("/restore", backup).status_code, 200)
        self.assertEqual(self.group()["quantity"], 12)

    def test_acknowledge_does_not_complete_eggs(self):
        self.post("/demo/seed", {})
        s = self.snapshot()
        alert = s["alerts"][0]
        active = sum(g["quantity"] for g in s["groups"])
        self.post("/alerts/" + alert["id"] + "/action", {"action": "acknowledge"})
        self.assertEqual(sum(g["quantity"] for g in self.snapshot()["groups"]), active)

    def test_real_adapter_never_reports_simulated_success(self):
        before = api.MODE
        api.MODE = "real"
        try:
            self.assertEqual(
                self.post(
                    "/device/commands", {"command": "Pause turning", "location": "C1-1"}
                ).status_code,
                503,
            )
        finally:
            api.MODE = before

    def test_command_location_checked(self):
        self.assertEqual(
            self.post(
                "/device/commands", {"command": "Pause turning", "location": "C2"}
            ).status_code,
            422,
        )

    def test_logout_revokes_token(self):
        self.post("/auth/logout", {})
        self.assertEqual(
            self.client.get("/snapshot", headers=self.auth).status_code, 401
        )

    def test_carrier_is_reserved_and_released_for_cleaning(self):
        body = {
            "name": "Carrier test",
            "quantity": 6,
            "location": "C1-1",
            "tray_id": "T01",
            "started": (datetime.now(timezone.utc) - timedelta(days=7)).isoformat(),
        }
        self.assertEqual(self.post("/batches", body).status_code, 200)
        self.assertEqual(
            next(t for t in self.snapshot()["trays"] if t["id"] == "T01")["state"],
            "In use",
        )
        self.transfer(self.group(), "C2")
        self.transfer(self.group(), "C3-2", 6, "A")
        self.assertEqual(
            next(t for t in self.snapshot()["trays"] if t["id"] == "T01")["state"],
            "Cleaning",
        )

    def test_in_use_carrier_cannot_be_marked_available(self):
        self.post(
            "/batches",
            {
                "name": "Carrier test",
                "quantity": 6,
                "location": "C1-1",
                "tray_id": "T01",
                "started": (datetime.now(timezone.utc) - timedelta(days=7)).isoformat(),
            },
        )
        self.assertEqual(
            self.client.patch(
                "/trays/T01", json={"state": "Available"}, headers=self.auth
            ).status_code,
            409,
        )

    def test_backup_cannot_mix_operating_modes(self):
        backup = self.client.get("/backup", headers=self.auth).json()
        backup["mode"] = "real"
        self.assertEqual(self.post("/restore", backup).status_code, 422)

    def test_blank_batch_name_rejected(self):
        self.assertEqual(
            self.post(
                "/batches",
                {
                    "name": "   ",
                    "quantity": 6,
                    "location": "C1-1",
                    "started": (
                        datetime.now(timezone.utc) - timedelta(days=7)
                    ).isoformat(),
                },
            ).status_code,
            422,
        )


if __name__ == "__main__":
    unittest.main()
