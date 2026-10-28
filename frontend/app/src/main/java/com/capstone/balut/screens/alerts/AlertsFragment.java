package com.capstone.balut.screens.alerts;

import android.app.AlertDialog;
import android.widget.LinearLayout;
import com.capstone.balut.logic.IncubationCalculator;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.screens.batches.BatchDetailsFragment;
import com.capstone.balut.utils.Ui;
import java.time.*;
import org.json.*;

public class AlertsFragment extends BaseFragment {
  @Override
  protected void draw() {
    back();
    Ui.title(
        content,
        "Reviews & alerts",
        "Acknowledging means you saw the alert. It does not complete a harvest.");
    if (!repo().online)
      Ui.notice(
          content,
          "Backend disconnected",
          "Current equipment conditions cannot be confirmed. Displayed review records were last"
              + " saved on this phone.");
    JSONArray alerts = repo().list("alerts");
    int count = 0;
    for (int i = 0; i < alerts.length(); i++) {
      JSONObject alert = alerts.optJSONObject(i);
      if (alert.optString("state").equals("resolved")) continue;
      count++;
      LinearLayout card = Ui.card(content);
      Ui.add(card, Ui.text(requireContext(), alert.optString("title"), 18, Ui.INK, true));
      Ui.body(
          card,
          (IncubationCalculator.due(alert.optString("due")) ? "Target reached" : "Upcoming review")
              + " · "
              + alert.optString("state")
              + "\n"
              + IncubationCalculator.display(alert.optString("due")));
      if (!alert.optString("snoozed_until").isEmpty())
        Ui.body(
            card,
            "Snoozed until " + IncubationCalculator.display(alert.optString("snoozed_until")));
      JSONObject group = find("groups", alert.optString("group_id"));
      Ui.secondary(
          card,
          "Open batch",
          () -> host().open(BatchDetailsFragment.create(group.optString("batch_id"))));
      if (!alert.optString("state").equals("acknowledged"))
        Ui.button(card, "Acknowledge review", () -> act(alert, "acknowledge", ""));
      Ui.secondary(
          card,
          "Snooze for one hour",
          () -> act(alert, "snooze", Instant.now().plus(Duration.ofHours(1)).toString()));
      Ui.secondary(
          card,
          "Resolve reminder",
          () ->
              new AlertDialog.Builder(requireContext())
                  .setTitle("Resolve this reminder?")
                  .setMessage(
                      "This closes the reminder only. Record any harvest or removal in the batch"
                          + " separately.")
                  .setPositiveButton("Resolve", (d, w) -> act(alert, "resolve", ""))
                  .setNegativeButton("Cancel", null)
                  .show());
    }
    if (count == 0)
      Ui.notice(
          content,
          "All clear",
          "Scheduled review targets appear here after finishing assignments. There are no"
              + " unresolved review records.");
    Ui.heading(content, "Resolved history");
    int done = 0;
    for (int i = alerts.length() - 1; i >= 0 && done < 10; i--) {
      JSONObject alert = alerts.optJSONObject(i);
      if (alert.optString("state").equals("resolved")) {
        Ui.body(
            content,
            alert.optString("title")
                + "\nResolved / no longer applicable • "
                + IncubationCalculator.display(alert.optString("due")));
        done++;
      }
    }
  }

  private void act(JSONObject alert, String action, String until) {
    if (!repo().online) {
      host().message("Connect before updating a shared reminder.");
      return;
    }
    host()
        .mutate(
            "POST",
            "/alerts/" + alert.optString("id") + "/action",
            json("action", action, "until", until),
            null);
  }
}
