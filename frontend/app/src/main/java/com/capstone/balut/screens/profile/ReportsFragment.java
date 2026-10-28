package com.capstone.balut.screens.profile;

import android.widget.*;
import com.capstone.balut.logic.IncubationCalculator;
import com.capstone.balut.models.BatchRecord;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.utils.FileExports;
import com.capstone.balut.utils.Ui;
import java.time.*;
import java.util.*;
import org.json.*;

public class ReportsFragment extends BaseFragment {
  @Override
  public boolean refreshesAutomatically() {
    return false;
  }

  private String from = Instant.now().minus(Duration.ofDays(30)).toString(),
      to = Instant.now().plus(Duration.ofDays(1)).toString();

  @Override
  protected void draw() {
    back();
    Ui.title(
        content, "Reports & exports", "Summaries based on recorded batches and actual outcomes.");
    EditText start = Ui.date(content, "Batches started from (Manila)", from);
    EditText end = Ui.date(content, "Batches started before (Manila)", to);
    Ui.button(
        content,
        "Apply date range",
        () -> {
          try {
            String a = IncubationCalculator.fromForm(start.getText().toString()),
                b = IncubationCalculator.fromForm(end.getText().toString());
            if (!Instant.parse(b).isAfter(Instant.parse(a))) throw new IllegalArgumentException();
            from = a;
            to = b;
            render();
          } catch (Exception error) {
            host().message("The end date must be after the start date.");
          }
        });
    JSONArray batches = repo().list("batches"), outcomes = repo().list("outcomes");
    Set<String> ids = new HashSet<>();
    int registered = 0, active = 0;
    StringBuilder csv =
        new StringBuilder("batch_id,name,source,original_start,registered,active,archived\n");
    StringBuilder report = new StringBuilder("BALUT COMPANION — BATCH REPORT\n");
    report
        .append("Generated: ")
        .append(IncubationCalculator.display(Instant.now().toString()))
        .append("\nWorkspace: ")
        .append(repo().settings.mode())
        .append("\nFarm: ")
        .append(repo().snapshot().optString("farm_name"))
        .append("\nBatch start range: ")
        .append(IncubationCalculator.display(from))
        .append(" to ")
        .append(IncubationCalculator.display(to))
        .append(" (end excluded)\n\n");
    for (int i = 0; i < batches.length(); i++) {
      BatchRecord b = new BatchRecord(batches.optJSONObject(i));
      Instant date = Instant.parse(b.started);
      if (date.isBefore(Instant.parse(from)) || !date.isBefore(Instant.parse(to))) continue;
      ids.add(b.id);
      registered += b.total;
      int current = active(b.id);
      active += current;
      csv.append(FileExports.csv(b.id))
          .append(',')
          .append(FileExports.csv(b.name))
          .append(',')
          .append(FileExports.csv(b.source))
          .append(',')
          .append(FileExports.csv(b.started))
          .append(',')
          .append(b.total)
          .append(',')
          .append(current)
          .append(',')
          .append(b.archived)
          .append('\n');
      report
          .append(b.name)
          .append(" | ")
          .append(b.id)
          .append("\n")
          .append("Registered: ")
          .append(b.total)
          .append("; active: ")
          .append(current)
          .append("\nOriginal start: ")
          .append(IncubationCalculator.display(b.started))
          .append("\n\n");
    }
    Map<String, Integer> totals = new LinkedHashMap<>();
    for (String type : new String[] {"Harvested", "Hatched", "Removed", "Discarded"})
      totals.put(type, 0);
    for (int i = 0; i < outcomes.length(); i++) {
      JSONObject outcome = outcomes.optJSONObject(i);
      if (ids.contains(outcome.optString("batch_id"))) {
        String type = outcome.optString("outcome");
        totals.put(type, totals.getOrDefault(type, 0) + outcome.optInt("quantity"));
      }
    }
    Ui.heading(content, "Selected batch cohort");
    Ui.body(
        content,
        ids.size() + " batches\n" + registered + " registered eggs\n" + active + " active eggs");
    report
        .append("TOTALS\n")
        .append(ids.size())
        .append(" batches; ")
        .append(registered)
        .append(" registered; ")
        .append(active)
        .append(" active\n");
    for (Map.Entry<String, Integer> row : totals.entrySet()) {
      Ui.body(content, row.getKey() + ": " + row.getValue());
      report.append(row.getKey()).append(": ").append(row.getValue()).append('\n');
    }
    String note =
        "Scope: batches selected by their original start date. Outcome totals include all saved"
            + " outcomes for those batches, not only outcomes occurring inside this date range."
            + " These counts do not establish hatchability, model accuracy, food safety, profit or"
            + " monthly production capacity.";
    Ui.body(content, note);
    report.append("\n").append(note);
    Ui.button(
        content,
        "Export batches as CSV",
        () -> host().saveDocument("balut-batches.csv", "text/csv", csv.toString(), "csv"));
    Ui.secondary(
        content,
        "Export summary as PDF",
        () -> host().saveDocument("balut-report.pdf", "application/pdf", report.toString(), "pdf"));
    Ui.heading(content, "Traceability");
    Ui.body(
        content,
        "Reports are computed from saved records. Missing outcomes remain unrecorded; the system"
            + " does not assume every egg became a saleable product.");
  }
}
