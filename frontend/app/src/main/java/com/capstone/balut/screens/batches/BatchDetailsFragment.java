package com.capstone.balut.screens.batches;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import com.capstone.balut.data.repositories.FarmRepository;
import com.capstone.balut.logic.IncubationCalculator;
import com.capstone.balut.models.BatchRecord;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.utils.Ui;
import java.time.*;
import org.json.*;

public class BatchDetailsFragment extends BaseFragment {
  public static BatchDetailsFragment create(String id) {
    BatchDetailsFragment page = new BatchDetailsFragment();
    Bundle args = new Bundle();
    args.putString("batch_id", id);
    page.setArguments(args);
    return page;
  }

  private String id() {
    return requireArguments().getString("batch_id", "");
  }

  @Override
  protected void draw() {
    back();
    JSONObject data = find("batches", id());
    if (data.length() == 0) {
      Ui.notice(
          content, "Batch unavailable", "Refresh the records or check the selected workspace.");
      return;
    }
    BatchRecord batch = new BatchRecord(data);
    Ui.title(
        content,
        batch.name,
        batch.id + "\n" + (batch.source.isEmpty() ? "Source not specified" : batch.source));
    Ui.metric(
        content,
        "Day " + IncubationCalculator.elapsedDays(batch.started, Instant.now().toString()),
        "Original start: " + IncubationCalculator.display(batch.started));
    int done = 0;
    JSONArray outcomes = repo().list("outcomes");
    for (int i = 0; i < outcomes.length(); i++)
      if (outcomes.optJSONObject(i).optString("batch_id").equals(id()))
        done += outcomes.optJSONObject(i).optInt("quantity");
    Ui.body(
        content,
        batch.total
            + " registered = "
            + active(id())
            + " active + "
            + done
            + " recorded outcomes / removals");
    if (!batch.notes.isEmpty()) Ui.body(content, batch.notes);
    Ui.secondary(content, "Edit batch details", () -> edit(batch));
    Ui.heading(content, "Egg groups & locations");
    JSONArray groups = repo().list("groups");
    for (int i = 0; i < groups.length(); i++) {
      JSONObject group = groups.optJSONObject(i);
      if (!group.optString("batch_id").equals(id()) || group.optInt("quantity") == 0) continue;
      LinearLayout card = Ui.card(content);
      Ui.add(
          card,
          Ui.text(
              requireContext(),
              group.optString("location")
                  + (group.optString("row_name").isEmpty()
                      ? ""
                      : " · Row " + group.optString("row_name")),
              20,
              Ui.INK,
              true));
      Ui.body(
          card,
          group.optInt("quantity")
              + " eggs\nHuman assessment: "
              + group.optString("candidate")
              + "\nPurpose: "
              + group.optString("purpose"));
      if (group.optString("location").startsWith("C1")
          || group.optString("location").equals("C2")) {
        String carrier = "Not assigned";
        JSONArray trays = repo().list("trays");
        for (int j = 0; j < trays.length(); j++) {
          JSONObject tray = trays.optJSONObject(j);
          if (tray.optString("group_id").equals(group.optString("id"))) {
            carrier = tray.optString("id") + " · " + tray.optString("state");
            break;
          }
        }
        Ui.add(card, Ui.text(requireContext(), "Physical carrier: " + carrier, 15, Ui.GREEN, true));
      }
      if (!group.optString("target").isEmpty())
        Ui.body(
            card,
            "Scheduled review: "
                + IncubationCalculator.display(group.optString("target"))
                + (IncubationCalculator.due(group.optString("target"))
                    ? "\nReview target reached"
                    : ""));
      Ui.button(
          card,
          group.optString("location").startsWith("C1")
              ? "Record transfer to C2"
              : group.optString("location").equals("C2")
                  ? "Review & assign finishing row"
                  : "Move / adjust review target",
          () -> transfer(group, batch));
      Ui.secondary(card, "Record outcome / removal", () -> complete(group));
    }
    Ui.heading(content, "Recorded outcomes");
    int records = 0;
    for (int i = 0; i < outcomes.length(); i++) {
      JSONObject outcome = outcomes.optJSONObject(i);
      if (outcome.optString("batch_id").equals(id())) {
        Ui.body(
            content,
            outcome.optInt("quantity")
                + " eggs · "
                + outcome.optString("outcome")
                + "\n"
                + IncubationCalculator.display(outcome.optString("at")));
        records++;
      }
    }
    if (records == 0) Ui.body(content, "No harvest, hatch or removal recorded yet.");
    Ui.secondary(
        content,
        "View activity history",
        () -> {
          StringBuilder history = new StringBuilder();
          JSONArray events = repo().list("events");
          for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event.optString("entity").equals(id()))
              history
                  .append(IncubationCalculator.display(event.optString("at")))
                  .append("\n")
                  .append(event.optString("actor"))
                  .append(" · ")
                  .append(event.optString("action"))
                  .append("\n")
                  .append(event.optString("detail"))
                  .append("\n\n");
          }
          host().message(history.length() == 0 ? "No activity available." : history.toString());
        });
    if (active(id()) == 0 && !batch.archived)
      Ui.secondary(
          content,
          "Archive completed batch",
          () ->
              new AlertDialog.Builder(requireContext())
                  .setTitle("Archive this batch?")
                  .setMessage("History remains available in the Archived filter.")
                  .setPositiveButton(
                      "Archive",
                      (d, w) ->
                          host().mutate("POST", "/batches/" + id() + "/archive", json(), null))
                  .setNegativeButton("Cancel", null)
                  .show());
  }

  private void edit(BatchRecord batch) {
    LinearLayout form = Ui.form(requireContext());
    EditText name = Ui.field(form, "Batch name", batch.name, InputType.TYPE_CLASS_TEXT);
    EditText source = Ui.field(form, "Source", batch.source, InputType.TYPE_CLASS_TEXT);
    EditText started = Ui.date(form, "Original incubation start", batch.started);
    EditText notes =
        Ui.field(
            form,
            "Notes",
            batch.notes,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    EditText reason =
        Ui.field(form, "Reason if correcting start date", "", InputType.TYPE_CLASS_TEXT);
    AlertDialog dialog =
        new AlertDialog.Builder(requireContext())
            .setTitle("Edit batch")
            .setView(Ui.scroll(form))
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      try {
                        JSONObject body =
                            json(
                                "name",
                                name.getText().toString().trim(),
                                "source",
                                source.getText().toString(),
                                "started",
                                IncubationCalculator.fromForm(started.getText().toString()),
                                "notes",
                                notes.getText().toString(),
                                "reason",
                                reason.getText().toString(),
                                "version",
                                batch.version);
                        save(dialog, "PATCH", "/batches/" + batch.id, body);
                      } catch (Exception error) {
                        host().message("Check the date and required fields.");
                      }
                    }));
    dialog.show();
  }

  private void transfer(JSONObject group, BatchRecord batch) {
    if (!repo().online) {
      host()
          .message(
              "Connect and refresh before recording a physical transfer. Transfers are not queued"
                  + " offline.");
      return;
    }
    boolean initial = group.optString("location").startsWith("C1");
    LinearLayout form = Ui.form(requireContext());
    Ui.body(
        form,
        "Move the eggs manually, then confirm their actual destination. The original incubation"
            + " date is retained.");
    EditText quantity =
        Ui.field(
            form,
            "Eggs to move",
            String.valueOf(
                initial ? group.optInt("quantity") : Math.min(6, group.optInt("quantity"))),
            InputType.TYPE_CLASS_NUMBER);
    Spinner destination =
        Ui.select(
            form,
            "Destination",
            initial ? new String[] {"C2"} : new String[] {"C3-1", "C3-2", "C3-3"});
    Spinner row =
        initial
            ? null
            : Ui.select(
                form, "Finishing row (maximum 6 eggs)", new String[] {"A", "B", "C", "D", "E"});
    Spinner candidate =
        initial
            ? null
            : Ui.select(
                form,
                "Your candling assessment",
                new String[] {"Developing", "Non-developing", "Abnormal", "Recheck"});
    Spinner purpose =
        initial
            ? null
            : Ui.select(
                form,
                "Intended outcome / handling",
                new String[] {
                  "Balut production", "Penoy processing", "Duckling hatching", "Recheck"
                });
    EditText target =
        initial
            ? null
            : Ui.date(
                form,
                "Scheduled human review target",
                group.optString("target").isEmpty()
                    ? Instant.now().plus(Duration.ofDays(1)).toString()
                    : group.optString("target"));
    if (!initial) {
      Ui.body(
          form,
          "Choose the review date with your adviser/client. No biological readiness or food safety"
              + " is inferred. Abnormal or unclear eggs may need separate handling, rather than"
              + " continued food production.");
      EditText days =
          Ui.field(
              form, "Optional: total days from original start", "", InputType.TYPE_CLASS_NUMBER);
      Ui.secondary(
          form,
          "Apply total-day target",
          () -> {
            try {
              int value = Integer.parseInt(days.getText().toString());
              if (value < 1 || value > 60) throw new IllegalArgumentException();
              target.setText(
                  IncubationCalculator.formDate(
                      Instant.parse(batch.started).plus(Duration.ofDays(value)).toString()));
            } catch (Exception error) {
              host()
                  .message(
                      "Enter a total-day target between 1 and 60, confirmed by your"
                          + " adviser/client.");
            }
          });
    }
    AlertDialog dialog =
        new AlertDialog.Builder(requireContext())
            .setTitle(initial ? "Transfer to candling" : "Human review & assignment")
            .setView(Ui.scroll(form))
            .setPositiveButton("Confirm transfer", null)
            .setNegativeButton("Cancel", null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      try {
                        JSONObject body =
                            json(
                                "quantity",
                                Integer.parseInt(quantity.getText().toString()),
                                "destination",
                                destination.getSelectedItem().toString(),
                                "row_name",
                                initial ? "" : row.getSelectedItem().toString(),
                                "candidate",
                                initial ? "Unreviewed" : candidate.getSelectedItem().toString(),
                                "purpose",
                                initial ? "Unassigned" : purpose.getSelectedItem().toString(),
                                "target",
                                initial
                                    ? ""
                                    : IncubationCalculator.fromForm(target.getText().toString()),
                                "version",
                                group.optInt("version"));
                        save(
                            dialog, "POST", "/groups/" + group.optString("id") + "/transfer", body);
                      } catch (Exception error) {
                        host().message("Check quantity and review date.");
                      }
                    }));
    dialog.show();
  }

  private void complete(JSONObject group) {
    if (!repo().online) {
      host().message("Connect and refresh before recording an outcome.");
      return;
    }
    LinearLayout form = Ui.form(requireContext());
    EditText quantity =
        Ui.field(
            form,
            "Egg count",
            String.valueOf(group.optInt("quantity")),
            InputType.TYPE_CLASS_NUMBER);
    Spinner outcome =
        Ui.select(
            form,
            "Actual outcome",
            group.optString("location").startsWith("C3")
                ? new String[] {"Harvested", "Hatched", "Removed", "Discarded"}
                : new String[] {"Removed", "Discarded"});
    Ui.body(
        form,
        "Record what actually happened. This changes active quantities; a reminder acknowledgment"
            + " does not.");
    AlertDialog dialog =
        new AlertDialog.Builder(requireContext())
            .setTitle("Record outcome / removal")
            .setView(Ui.scroll(form))
            .setPositiveButton("Record", null)
            .setNegativeButton("Cancel", null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      try {
                        save(
                            dialog,
                            "POST",
                            "/groups/" + group.optString("id") + "/complete",
                            json(
                                "quantity",
                                Integer.parseInt(quantity.getText().toString()),
                                "outcome",
                                outcome.getSelectedItem().toString(),
                                "version",
                                group.optInt("version")));
                      } catch (Exception error) {
                        host().message("Enter a valid egg count.");
                      }
                    }));
    dialog.show();
  }

  private void save(AlertDialog dialog, String method, String path, JSONObject body) {
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    repo()
        .send(
            method,
            path,
            body,
            new FarmRepository.Callback() {
              public void success(JSONObject result) {
                if (!isAdded()) return;
                dialog.dismiss();
                host().refresh();
              }

              public void failure(int status, String message) {
                if (!isAdded()) return;
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                host().message(message);
                if (status == 409) host().refresh();
              }
            });
  }
}
