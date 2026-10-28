# Delivery scope and acceptance

The agreed first build includes the mobile app and local software backend. Completeness is demonstrated through working paths and evidence, not an unmeasured percentage.

## Functional scope

- First Owner setup, login/logout, Owner-created Operators, disabling Operators and profile/password changes.
- Five main tabs and subsidiary forms, settings, help, activity, reports and tray inventory.
- Batch registration (1–30 eggs), original start, source, notes, filtering/searching, correction reason and archive after completion.
- C1 positions 1–2; one C2 review station; C3 levels 1–3 with five six-egg rows per level.
- Manual transfers, partial splits, candidate assessment, purpose and scheduled review target. Same original date retained.
- Physical carrier inventory; available/in-use/cleaning/unavailable state. C2 carrier released for cleaning after eggs leave it.
- Partial harvested/hatched/removed/discarded outcomes with exact accounting.
- Review alerts with distinct acknowledge, snooze and resolve actions.
- Notification permission, test delivery, known-date inexact reminders and reboot/time-change scheduling.
- Simulated device readings and motor/light commands with confirmations, capability checks and backend results.
- Real mode refuses unsupported hardware operations; no fake success or fallback simulation.
- Timestamped cache, offline-readable records, pending new registrations, idempotent retry and version conflict reporting.
- CSV batch export, cohort-based PDF report, validated operational JSON backup/restore excluding accounts and sessions.
- Fixed 30-position scan map with no claimed predictions.

## Invariants

1. Original batch total = active group quantities + recorded outcome/removal quantities.
2. No occupied initial/candling tray position accepts a second active carrier group.
3. C3 level <=30 eggs; each finishing row <=6 eggs.
4. A transfer preserves source batch and original incubation time.
5. Acknowledging/resolving a reminder does not alter active egg quantities.
6. Idempotent registration/transfer/outcome/command retries do not execute twice.
7. Versions reject stale group/batch updates; failed transactions roll back.
8. Real and simulated records, modes and sessions remain distinct.
9. Food safety, hatchability and sale readiness are never inferred from dates or image labels.
10. App shutdown or disconnection cannot be the Pi's incubation-control dependency.

## Integration boundaries

Remaining engineering includes physical Pi deployment/TLS provisioning, sensor/motor/camera adapters, real control interlocks, ML inference and calibrated presence/classification, actual chamber/turning capability configuration, physical-phone validation and prolonged farm trials. These are not disguised as minor bug fixes.

Remote access, immediate remote background equipment push, SMS, public signup, e-commerce, payments and financial ROI are excluded. A local notification for an already saved review target is different from receiving a new equipment fault while the app is closed.

## Quality targets

Readable Java with separate screens/data/rules; no network work on UI thread; bounded network timeouts; stale/unavailable readings clearly identified; disabled unsupported controls; data persists; restore validates before commit; platform document picker avoids broad storage permission; records survive app process restart; error states explain recovery. Reports distinguish batch-start cohort from event-date reports.

Targets needing separate measurement: 1,000-record phone performance, enlarged-text usability, multi-device conflict trials, notification behavior under battery restrictions, TLS deployment and long-run equipment reliability. Do not claim those passed without a result in `evidence/`.

## Test paths

Build APK, run date unit tests, run Android lint, run backend workflow tests, install in Android Studio emulator, create/login Owner, grant notification permission, navigate all tabs, open details, register a batch, move/split it, record partial outcomes, verify original date, acknowledge a review without reducing quantity, restart app, export files, inspect offline state, test notification and verify role restrictions. Record actual results and limitations in the verification report.
