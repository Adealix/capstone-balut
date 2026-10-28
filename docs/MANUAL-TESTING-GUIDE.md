# Balut Companion: manual testing walkthrough

For the delivered Java/XML app version **1.0.0**, Android Studio emulator **capstone-balut**, and the local Python backend. This is a walkthrough for you to execute: the cases below are **Not run by you yet**. Previous automated results remain in VERIFICATION.md.

Use the steps in order. They deliberately create predictable practice records so that you can check the quantities yourself. Dates and candidate assessments here are artificial test inputs, not recommended incubation or food-handling instructions.

## How to read this guide

Each case explains the screen's purpose, what to click, and its expected result. A result that differs from the expectation should be recorded in MANUAL-TEST-RESULTS.md with a screenshot and the smallest steps that reproduce it. Do not include passwords, login tokens or your private QA credential file in evidence.

Some actions are below the visible area. Swipe upward inside the page or dialog to reveal them. Use the page's Back button or Android's Back gesture to return. Selecting a bottom tab opens that tab's main page. Allow a few seconds for a save/refresh; regular visible-app refresh happens approximately every 20 seconds.

## MT-01 — Start a separate practice workspace

**Purpose:** Begin with empty test positions without deleting the existing demonstration data.

The existing default workspace already contains River flock, Finishing sample and other automated-test records, including occupied C1/C2 locations. Do not use those records for this exact sequence.

1. Open Android Studio.
2. Choose **Open** (or File → Open) and select **D:\capstone-balut\frontend**. Wait for Gradle sync/indexing.
3. Select the `app` run configuration and emulator **capstone-balut** in the toolbar. Start the emulator if necessary, then click the green Run triangle.
4. Open a Windows PowerShell window. Paste:

```powershell
powershell -ExecutionPolicy Bypass -File "D:\capstone-balut\tools\Start-Manual-Practice.ps1"
```

5. Leave that window open. It runs a simulation API on port **8789**, using **backend\data\manual-tutorial.sqlite3**. It does not replace **simulation.sqlite3**, the existing default workspace.
6. If the app is already signed in, open **Profile → Settings & connection → Change workspace / address**. If signed out, tap **Connection settings**.
7. Select **simulation** and enter **http://10.0.2.2:8789**. Choose **Save & sign out** or **Save & check**, depending on the screen.

**Expected:** The app connects to a fresh simulation workspace. Its first account screen says **Create the farm Owner**. A simulation label remains visible.

If port 8789 is already in use by your practice server, leave it running and use it; do not start a second copy. If you need a new empty attempt, stop that practice terminal with Ctrl+C, then run the same command with `-Session tutorial2`. This creates a different practice database without deleting earlier attempts. You must sign out/reconnect to create or log into the new workspace's Owner. Use one practice session throughout this walkthrough.

The PowerShell terminal is only for starting/stopping the server, like starting a development server for React. The remaining app tests happen through the emulator UI.

## MT-02 — Owner setup and login

**Purpose:** Establish who can administer the farm.

1. Enter your preferred test display name.
2. Use username **practiceowner**.
3. Choose and privately save your own password of at least eight characters.
4. Tap **Create Owner account**.
5. Allow Android notifications if prompted. If a password-manager prompt appears, you may dismiss it; saving the password there is not required by this app.

**Expected:** Home opens and Profile identifies the account as **Owner**. There are no batches yet. Do not load sample farm records; this tutorial supplies its own data.

If this practice database was used earlier, the app will show Sign in instead. Use the Owner credentials you created for that practice database, or use a new session name as explained above.

## MT-03 — Understand the five tabs

| Tab | What it should do | Your first check |
|---|---|---|
| Home | Daily tasks, active counts, due reviews and registration shortcuts | Zero active eggs on a fresh workspace |
| Dashboard | Compartment readings, occupancy, supported controls and history | C1-1/C1-2, C2, C3-1/C3-2/C3-3 are visible and empty |
| Scan | Future candling/ML workspace | ML integration pending and a 6-column × 5-row map |
| Batches | Search/filter and operate batch records | No active batches yet |
| Profile | Account, settings, reports, trays, accounts, backups, history and help | Owner administration options are present |

Visit every tab once. Navigation should work without a crash or lost connection. The Dashboard's temperatures/humidity are labeled simulation; they are not measurements from your incubator.

## MT-04 — Test the Scan placeholder

1. Open **Scan**.
2. Count six columns and five rows. Row labels run A–E and column numbers 1–6.
3. Tap **A1**, then **E6**.

**Expected:** Both taps explain that camera/ML integration is pending. There are no claimed predictions, live camera images or detected embryo results. Tap **Open batches for manual review** to confirm its navigation, then return Home.

