# Balut Companion

Java/XML Android application and a separate local Python API for the tri-stage duck-egg capstone. This project is independent of the ML training workspace at `D:\capstone_incubator`.

## Open and run

For a screen-by-screen practice walkthrough with expected results, use [Manual Testing Guide](docs/MANUAL-TESTING-GUIDE.md) and [Manual Test Results](docs/MANUAL-TEST-RESULTS.md). The guide provides a separate practice database so existing demo records remain unchanged.

Latest manual-testing checkpoint: [paused at MT-17](docs/MANUAL-TEST-CHECKPOINT.md). See [UI feedback fixes](docs/UI-FEEDBACK-FIXES.md) for the carrier labels and farm-name save confirmation added after that session.

1. In Android Studio, choose **Open** and select **`D:\capstone-balut\frontend`**. Do not select `app` alone.
2. Run `powershell -ExecutionPolicy Bypass -File D:\capstone-balut\tools\Start-Backend.ps1` to start the local simulation API.
3. In Android Studio Device Manager, start **capstone-balut** (Pixel 6 profile, Android API 36.1), then run the `app` configuration.
4. The emulator uses **http://10.0.2.2:8787** to reach this computer. In the app's Connection settings, select **simulation**.
5. On an empty database, create the farm Owner. Passwords require at least eight characters; there is no bundled production password. The Owner can add Operators from Profile.
6. To practice with records, use **Load sample farm records** on an empty simulation workspace. Sample data is clearly identified.

The emulator QA may already be signed into its generated local demonstration Owner. Its private credentials, when present, are in `.local/qa-login.json`, excluded from source packages. Change the password in Profile when taking over that demonstration account. Do not publish that file or the database.

## Folder map

```text
frontend/   Android Studio project, Java screens, XML resources and local cache
backend/    FastAPI server, SQLite records, authentication and farm rules
docs/       Architecture, requirements, API and integration documentation
tools/      Local run/build/test helpers
evidence/   Build, test and emulator verification evidence
```

Read [the code map](docs/CODE-MAP.md) before editing. For scope and known integration boundaries, see [requirements](docs/REQUIREMENTS.md). API documentation is available at http://127.0.0.1:8787/docs while the backend runs.

## Implemented workflow

Register up to 30 eggs in either C1 position, retain the original incubation time, manually move the carrier into C2, and enter human candling assessments while splitting groups into C3 levels and rows. Each C3 level holds at most 30 eggs, with rows Aâ€“E holding at most six eggs each. Preserve the source date, set a human review target, acknowledge reminders, and separately record actual outcomes or removals. Physical carrier IDs and cleaning/availability states are tracked separately from locations.

Home shows daily work. Dashboard shows clearly labeled simulated readings, capabilities, turning status and recorded temperature samples. Batches provides searching, filtering, details, manual transfers, partial outcomes, corrections and history. Profile contains accounts, tray inventory, settings, reminders, CSV/PDF exports and operational JSON backup/restore.

## Operating modes

Simulation is a **server mode**, not a fake success fallback after a failed connection. The default database is `backend/data/simulation.sqlite3`. `BALUT_MODE=real` selects a separate database and rejects hardware commands until an actual adapter exists. The app rejects a backend reporting a different mode from the selected workspace.

No physical motor, heater, bulb, sensor or camera is operated by this version. The Scan tab is deliberately a 6 Ã— 5 placeholder. The previous trained ML files are not silently connected: abnormal-class performance still requires further work.

Real LAN connections require HTTPS with a certificate trusted by Android. Plain HTTP is permitted only for the emulator host and loopback addresses. Localhost binding is intentional during development. Deployment to the farm network needs trusted TLS, provisioning and hardware integration; remote internet access and immediate closed-app equipment push notifications are outside this delivery.

## Data and reminders

The backend is authoritative for shared farm records. The phone has a SQLite cache and a pending-registration outbox. Only registrations may be kept pending offline; they do not reserve capacity. Reuse the original request ID to prevent duplicate creation when the original response was lost. Transfers, review changes and commands require connectivity. Conflicts remain visible for operator correction.

Scheduled review dates are **human review targets**, not automatic food-readiness decisions. Original start time is Day 0; Day 1 means 24 hours have elapsed. All display/form dates use Asia/Manila; timestamps are stored with UTC offsets. No fixed incubation recipe is imposed by the app.

Local known-date reminders use Android's inexact alarm scheduling, notification permission and the user's system settings. Unacknowledged due reviews can remind daily; acknowledgment stops repeated delivery, while a separate resolution closes the reminder. They are not guaranteed immediate equipment alarms. A force-stopped app cannot deliver notifications until Android allows it to run again. The app never claims acknowledgment equals harvesting.

## Development

- Android: Java 17 source, XML layouts, AndroidX AppCompat/Fragments, compile/target SDK 36, min SDK 26. Java-only app source; AndroidX has transitive Kotlin runtime libraries.
- Build: AGP 9.2.0 / Gradle 9.5.0; tested with Android Studio's bundled JBR 25.
- Backend: Python 3.10+, FastAPI, SQLite and expiring opaque sessions. Passwords use salted PBKDF2. App tokens are encrypted using Android Keystore.
- Basic Java classes, explicit SQL and repositories keep responsibilities visible. No dependency injection framework, Compose or complex domain framework is required.

Run `tools\Build-Android.ps1` for assemble, Java tests and lint. Run `tools\Test-Backend.ps1` for the backend workflow tests. `tools/emulator_smoke.py` installs the APK and checks actual emulator screens using ADB; it creates only demonstration records.

Future hardware control must remain autonomous on the Raspberry Pi. A shared motor is not independently controllable per row, and a shared chamber does not provide per-row humidity zones.
