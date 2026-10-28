package com.capstone.balut.screens.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.provider.Settings;
import android.text.InputType;
import android.widget.*;
import com.capstone.balut.data.repositories.FarmRepository;
import com.capstone.balut.notifications.ReminderScheduler;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.utils.Ui;
import org.json.*;

public class SettingsFragment extends BaseFragment {
  @Override
  public boolean refreshesAutomatically() {
    return false;
  }

  @Override
  protected void draw() {
    back();
    Ui.title(content, "Settings", "Connection, reminders and pending registrations.");
    Ui.heading(content, "Backend connection");
    Ui.body(
        content,
        "Workspace: "
            + repo().settings.mode()
            + "\nAddress: "
            + repo().settings.endpoint()
            + "\n"
            + (repo().online ? "Backend reachable" : "Offline or not verified"));
    Ui.secondary(content, "Check connection", () -> host().refresh());
    Ui.secondary(
        content,
        "Change workspace / address",
        () -> {
          if (repo().pending().length() > 0) {
            host()
                .message("Synchronize or discard pending registrations before changing workspace.");
            return;
          }
          LinearLayout form = Ui.form(requireContext());
          Spinner mode = Ui.select(form, "Workspace", new String[] {"simulation", "real"});
          mode.setSelection(repo().settings.mode().equals("simulation") ? 0 : 1);
          EditText url =
              Ui.field(
                  form,
                  "Backend address",
                  repo().settings.endpoint(),
                  InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
          Ui.body(
              form,
              "Simulation uses http://10.0.2.2:8787 in the Android emulator. Real-device addresses"
                  + " require HTTPS and a trusted certificate. Changing this signs you out.");
          new AlertDialog.Builder(requireContext())
              .setTitle("Change workspace")
              .setView(Ui.scroll(form))
              .setPositiveButton(
                  "Save & sign out",
                  (d, w) -> {
                    ReminderScheduler.clear(requireContext());
                    repo().settings.clearToken();
                    String selected = mode.getSelectedItem().toString();
                    repo().settings.set("mode", selected);
                    repo()
                        .settings
                        .set(
                            "url_" + selected,
                            url.getText().toString().trim().replaceAll("/+$", ""));
                    repo().loadCache();
                    host().openLogin();
                  })
              .setNegativeButton("Cancel", null)
              .show();
        });
    Ui.heading(content, "Scheduled review reminders");
    Switch reminders = new Switch(requireContext());
    reminders.setText("Local review notifications");
    reminders.setChecked(ReminderScheduler.enabled(requireContext()));
    reminders.setMinHeight(Ui.dp(requireContext(), 48));
    reminders.setOnCheckedChangeListener(
        (v, checked) -> ReminderScheduler.enabled(requireContext(), checked));
    Ui.add(content, reminders);
    Ui.secondary(
        content,
        "Send test notification",
        () -> {
          ReminderScheduler.test(requireContext());
          host()
              .message(
                  "Test notification requested. If it does not appear, check Android notification"
                      + " permission and system settings.");
        });
    Ui.secondary(
        content,
        "Android notification settings",
        () ->
            startActivity(
                new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName())));
    Ui.body(
        content,
        "Local reminders use saved review dates. Android may delay delivery. The app does not"
            + " currently receive immediate remote equipment alerts while closed. Unresolved"
            + " reviews remain in the app.");
    Ui.heading(content, "Pending registrations");
    JSONArray pending = repo().pending();
    if (pending.length() == 0) Ui.body(content, "Nothing waiting to synchronize.");
    for (int i = 0; i < pending.length(); i++) {
      JSONObject item = pending.optJSONObject(i);
      LinearLayout card = Ui.card(content);
      Ui.body(
          card,
          item.optJSONObject("payload").optString("name") + "\nPending — capacity not reserved.");
      Ui.secondary(
          card,
          "Discard pending registration",
          () ->
              new AlertDialog.Builder(requireContext())
                  .setTitle("Discard this pending registration?")
                  .setMessage(
                      "This removes the local retry. If the original response was lost, refresh"
                          + " server records before registering again.")
                  .setPositiveButton(
                      "Discard",
                      (d, w) -> {
                        repo().database.removePending(item.optString("id"));
                        render();
                      })
                  .setNegativeButton("Cancel", null)
                  .show());
    }
    if (pending.length() > 0)
      Ui.button(
          content,
          "Synchronize pending registrations",
          () ->
              repo()
                  .synchronizePending(
                      new FarmRepository.Callback() {
                        public void success(JSONObject result) {
                          if (isAdded()) {
                            host().message("Pending registrations synchronized.");
                            host().refresh();
                          }
                        }

                        public void failure(int status, String message) {
                          if (isAdded()) host().message(message);
                        }
                      }));
    if (repo().owner()) {
      Ui.heading(content, "Farm settings");
      EditText farm =
          Ui.field(
              content,
              "Farm name",
              repo().snapshot().optString("farm_name"),
              InputType.TYPE_CLASS_TEXT);
      TextView feedback = Ui.text(requireContext(), "", 14, Ui.GREEN, false);
      feedback.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
      Button saveFarm = Ui.button(content, "Save farm name", () -> {});
      Ui.add(content, feedback);
      saveFarm.setOnClickListener(
          view -> {
            String name = farm.getText().toString().trim();
            if (name.isEmpty()) {
              farm.setError("Enter a farm name.");
              return;
            }
            saveFarm.setEnabled(false);
            saveFarm.setText("Saving…");
            feedback.setTextColor(Ui.MUTED);
            feedback.setText("Saving farm name…");
            repo()
                .send(
                    "PATCH",
                    "/settings/farm",
                    json("name", name),
                    new FarmRepository.Callback() {
                      public void success(JSONObject result) {
                        if (!isAdded()) return;
                        saveFarm.setEnabled(true);
                        saveFarm.setText("Save farm name");
                        feedback.setTextColor(Ui.GREEN);
                        feedback.setText("Farm name saved.");
                        Toast.makeText(requireContext(), "Farm name saved.", Toast.LENGTH_LONG)
                            .show();
                        host().refresh();
                      }

                      public void failure(int status, String message) {
                        if (!isAdded()) return;
                        saveFarm.setEnabled(true);
                        saveFarm.setText("Save farm name");
                        feedback.setTextColor(Ui.RED);
                        feedback.setText("Save failed: " + message);
                      }
                    });
          });
    }
    Ui.heading(content, "Diagnostics");
    Ui.body(
        content,
        "App 1.0.0\nMode: "
            + repo().settings.mode()
            + "\nLast error: "
            + (repo().lastError.isEmpty() ? "None" : repo().lastError)
            + "\nNo passwords or tokens are included.");
    Ui.secondary(
        content,
        "Export connection diagnostics",
        () ->
            host()
                .saveDocument(
                    "balut-diagnostics.txt",
                    "text/plain",
                    "Balut Companion 1.0.0\nMode: "
                        + repo().settings.mode()
                        + "\nOnline: "
                        + repo().online
                        + "\nLast error: "
                        + repo().lastError,
                    "text"));
  }
}