## MT-05 — Register Manual Batch A

**Purpose:** Save a collection, its original start time, and its actual C1 position.

1. On Home, tap **Register a batch**.
2. Enter:

| Field | Test input |
|---|---|
| Batch name | Manual Batch A |
| Egg source / supplier | Practice Farm |
| Number of eggs | 12 |
| Original incubation start | Seven calendar days ago, at 12:00 AM Manila |
| Initial tray position | C1-1 |
| Physical carrier tray | T01 |
| Notes | First manual test batch |

3. Tap the date field to open the date picker, choose the date, then select the time in the time picker. Midnight means **12:00 AM**, not noon.
4. Tap **Save batch**.
5. Open **Batches → View batch** for Manual Batch A. Write down its generated batch ID and displayed original start time.

**Expected:** Registered 12, active 12, outcomes 0. Age is Day 7 until the next Manila midnight. Dashboard shows C1-1 **12 / 30**. Batch details shows **Physical carrier: T01 · In use** under the C1 group. In **Profile → Physical tray inventory**, scroll to T01: its card shows **Status: In use**, the assigned batch name and location. Use Refresh tray inventory if needed. After the later C2-to-C3 transfer and cleaning steps, T01 should no longer remain In use. The test date is an age-calculation input, not a declaration that every egg is fertile.

## MT-06 — Check registration validation and create Batch B

1. Open another **Register a batch** form. Fill name **Manual Batch B**, source **Practice Farm**, quantity **6**, original start **yesterday at 12:00 AM Manila**, position **C1-2**, and tray **T02**. Do not save yet.
2. Change only quantity to **0**, then try Save. Repeat with **31**. Each must be rejected. Restore quantity **6**.
3. Change only the start date to a future date and try Save; it must be rejected. Restore yesterday at midnight.
4. Change the position to **C1-1** and try saving. It is occupied by A, so the save must fail without adding B.
5. Change the position back to **C1-2**, then save.

**Expected:** Exactly two batches, 18 active eggs total. A has 12 in C1-1; B has 6 in C1-2. T01 and T02 are In use. No extra batches were created by rejected attempts.

A C1 slot accepts one active carrier group, even when that carrier has fewer than 30 eggs. Having room for more eggs is not the same as having room for a second tray.

## MT-07 — Dashboard and simulated controls

1. Open **Dashboard → Refresh device status**.
2. Confirm C1, C2 and C3 appear with the appropriate positions beneath them.
3. Inspect the timestamps. C1/C3 show simulated values; C2 need not show incubation readings.
4. Under C1-1, open its turning control, select **Pause turning**, then **Cancel** at confirmation.
5. Repeat, confirm Pause, and read the result. Refresh if necessary.
6. Confirm **Resume turning** afterward.
7. Under C2, try **Candling light on**, then **Candling light off**.

**Expected:** Cancel does not change state. Confirmed actions explicitly report simulated completion. C1-1 changes between Paused and Scheduled. No actual motor or bulb operates, and the Scan tab does not suddenly acquire a camera feed.

Keep the Dashboard connected for a minute or two, then scroll to Reading history. It should contain samples actually collected since the server started. A short/empty chart immediately after startup is normal. It should not invent earlier history.

## MT-08 — Move A into C2

1. Open **Batches → Manual Batch A → View batch**.
2. Under its C1-1 group, tap **Record transfer to C2**.
3. Leave **Eggs to move = 12** and **Destination = C2**.
4. Tap **Confirm transfer**.

**Expected:** A now has 12 eggs in C2; C1-1 becomes empty. Its registered/active quantities and original start time are unchanged. T01 remains In use because its eggs are still on the C2 carrier.

Now open B and try transferring its six eggs into C2.

**Expected:** C2 is occupied, so B's move is rejected. B stays in C1-2 with six eggs. This is a successful validation test, not an application defect.

## MT-09 — Split A into finishing rows

**Purpose:** Separate reviewed groups without losing their original batch/date.

1. Return to A's C2 group and tap **Review & assign finishing row**.
2. Enter quantity **6**, destination **C3-1**, row **A**, assessment **Developing**, intended outcome **Balut production**.
3. For **Scheduled human review target**, choose a time five minutes earlier than the current Manila time. If necessary, adjust the date too. This deliberately makes the practice review due; it must remain after A's original start.
4. Confirm.

**Expected:** A has six eggs in C2 and six in C3-1 Row A. Total active is still 12 and both retain A's original incubation date.

