package com.capstone.balut.screens.dashboard;

import android.app.AlertDialog;
import android.widget.*;
import com.capstone.balut.logic.IncubationCalculator;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.screens.batches.BatchDetailsFragment;
import com.capstone.balut.utils.Ui;
import java.time.*;
import org.json.*;

public class DashboardFragment extends BaseFragment {
  @Override
  protected void draw() {
    Ui.title(
        content,
        "Incubator overview",
        "Two initial trays. One review station. Three finishing levels.");
    Ui.secondary(content, "Refresh device status", () -> host().refresh());
    JSONObject device = repo().snapshot().optJSONObject("device");
    if (device == null) {
      Ui.notice(
          content,
          "No device data yet",
          "Connect to the backend, or start the local simulation server.");
      return;
    }
    Ui.notice(
        content,
        repo().settings.mode().equals("simulation") ? "Simulation is active" : "Device connection",
        device.optString("note"));
    JSONArray readings = device.optJSONArray("readings");
    if (readings != null)
      for (int i = 0; i < readings.length(); i++) {
        JSONObject reading = readings.optJSONObject(i);
        String stage = reading.optString("compartment");
        LinearLayout card = Ui.card(content);
        Ui.add(
            card,
            Ui.text(
                requireContext(),
                stage
                    + "  "
                    + (stage.equals("C1")
                        ? "Initial incubation"
                        : stage.equals("C2") ? "Candling & review" : "Finishing"),
                19,
                Ui.INK,
                true));
        boolean fresh = false;
        try {
          fresh =
              repo().online
                  && Duration.between(Instant.parse(reading.getString("at")), Instant.now())
                          .getSeconds()
                      < 30;
        } catch (Exception ignored) {
        }
        String values =
            reading.isNull("temperature")
                ? "No incubation reading available"
                : String.format(
                    java.util.Locale.US,
                    "%.1f °C     %.0f %% RH",
                    reading.optDouble("temperature"),
                    reading.optDouble("humidity"));
        Ui.add(card, Ui.text(requireContext(), values, 23, fresh ? Ui.GREEN : Ui.MUTED, true));
        Ui.body(
            card,
            (fresh ? reading.optString("state") : "Unavailable or stale — not a current reading")
                + "\nUpdated: "
                + IncubationCalculator.display(reading.optString("at")));
        int count = stage.equals("C1") ? 2 : stage.equals("C2") ? 1 : 3;
        for (int slot = 1; slot <= count; slot++) {
          String location = stage.equals("C2") ? "C2" : stage + "-" + slot;
          int quantity = 0;
          JSONArray groups = repo().list("groups");
          for (int j = 0; j < groups.length(); j++) {
            JSONObject group = groups.optJSONObject(j);
            if (group.optString("location").equals(location)) quantity += group.optInt("quantity");
          }
          Ui.heading(card, location + "  ·  " + quantity + " / 30 eggs");
          ProgressBar bar =
              new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
          bar.setMax(30);
          bar.setProgress(quantity);
          Ui.add(card, bar);
          for (int j = 0; j < groups.length(); j++) {
            JSONObject group = groups.optJSONObject(j);
            if (!group.optString("location").equals(location) || group.optInt("quantity") == 0)
              continue;
            JSONObject batch = find("batches", group.optString("batch_id"));
            Ui.secondary(
                card,
                batch.optString("name")
                    + " · "
                    + group.optInt("quantity")
                    + " eggs"
                    + (stage.equals("C3") ? " · Row " + group.optString("row_name") : ""),
                () -> host().open(BatchDetailsFragment.create(batch.optString("id"))));
          }
          JSONObject caps = device.optJSONObject("capabilities");
          boolean supported =
              caps != null && caps.optBoolean(stage.equals("C2") ? "candling_light" : "turning");
          String[] options =
              stage.equals("C2")
                  ? new String[] {"Candling light on", "Candling light off"}
                  : new String[] {"Pause turning", "Resume turning"};
          Button control =
              Ui.secondary(
                  card,
                  stage.equals("C2")
                      ? "Candling controls"
                      : "Turning: "
                          + device.optJSONObject("turning").optString(location, "Unknown"),
                  () ->
                      new AlertDialog.Builder(requireContext())
                          .setTitle(location + " controls")
                          .setItems(
                              options,
                              (dialog, index) ->
                                  new AlertDialog.Builder(requireContext())
                                      .setTitle("Confirm " + options[index].toLowerCase() + "?")
                                      .setMessage(
                                          "Applies to the whole "
                                              + location
                                              + " level. "
                                              + (repo().settings.mode().equals("simulation")
                                                  ? "Simulation only; no motor or bulb will"
                                                      + " operate."
                                                  : "Wait for a device-confirmed result."))
                                      .setPositiveButton(
                                          "Confirm",
                                          (d, w) ->
                                              host()
                                                  .mutate(
                                                      "POST",
                                                      "/device/commands",
                                                      json(
                                                          "command",
                                                          options[index],
                                                          "location",
                                                          location),
                                                      () ->
                                                          host()
                                                              .message(
                                                                  "The backend completed this"
                                                                      + " simulated command. No"
                                                                      + " hardware was operated.")))
                                      .setNegativeButton("Cancel", null)
                                      .show())
                          .show());
          control.setEnabled(supported && repo().online);
        }
      }
    Ui.heading(content, "Reading history");
    Ui.body(
        content,
        "History contains recorded samples only. Gaps are left visible; there is no interpolation"
            + " across missing periods.");
    JSONArray history = repo().list("telemetry");
    if (history.length() < 2)
      Ui.body(
          content,
          "Keep the dashboard connected to collect history. No historical readings are"
              + " fabricated.");
    else {
      TrendView chart = new TrendView(requireContext(), history);
      content.addView(chart, new LinearLayout.LayoutParams(-1, Ui.dp(requireContext(), 180)));
    }
    Ui.body(
        content,
        "Incubation must continue independently on the Raspberry Pi when this app is closed. Row"
            + " categories do not create independent motors or humidity zones.");
  }
}
