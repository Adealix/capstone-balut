# Code map for students

## Android app

Open `frontend/app/src/main/java/com/capstone/balut` in Android Studio.

| File/folder | Responsibility |
|---|---|
| `MainActivity.java` | Five-tab navigation, visible connection banner, refresh lifecycle and file picker results |
| `BalutApplication.java` | Creates one shared repository for the application |
| `screens/auth/AuthActivity.java` | First Owner setup, login and connection selection |
| `screens/home/HomeFragment.java` | Daily work and summaries |
| `screens/dashboard/` | Compartment readings, occupancy, supported controls and recorded trend |
| `screens/scan/ScanFragment.java` | Explicit ML placeholder and A1–E6 tray map |
| `screens/batches/` | Registration, list/search/filter, details, corrections, transfers and outcomes |
| `screens/alerts/` | Acknowledge, snooze and resolve shared reminders |
| `screens/profile/` | Profile, settings, accounts, trays, reports, activity and help |
| `data/repositories/FarmRepository.java` | Screen-facing requests, cache, session and outbox orchestration |
| `data/remote/PiApiClient.java` | HTTP requests on the repository's background executor |
| `data/local/` | SQLite phone cache/outbox and Keystore-encrypted session storage |
| `models/BatchRecord.java` | Simple batch read model |
| `logic/IncubationCalculator.java` | Explicit date formatting and elapsed-day calculation |
| `notifications/` | Local reminder scheduling, delivery and reboot/time-change handling |
| `utils/Ui.java` | Shared visual components; no farm database rules |
| `utils/FileExports.java` | CSV quoting, paginated PDF output and Android document streams |

Layouts are in `app/src/main/res/layout`. `activity_main.xml` defines the app shell; `fragment_page.xml` is the shared scrollable page layout. Screen Java adds reusable native widgets for dynamic farm records. Resources cannot have arbitrary nested screen folders; names identify their purpose.

## Backend

| File | Responsibility |
|---|---|
| `app/main.py` | HTTP routes, role checks and request transactions |
| `app/schemas.py` | Required fields and allowed values |
| `app/service.py` | Capacity, dates, transfers/splits, outcomes, idempotency, snapshots, backup rules |
| `app/database.py` | SQLite connections and schema |
| `app/security.py` | Password hashes, sessions and authenticated user lookup |
| `tests/test_workflows.py` | Farm invariants, authorization and recovery tests |

## Debug by symptom

- Wrong display: inspect the relevant screen and `Ui.java`.
- Network failure: inspect Settings, server startup, mode and `PiApiClient`.
- Wrong quantity or capacity: inspect `service.py`, not the XML.
- Date mismatch: check `IncubationCalculator`, original batch timestamp and server time.
- Missing reminder: check permission, Settings, alert state, snooze and Android restrictions.
- Failed split: refresh the group's version and inspect row capacity.
- No ML results: expected in this version; the Scan tab is a placeholder.

Do not change a server rule only in the phone UI. The backend must still enforce it for any future web app.
