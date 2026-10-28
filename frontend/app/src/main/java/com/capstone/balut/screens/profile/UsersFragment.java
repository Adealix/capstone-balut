package com.capstone.balut.screens.profile;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.*;
import com.capstone.balut.data.repositories.FarmRepository;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.utils.Ui;
import org.json.*;

public class UsersFragment extends BaseFragment {
  private JSONArray users = new JSONArray();
  private boolean loaded = false;

  @Override
  protected void draw() {
    back();
    Ui.title(content, "Manage Operators", "Only the Owner can create or disable accounts.");
    if (!repo().owner()) {
      Ui.body(content, "Owner permission required.");
      return;
    }
    Ui.button(content, "Add Operator", this::add);
    for (int i = 0; i < users.length(); i++) {
      JSONObject user = users.optJSONObject(i);
      LinearLayout card = Ui.card(content);
      Ui.add(card, Ui.text(requireContext(), user.optString("name"), 18, Ui.INK, true));
      Ui.body(
          card,
          user.optString("username")
              + " · "
              + user.optString("role")
              + "\n"
              + (user.optInt("active") == 1 ? "Active" : "Disabled"));
      if (user.optString("role").equals("Operator"))
        Ui.secondary(
            card,
            user.optInt("active") == 1 ? "Disable account" : "Enable account",
            () ->
                new AlertDialog.Builder(requireContext())
                    .setTitle("Change Operator access?")
                    .setMessage("Existing sessions for this Operator will be revoked.")
                    .setPositiveButton(
                        "Confirm",
                        (d, w) ->
                            host()
                                .mutate(
                                    "PATCH",
                                    "/users/" + user.optString("id"),
                                    json("active", user.optInt("active") != 1),
                                    () -> {
                                      loaded = false;
                                      render();
                                    }))
                    .setNegativeButton("Cancel", null)
                    .show());
    }
    if (!loaded) {
      loaded = true;
      repo()
          .send(
              "GET",
              "/users",
              null,
              new FarmRepository.Callback() {
                public void success(JSONObject result) {
                  users = result.optJSONArray("items");
                  if (isAdded()) render();
                }

                public void failure(int status, String message) {
                  if (isAdded()) Ui.body(content, message);
                }
              });
    }
  }

  private void add() {
    LinearLayout form = Ui.form(requireContext());
    EditText name = Ui.field(form, "Operator name", "", InputType.TYPE_CLASS_TEXT);
    EditText username =
        Ui.field(
            form,
            "Username",
            "",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    EditText password =
        Ui.field(
            form,
            "Initial password (at least 8 characters)",
            "",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    AlertDialog dialog =
        new AlertDialog.Builder(requireContext())
            .setTitle("Create Operator")
            .setView(Ui.scroll(form))
            .setPositiveButton("Create account", null)
            .setNegativeButton("Cancel", null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                      repo()
                          .send(
                              "POST",
                              "/users",
                              json(
                                  "name",
                                  name.getText().toString().trim(),
                                  "username",
                                  username.getText().toString().trim(),
                                  "password",
                                  password.getText().toString(),
                                  "role",
                                  "Operator"),
                              new FarmRepository.Callback() {
                                public void success(JSONObject result) {
                                  dialog.dismiss();
                                  loaded = false;
                                  if (isAdded()) render();
                                }

                                public void failure(int status, String message) {
                                  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                  if (isAdded()) host().message(message);
                                }
                              });
                    }));
    dialog.show();
  }
}