5. From the remaining C2 group, try assigning another six to **C3-1 Row A**.
6. The row is full; expect rejection and unchanged quantities.
7. Change the destination row to **B**. Choose assessment Developing, intended outcome **Duckling hatching**, and a future practice review target such as tomorrow at 9:00 AM. Confirm.

**Expected:** A now has six in Row A and six in Row B. C2 is empty. T01 becomes **Cleaning** because all eggs left the carrier. The purpose label does not trigger an automatic harvest or hatch. These target times only exercise the app.

The optional **total days from original start** field computes a review target from A's original date; it does not add those days again after C3 transfer. You can try it, inspect the computed date, then restore the practice target before saving.

## MT-10 — Reviews, acknowledgment, snooze and resolution

Do this before completing A's eggs.

1. Open Home. A's past target should appear under What needs attention.
2. Open **Alerts** in the header, or **Open review** from Home.
3. Tap **Open batch** on the due reminder. Confirm it opens A, then return to Alerts.
4. Record A's current active count: **12**.
5. Tap **Acknowledge review**.

**Expected:** The reminder becomes acknowledged, but A remains 12 active. No harvested or removed quantity is created.

6. Try **Snooze for one hour** and inspect the displayed snooze time.
7. Choose **Resolve reminder** and confirm.

**Expected:** The reminder moves to resolved history. It still does not change egg quantities. Snoozing does not move the incubation start date or the review target itself; it changes reminder handling.

## MT-11 — Actual outcomes, partial completion and archive

1. Open A. In **C3-1 Row A**, tap **Record outcome / removal**.
2. Enter quantity **2**, outcome **Harvested**, and confirm.

**Expected:** A = 12 registered = **10 active + 2 outcomes**. Row A has four; Row B has six.

3. Try recording five from the four-egg Row A. It must fail without changing counts.
4. Record the remaining **4** in Row A as Harvested.
5. Record the **6** in Row B as Removed. This is simulation-data cleanup, not an instruction to remove real eggs.

**Expected:** A = 12 registered = **0 active + 12 outcomes** (6 Harvested, 6 Removed). No quantity is negative.

6. In Batches, select **Completed**; A should appear before archiving.
7. Open A, scroll down and tap **Archive completed batch → Archive**.
8. Return to Batches; select **Archived**. A should remain accessible with its history. It should no longer appear under Active.

An active batch must not be archived. You can check B to confirm it does not offer the completed-batch archive action while its eggs remain active.

## MT-12 — Continue B through C2 into C3

1. Open B. Transfer all six from C1-2 into the now-empty C2.
2. From B's C2 group, choose **Review & assign finishing row**.
3. Assign six eggs to **C3-2 Row A**, human assessment **Non-developing**, purpose **Penoy processing**, and a future practice review target.
4. Confirm and inspect the original start date.

**Expected:** B remains six active, now in C3-2 Row A, with the same original date. T02 becomes Cleaning. Both C1 positions and C2 are now empty. A remains archived with 12 outcomes.

These observations are manual test labels. The app does not verify edibility or predict food safety.

## MT-13 — Search, filters and batch corrections

1. Open Batches. Search **Manual Batch**, then B's generated batch ID, then **Practice Farm**. Clear the search between checks.
2. Try Active, Completed, Archived and All. Active should contain B, Archived should contain A, and All should contain both.
3. Open B → **Edit batch details**. Change its notes and source, save, and verify the changes.
4. Change its original start to two days ago but leave the correction reason blank. The server should reject it.
5. Enter reason **Manual test of date correction**, then save.

**Expected:** The valid correction persists and appears in history. The current age recalculates, but the six eggs stay in the same C3 row. An explicit, logged date correction is different from the system accidentally resetting a date during transfer.

## MT-14 — Physical tray inventory

1. Open **Profile → Physical tray inventory**.
2. Confirm T01 and T02 are Cleaning after their C2 eggs have been assigned out; T03 is Available.
3. Choose **Update tray availability** for T01 and select Available. Repeat for T02 once you have conceptually completed the practice cleaning check.
4. As Owner, register tray **T-MANUAL**.
5. Change it to Unavailable, then back to Available.

**Expected:** States persist after leaving and reopening the page. An In use tray does not offer the same free availability change; it is still assigned to active eggs.

## MT-15 — Profile, farm settings and notifications

1. Open Profile → **Edit profile / password**.
2. Change only the display name to **Practice Owner**. Leave both password fields empty and save.
3. Open **Settings & connection**. Change the farm name to **Manual Test Farm**, then tap **Save farm name**. The button shows Saving while the request is pending. On backend success, expect a **Farm name saved.** toast and matching persistent text below the button. A failed save displays an error instead of a success message.
4. Return Home/Profile to check the new names.
5. In Settings, enable **Local review notifications**.
6. Tap **Send test notification**, acknowledge the explanatory dialog, then open the emulator's notification shade.

