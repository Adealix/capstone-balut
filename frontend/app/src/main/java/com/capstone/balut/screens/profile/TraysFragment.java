package com.capstone.balut.screens.profile;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.*;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.utils.Ui;
import org.json.*;

public class TraysFragment extends BaseFragment {
  @Override
  protected void draw() {
    back();
    Ui.title(
        content, "Physical trays", "Reusable carrier IDs are separate from compartment positions.");
    Ui.body(
        content,
        "Carriers follow their eggs through C1 and C2. Empty carriers are marked Cleaning; C3"
            + " finishing positions are tracked separately.");
    Ui.secondary(content, "Refresh tray inventory", () -> host().refresh());
    if (repo().owner())
      Ui.button(
          content,
          "Register physical tray",
          () -> {
            LinearLayout form = Ui.form(requireContext());
            EditText code =
                Ui.field(
                    form, "Tray code (letters / numbers / hyphen)", "", InputType.TYPE_CLASS_TEXT);
            new AlertDialog.Builder(requireContext())
                .setTitle("Register tray")
                .setView(Ui.scroll(form))
                .setPositiveButton(
                    "Register",
                    (d, w) ->
                        host()
                            .mutate(
                                "POST",
                                "/trays",
                                json("id", code.getText().toString().trim()),
                                null))
                .setNegativeButton("Cancel", null)
                .show();
          });
    JSONArray trays = repo().list("trays");
    for (int i = 0; i < trays.length(); i++) {
      JSONObject tray = trays.optJSONObject(i);
      LinearLayout card = Ui.card(content);
      Ui.add(card, Ui.text(requireContext(), tray.optString("id"), 20, Ui.INK, true));
      String state = tray.optString("state", "Unknown");
      TextView status = Ui.text(requireContext(), "Status: " + state, 16, Ui.GREEN, true);
      status.setContentDescription(tray.optString("id") + ", status: " + state);
      Ui.add(card, status);
      if (tray.optString("group_id").isEmpty())
        Ui.secondary(
            card,
            "Update tray availability",
            () -> {
              String[] options = {"Available", "Cleaning", "Unavailable"};
              new AlertDialog.Builder(requireContext())
                  .setTitle(tray.optString("id"))
                  .setItems(
                      options,
                      (d, w) ->
                          host()
                              .mutate(
                                  "PATCH",
                                  "/trays/" + tray.optString("id"),
                                  json("state", options[w]),
                                  null))
                  .show();
            });
      else {
        JSONObject group = find("groups", tray.optString("group_id"));
        JSONObject batch = find("batches", group.optString("batch_id"));
        Ui.body(
            card,
            "Batch: "
                + batch.optString("name", "Unknown batch")
                + "\nLocation: "
                + group.optString("location", "Unknown position"));
      }
    }
  }
}
