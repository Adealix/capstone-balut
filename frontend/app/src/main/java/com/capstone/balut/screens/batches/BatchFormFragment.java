package com.capstone.balut.screens.batches;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.*;
import com.capstone.balut.data.repositories.FarmRepository;
import com.capstone.balut.logic.IncubationCalculator;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.utils.Ui;
import java.time.Instant;
import java.util.UUID;
import org.json.*;

public class BatchFormFragment extends BaseFragment {
  @Override
  public boolean refreshesAutomatically() {
    return false;
  }

  @Override
  protected void draw() {
    back();
    Ui.title(content, "Register collection", "One 6 × 5 tray holds up to 30 eggs.");
    EditText name = Ui.field(content, "Batch name", "", InputType.TYPE_CLASS_TEXT);
    EditText source = Ui.field(content, "Egg source / supplier", "", InputType.TYPE_CLASS_TEXT);
    EditText quantity = Ui.field(content, "Number of eggs", "30", InputType.TYPE_CLASS_NUMBER);
    EditText start =
        Ui.date(content, "Original incubation start (Manila)", Instant.now().toString());
    Spinner position = Ui.select(content, "Initial tray position", new String[] {"C1-1", "C1-2"});
    java.util.ArrayList<String> trayCodes = new java.util.ArrayList<>();
    trayCodes.add("Unassigned / identify later");
    org.json.JSONArray trays = repo().list("trays");
    for (int i = 0; i < trays.length(); i++) {
      org.json.JSONObject tray = trays.optJSONObject(i);
      if (tray.optString("state").equals("Available")) trayCodes.add(tray.optString("id"));
    }
    Spinner physicalTray =
        Ui.select(content, "Physical carrier tray", trayCodes.toArray(new String[0]));
    EditText notes =
        Ui.field(
            content, "Notes", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    Ui.body(
        content,
        "Day 0 starts at the original incubation time. Transfers and finishing assignments keep"
            + " this date. A pending offline registration does not reserve space.");
    Button save = Ui.button(content, "Save batch", () -> {});
    save.setOnClickListener(
        v -> {
          try {
            int count = Integer.parseInt(quantity.getText().toString());
            String started = IncubationCalculator.fromForm(start.getText().toString());
            if (name.getText().toString().trim().isEmpty()
                || count < 1
                || count > 30
                || Instant.parse(started).isAfter(Instant.now())) {
              host()
                  .message(
                      "Enter a batch name, 1–30 eggs, and a start date that is not in the future.");
              return;
            }
            JSONObject data =
                json(
                    "name",
                    name.getText().toString().trim(),
                    "source",
                    source.getText().toString(),
                    "quantity",
                    count,
                    "started",
                    started,
                    "location",
                    position.getSelectedItem().toString(),
                    "notes",
                    notes.getText().toString());
            String requestId = UUID.randomUUID().toString();
            data.put(
                "tray_id",
                physicalTray.getSelectedItemPosition() == 0
                    ? ""
                    : physicalTray.getSelectedItem().toString());
            save.setEnabled(false);
            save.setText("Saving…");
            repo()
                .sendId(
                    "POST",
                    "/batches",
                    data,
                    requestId,
                    new FarmRepository.Callback() {
                      public void success(JSONObject result) {
                        if (!isAdded()) return;
                        host().getSupportFragmentManager().popBackStack();
                        host().refresh();
                      }

                      public void failure(int status, String message) {
                        if (!isAdded()) return;
                        save.setEnabled(true);
                        save.setText("Save batch");
                        if (status == 0)
                          new AlertDialog.Builder(requireContext())
                              .setTitle("Save as pending registration?")
                              .setMessage(
                                  "The backend did not confirm this save. Keep the same request ID"
                                      + " for synchronization, so a retried response cannot create"
                                      + " duplicate eggs. Capacity will be checked on sync.")
                              .setPositiveButton(
                                  "Keep pending",
                                  (d, w) -> {
                                    repo().queueBatch(requestId, data);
                                    host().getSupportFragmentManager().popBackStack();
                                    host().redraw();
                                  })
                              .setNegativeButton("Keep editing", null)
                              .show();
                        else host().message(message);
                      }
                    });
          } catch (Exception error) {
            host().message("Check the egg count and start date.");
          }
        });
  }
}