**Expected:** A notification titled **Test review reminder** appears if Android permission is granted. Tapping it opens the app. For a real saved review reminder, the link is intended to open its associated batch.

7. Open **Android notification settings** to inspect the app's permission. If disabled, enable it yourself and retry the test.
8. Toggle local reminders off and back on. Existing records/review dates must not disappear.
9. Try **Export connection diagnostics**; save the text file through Android's picker.

Review dates may also produce local notifications while the app is in the background, but Android may delay them. Use the test button to verify the notification path without waiting. No immediate remote equipment push is implemented.

## MT-16 — Reports and exports

1. Open **Profile → Reports & exports**.
2. Choose a start range that includes A's original seven-days-ago date and an end in the future. Tap **Apply date range**.
3. Before the later offline-registration test, expect **2 batches, 18 registered, 6 active, 6 Harvested, 6 Removed**.
4. Test an end before the start. It should be rejected without replacing the valid range.
5. Tap **Export batches as CSV**. Save as `manual-test-batches.csv` in the emulator's Downloads.
6. Tap **Export summary as PDF**. Save as `manual-test-report.pdf`.
7. Open the emulator's Files app → Downloads. Inspect the PDF with an available PDF viewer. Inspect/export the CSV with a text or spreadsheet viewer, or retrieve it using Android Studio's Device Explorer.

**Expected:** Files exist, contain the selected records, and match the displayed totals. Archived A is included in the selected cohort. The report explicitly selects batches by original start date; it is not an event-date-only harvest report.

Files saved through the emulator picker are in the **virtual phone**, not automatically in Windows Downloads. Device Explorer can retrieve files such as `/sdcard/Download/manual-test-report.pdf` to the laptop. If a viewer is absent, that is a viewer/environment issue; inspect the saved file through another available tool.

## MT-17 — Backup, restore and cancel behavior

Only perform restore against this practice workspace.

1. As Owner, choose **Profile → Backup operational records**.
2. Save `practice-baseline.json` in emulator Downloads. Keep this file unchanged.
3. Open B and add note **TEMPORARY RESTORE MARKER**; save and verify it appears.
4. Open **Profile → Restore backup**, then cancel the file picker. Verify nothing changed.
5. Open Restore again, choose `practice-baseline.json`, and type **RESTORE** in the confirmation field.
6. Confirm the restore and refresh.

**Expected:** B's notes return to the backed-up version without the temporary marker. Counts and operational records match the backup. Accounts and sessions are not part of the operational backup; this is not a password-reset mechanism. The database validates the backup before committing replacement.

Do not choose the original demonstration workspace at port 8787 for this restore test.

## MT-18 — Offline registration and synchronization

**Precondition:** B is in C3, both C1 positions are empty, and you know which PowerShell window runs the practice server on port 8789.

1. In that practice terminal, press **Ctrl+C**. This stops only the practice server.
2. In the app, open Dashboard → Refresh device status. Allow the request to fail.
3. Check for **Offline / saved records**. Old sensor values must be marked stale/unavailable, rather than presented as current measurements.
4. Open Home → Register a batch. Enter **Offline Manual**, quantity **3**, source **Practice Farm**, a valid current/past start, location **C1-1**, carrier **T03**.
5. Tap Save batch. When connection fails, select **Keep pending**.
6. Verify a pending indicator. The registration has not reserved a confirmed tray position yet.
7. Visit Settings to inspect the pending record. Try signing out: it should ask you to synchronize or discard pending registrations first.
8. Restart the same practice database in the terminal:

```powershell
powershell -ExecutionPolicy Bypass -File "D:\capstone-balut\tools\Start-Manual-Practice.ps1"
```

9. If you originally used a different `-Session`, supply that same name now. Do not accidentally start a new empty database.
10. In Dashboard, tap Refresh. Allow a few seconds for synchronization. If a conflict is reported, inspect it in Settings instead of repeatedly re-registering the batch.
11. In Batches, search Offline Manual. Check Dashboard and Settings.

**Expected:** Exactly one new batch with three eggs appears; C1-1 holds three; T03 is In use; the pending list is empty. Global totals are now **21 registered = 9 active + 12 outcomes/removals**, assuming the earlier steps were followed exactly.

If automatic synchronization cannot finish, **Settings → Synchronize pending registrations** retries the same request ID. Repeated refreshes must not add more copies. Transfers and hardware commands are not queued for surprise execution after reconnecting.

## MT-19 — Persistence and cancellation

