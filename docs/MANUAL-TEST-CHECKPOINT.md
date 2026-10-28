# Manual testing checkpoint

User-reported checkpoint: **MT-17**, 28 October 2026. The user reported that testing had gone well so far and paused for the night. Completion of MT-17 itself was not confirmed. Do not infer a Pass for every preceding subcase or for unexecuted cases.

## Reported observations

1. **MT-05 — carrier status was hard to find.** Registration events in the local practice database recorded T01 for Manual Batch A and T02 for Manual Batch B. The batch-details page did not display the carrier, and inventory displayed the state without an explicit status label. Added `Physical carrier: T01 · In use` to active C1/C2 groups, explicit `Status: …` labels in inventory, readable linked batch/location details and a refresh action. A carrier correctly returns to Cleaning, then Available after the later manual cleaning step; its status is not expected to remain In use forever.
2. **Farm-name save lacked feedback.** Settings previously sent the update without a success callback. Added a disabled Saving state, persistent success/failure text, a success toast only after backend acknowledgment, and a blank-name field error.

## Resume instructions

- Resume at **MT-17** in the manual guide and confirm whether its backup/restore steps were completed.
- Recheck the two UI observations on the rebuilt app. Use an active C1/C2 group with an assigned carrier for the tray-status check; already-finished carriers may correctly be Available.
- MT-18 and later remain unconfirmed for the user's manual session.
- Existing practice databases, account sessions and pending records were preserved; they are not included in GitHub.
