# API and integration guide

Local development API: `http://127.0.0.1:8787`; emulator address: `http://10.0.2.2:8787`. Start the service from `backend/` or use `tools/Start-Backend.ps1`. Generated OpenAPI is in `openapi.json`; interactive docs are at `/docs`.

## Authentication

`GET /health` reports mode, server time, API version and whether first-Owner setup is required. `POST /auth/setup` is accepted only while no accounts exist. Then use `POST /auth/login`; subsequent requests send `Authorization: Bearer <opaque token>`. Tokens expire after seven days. Logout deletes the token, disabling an Operator revokes their sessions, and changing a password revokes that user's sessions. The app encrypts its token using Android Keystore. Never put live tokens in source code, screenshots or reports.

The Owner creates Operators. Operators can perform routine record/assignment/review operations. Owner-only routes include users, farm settings, registering carrier IDs, backup and restore. The backend, not the screen's visibility, enforces permissions.

## Core routes

| Method / route | Meaning |
|---|---|
| GET `/snapshot` | Farm, authenticated user, batches, groups, outcomes, trays, alerts, latest 1,000 events, capabilities, current readings and last 120 telemetry samples |
| POST `/batches` | Register 1–30 eggs in C1-1 or C1-2; optionally reserve an available physical tray |
| PATCH `/batches/{id}` | Correct name/source/notes/start with matching version and a reason for start-date corrections |
| POST `/batches/{id}/archive` | Archive only when active quantity is zero |
| POST `/groups/{id}/transfer` | Move or split an active group; preserve batch/start; require row/purpose/human assessment/target for C3 |
| POST `/groups/{id}/complete` | Record a partial or full actual outcome; decrement active quantity |
| POST `/alerts/{id}/action` | Acknowledge, snooze until timestamp, or resolve a review reminder |
| POST `/device/commands` | Capability-limited simulated level-turning or C2-light command; real adapter absent => 503 |
| GET/POST `/users`, PATCH `/users/{id}` | Owner-managed Operator accounts and access |
| PATCH `/profile` | Display name or password change |
| PATCH `/settings/farm` | Owner changes farm display name |
| POST `/trays`, PATCH `/trays/{id}` | Register a physical carrier / update cleaning or availability state |
| GET `/backup`, POST `/restore` | Owner-only schema-versioned operational backup and transactional restore |
| POST `/demo/seed` | Owner only; simulation only; empty databases only |

## Retry and conflict handling

Registration, batch correction, transfer, outcome and device-command requests require a unique `X-Request-ID`. Keep the same ID **and payload** if retrying an uncertain response. IDs are bound to the authenticated account and payload; reuse for a different action returns 409. The transaction serializes capacity checking and mutation. Group/batch edits also require the current `version`; stale versions return 409 and must be refreshed before review/retry.

The phone's offline outbox holds registrations only. A pending record does not reserve a compartment or tray. The server checks current capacity when it finally receives the registration. Never automatically replay stale hardware commands after reconnecting.

## Time and quantities

All API timestamps must have an offset. The server normalizes to UTC; Android form/display time is Asia/Manila. The saved incubation time is Day 0. Transfers do not restart age. Targets are operator-selected human review times, not automatically inferred biological readiness.

Every batch satisfies `registered = active + outcomes/removals`. Initial/candling positions accept one active carrier group each; C3 levels accept up to 30 eggs distributed over rows A–E, six eggs per row. A C2 carrier becomes Cleaning after its last eggs are assigned out of it. Physical tray IDs and logical positions are separate concepts.

## Mode boundaries and hardware

`BALUT_MODE=simulation` uses a separate simulation database, clearly labeled sinusoidal demonstration readings and completed **simulated** command responses. It never calls GPIO. `BALUT_MODE=real` reports unavailable readings and no hardware capabilities; commands return 503. A database stores its mode marker to prevent accidentally starting real mode against the simulation file.

The hardware adapter must eventually provide authoritative timestamps, valid/invalid sensor quality, feedback/acknowledgment, independent control capabilities and error states. Do not enable independent row commands if rows share a motor. Device safety/interlocks and incubation scheduling must run independently of the phone. Real LAN deployment needs trusted TLS, device provisioning, access restrictions and actual hardware verification.

ML is not exposed as an implemented prediction endpoint. Future scan records should include scan ID, batch/tray ID, image reference, capture time, position A1–E6, candidate scores/model version, quality flags and the independent human decision. Presence/hand handling and abnormal-class accuracy are still unvalidated.

## Error meanings

- 401: sign in again; cached data does not authorize new server changes.
- 403: current role or operating mode cannot perform the action.
- 409: capacity, duplicate username/request ID, stale version, or record conflict.
- 422: invalid fields, dates, quantities or stage transition.
- 503: real hardware operation unavailable; no physical action was performed.

Backups deliberately exclude accounts/sessions and recorded telemetry; restore is operational-record replacement, not account recovery. The backend currently retains up to 2,880 telemetry samples and returns a shorter window to the mobile app. Event history in the snapshot is bounded, while a full operational backup retains all recorded events.
