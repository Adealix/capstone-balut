package com.capstone.balut.screens.profile;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.*;
import com.capstone.balut.data.repositories.FarmRepository;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.utils.Ui;
import org.json.JSONObject;

public class ProfileFragment extends BaseFragment {
  @Override
  protected void draw() {
    Ui.title(content, "Your workspace", "Farm records and settings, in one place.");
    JSONObject user = repo().user();
    LinearLayout card = Ui.card(content);
    Ui.add(card, Ui.text(requireContext(), user.optString("name", "Operator"), 23, Ui.INK, true));
    Ui.body(
        card,
        user.optString("username")
            + "\n"
            + user.optString("role")
            + " · "
            + repo().snapshot().optString("farm_name", "Farm"));
    Ui.secondary(card, "Edit profile / password", this::profile);
    Ui.button(content, "Settings & connection", () -> host().open(new SettingsFragment()));
    Ui.secondary(content, "Reports & exports", () -> host().open(new ReportsFragment()));
    Ui.secondary(content, "Activity history", () -> host().open(new ActivityFragment()));
    Ui.secondary(content, "Physical tray inventory", () -> host().open(new TraysFragment()));
    if (repo().owner()) {
      Ui.secondary(content, "Manage Operators", () -> host().open(new UsersFragment()));
      Ui.secondary(
          content,
          "Backup operational records",
          () ->
              repo()
                  .send(
                      "GET",
                      "/backup",
                      null,
                      new FarmRepository.Callback() {
                        public void success(JSONObject data) {
                          if (isAdded())
                            host()
                                .saveDocument(
                                    "balut-backup.json",
                                    "application/json",
                                    data.toString(),
                                    "json");
                        }

                        public void failure(int status, String message) {
                          if (isAdded()) host().message(message);
                        }
                      }));
      Ui.secondary(content, "Restore backup", () -> host().openRestore());
    }
    Ui.secondary(content, "Help & workflow guide", () -> host().open(new HelpFragment()));
    Ui.secondary(
        content,
        "Sign out",
        () -> {
          if (repo().pending().length() > 0) {
            host()
                .message(
                    "Synchronize or discard your pending registrations in Settings before signing"
                        + " out.");
            return;
          }
          new AlertDialog.Builder(requireContext())
              .setTitle("Sign out?")
              .setMessage(
                  "Saved farm records remain private in the app cache. Your local session will be"
                      + " removed.")
              .setPositiveButton(
                  "Sign out",
                  (d, w) ->
                      repo()
                          .send(
                              "POST",
                              "/auth/logout",
                              json(),
                              new FarmRepository.Callback() {
                                public void success(JSONObject data) {
                                  exit();
                                }

                                public void failure(int status, String message) {
                                  exit();
                                }

                                private void exit() {
                                  if (isAdded()) {
                                    repo().settings.clearToken();
                                    com.capstone.balut.notifications.ReminderScheduler.clear(
                                        requireContext());
                                    host().openLogin();
                                  }
                                }
                              }))
              .setNegativeButton("Cancel", null)
              .show();
        });
    Ui.body(
        content,
        "Balut Companion 1.0.0\n"
            + "Java Android app • Local Raspberry Pi API\n"
            + "ML and real hardware integration pending.");
  }

  private void profile() {
    LinearLayout form = Ui.form(requireContext());
    EditText name =
        Ui.field(form, "Display name", repo().user().optString("name"), InputType.TYPE_CLASS_TEXT);
    EditText current =
        Ui.field(
            form,
            "Current password (only to change password)",
            "",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    EditText next =
        Ui.field(
            form,
            "New password (optional)",
            "",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    new AlertDialog.Builder(requireContext())
        .setTitle("Your profile")
        .setView(Ui.scroll(form))
        .setPositiveButton(
            "Save",
            (d, w) ->
                host()
                    .mutate(
                        "PATCH",
                        "/profile",
                        json(
                            "name",
                            name.getText().toString().trim(),
                            "current_password",
                            current.getText().toString(),
                            "new_password",
                            next.getText().toString()),
                        () -> {
                          if (next.getText().length() > 0) {
                            repo().settings.clearToken();
                            host().openLogin();
                          }
                        }))
        .setNegativeButton("Cancel", null)
        .show();
  }
}
