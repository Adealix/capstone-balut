# UI feedback fixes — 28 October 2026

The user reported two usability issues during manual testing and paused at MT-17.

## Changes

- Active C1/C2 groups in Batch Details now display their physical carrier ID and state. An unassigned carrier is stated explicitly.
- Physical tray inventory now labels each state as `Status: In use`, `Status: Available`, etc., shows the linked batch/location instead of an opaque group ID and offers a refresh action.
- Farm-name saving now disables the button while pending, displays a persistent success message and toast after server acknowledgment, and displays a persistent failure message on error. Blank names stay in the form with a field error.
- Manual guide updated to identify these labels and to explain that In use is a stage-specific state, not a permanent property of T01.

No farm records were repaired, reset or reassigned. The local practice registration events already associated A with T01 and B with T02. These are display/feedback changes, not a database migration.

## Verification

- Android debug build, five Java date unit tests and lint completed successfully (0 lint errors; 28 warnings).
- All 32 backend workflow tests passed, including carrier reservation/release and quantities.
- Five new Android UI regression tests passed on a separate API 36.1 emulator using in-memory fixtures: explicit inventory state/assignment, carrier shown on batch details, successful-save confirmation, failure without false success, and blank-name validation without an API call.
- The regression fixtures make no live backend requests and do not change the user's practice database, accounts or reminder records.

Local logs: `evidence/feedback-fix-build.txt`, `evidence/feedback-backend-tests.txt`, and `evidence/feedback-device-tests.txt`. Runtime logs/screenshots remain local and are not committed. The user's manual recheck on the updated app remains pending.