1. Record A/B/Offline Manual counts and IDs.
2. Leave the app using Android's Home gesture, reopen it, then repeat by removing it from Recents and launching it again.
3. Verify records and the valid login session remain. If Android has expired the session, signing in must restore the server records.
4. Open an edit/transfer form and cancel it. Quantities and dates must remain unchanged.
5. Enter a registration form and wait past a background refresh while typing; the refresh should not clear your inputs.
6. Optionally rotate the emulator and inspect layout, navigation and draft fields. Record any lost input or clipped content as an issue rather than assuming rotation was exhaustively verified already.

Stopping the backend is not the same as closing the Android app. Reopening the app cannot restart a stopped server.

## MT-20 — Owner and Operator permissions

1. While signed in as Owner, open **Profile → Manage Operators → Add Operator**.
2. Create username **practiceoperator**, name **Practice Operator**, with a privately saved password of at least eight characters.
3. Confirm the Operator appears as Active.
4. Sign out. Try an incorrect password; expect rejection. Sign in with the Operator credentials.
5. Navigate Home, Dashboard, Scan, Batches, details, settings and reports.

**Expected:** Routine screens work, but Owner-only actions such as Manage Operators, operational backup/restore, registering a new tray and editing the farm name are not offered. Operators may still update routine tray availability and their own profile.

6. Sign out and sign back in as Owner.
7. In Manage Operators, disable practiceoperator and confirm.
8. Sign out and try logging in as that Operator; expect rejection.
9. Return as Owner and re-enable the Operator if you want to retain the account for later testing.

Do not use your personal passwords as test passwords or include them in a bug screenshot.

## MT-21 — Optional password-change test (do this last)

1. As Owner, open Edit profile / password.
2. Enter an incorrect current password and a valid new password; save. Expect rejection and no successful password change.
3. Repeat with the correct current password and a new password of at least eight characters that you save privately.

**Expected:** A successful change requires a new sign-in. The old password fails and the new one succeeds. Existing batch records are unchanged.

Skip this case if you do not want to change the practice account password yet; mark it Not run, not Pass.

## MT-22 — History, help and final reconciliation

1. Open **Profile → Activity history**. Inspect registrations, manual transfers, outcomes, corrections and account/configuration changes. Open event details.
2. Open A's View activity history and confirm entries refer to A.
3. Open **Help & workflow guide** and scroll through every section. Back should return to the app.
4. Revisit Home/Dashboard/Batches and reconcile quantities against this guide's ledger.

| Checkpoint | A | B | Offline Manual | Total active |
|---|---|---|---|---:|
| After registration | 12 in C1-1 | 6 in C1-2 | Not created | 18 |
| After moving A | 12 in C2 | 6 in C1-2 | Not created | 18 |
| After splitting A | 6 C3-1 A + 6 C3-1 B | 6 in C1-2 | Not created | 18 |
| After first harvest | 4 C3-1 A + 6 C3-1 B; 2 Harvested | 6 in C1-2 | Not created | 16 |
| After A completion and B assignment | A archived; 6 Harvested + 6 Removed | 6 C3-2 A | Not created | 6 |
| After offline sync | A archived; 12 outcomes/removals | 6 C3-2 A | 3 C1-1 | 9 |

Reports use all selected batches, including archived ones; Home's active total excludes completed/removed eggs. A discrepancy between those two differently scoped figures is not automatically a bug.

## MT-23 — What to report as a bug

Use MANUAL-TEST-RESULTS.md. For each problem, record:

1. Case ID and app version.
2. Your starting batch/location/quantity and whether the server was online.
3. The exact clicks and inputs (exclude passwords).
4. Expected result from this guide.
5. Actual result and whether it repeats.
6. Screenshot and, if relevant, Android Studio Logcat filtered to `com.capstone.balut` or the practice backend error.

Do not mark tests Pass because a screen opened: check the saved record/count after the operation. Do not assume a failed test is an implementation bug until its preconditions are checked (wrong port, occupied slot, denied permission, stopped server, expired session, missing viewer).

## Return to the original demonstration workspace

When finished, resolve/discard any remaining practice pending registrations, then use Profile → Settings & connection → Change workspace / address to select simulation at **http://10.0.2.2:8787**. Use the original demonstration Owner credentials if needed; their local location is described in README.md. Stop the practice terminal with Ctrl+C when you no longer need it. The practice database remains saved for later testing.

## References

- Android Studio emulator run flow: https://developer.android.com/studio/run/emulator
- The exact app controls and expectations above were checked against the local Java screen files and backend rules. Feature boundaries and prior automated evidence are in VERIFICATION.md.
