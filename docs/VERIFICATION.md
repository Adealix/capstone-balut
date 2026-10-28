# First-build verification — 28 October 2026

## Delivered software

Native Java/XML Android app and separate local Python/SQLite API, built under `D:\capstone-balut`. The APK uses simulation mode by default. The Scan page is an explicit ML placeholder. Real hardware commands are unavailable until a supported adapter is installed.

## Checks that actually passed

| Check | Evidence |
|---|---|
| Debug APK assembly | `evidence/android-build.txt` — BUILD SUCCESSFUL |
| Java date-calculation unit tests | 5 passed; Gradle JUnit results under frontend/app/build/test-results |
| Android lint | 0 errors; 21 warnings retained for further cleanup |
| Backend workflow tests | 32 passed; `evidence/backend-tests.txt` |
| Android device integration tests | 5 passed; `evidence/android-device-final.txt` |
| Emulator setup/login, five tabs, details, session restart | `evidence/emulator-smoke.json` and screenshots 01–06 |
| C1→C2, preserved original date, split to C3, partial outcome and exact quantity reconciliation | `evidence/emulator-workflow.json` and screenshots 07–09 |
| Acknowledgment does not harvest/remove eggs | Backend tests and emulator workflow assertions |
| UI registration survives process restart | Emulator workflow suite |
| Test notification posts on emulator | Device integration and `evidence/notification-proof.txt` |
| Offline banner, pending registration, reconnection, exactly one server record and empty outbox | `evidence/emulator-offline.json`, screenshots 10–11 |
| Password/token exclusion from portable package | Packaging script checks the generated private QA credential is absent |
| Source ZIP integrity and APK hash | `dist/delivery-manifest.json` |

The five device tests verify SQLite cache/outbox persistence across database reopen, Keystore token round-trip without plaintext storage, native PDF rendering/pagination, CSV quoting/formula neutralization and Android notification posting. They run in the actual Android Studio SDK emulator (Pixel 6 profile, API 36.1 / Android 16), not a browser mockup.

The offline QA's first launcher waited on an inherited Windows output pipe after starting the service. Offline steps had passed and the server had synchronized the record. The remaining server/UI/device-database assertions were completed separately with `verify_offline_completion.py`; the final evidence records that recovery. The run helper now uses a detached hidden console and direct file logging.

Android Studio was launched with the project path, but a targetable IDE window was not available to desktop automation. The Gradle wrapper/build and SDK emulator were independently verified. Open `D:\capstone-balut\frontend` manually in Android Studio; emulator `capstone-balut` is configured and the app is installed.

## Material limits and remaining validation

- No Raspberry Pi, sensors, motors, heaters, candling bulbs or camera were connected. No physical-control reliability is claimed.
- No ML candidate accuracy, empty/hand recognition or live camera performance was tested in this app. Scan is a placeholder.
- Closed-app immediate equipment push and outside-farm access are excluded from this local-first delivery.
- Notification permission and one local notification were tested. Extended Doze/battery restrictions, reboot delivery and long-running alarms still need validation; delivery is not guaranteed under Android force-stop or denied permissions.
- Backup/restore transaction, version, capacity and credential-exclusion checks passed at API level. Native PDF export and CSV logic passed on-device; every system document-picker destination and interrupted import/export scenario has not been exhaustively tested.
- Role authorization passed at API level. Broad concurrent multi-phone trials and physical-phone compatibility remain outstanding.
- A 1,000-record performance benchmark and enlarged-font accessibility campaign have not been completed.
- Lint warnings include locale handling, dependency-update suggestions, old-target warning relative to newer SDKs, hardcoded/constructed text, drawing allocation, view constructors, switch styling and data-extraction metadata. There are no lint errors. Some document-picker APIs generate Java deprecation notes but remain functional.

The portable archive intentionally excludes the local account database, sessions, private QA credentials, Android SDK paths, Gradle caches, virtual environment and downloaded formatting/build tools. The local demonstration account remains usable in the emulator; its generated credentials are in `.local/qa-login.json` on this laptop only.
