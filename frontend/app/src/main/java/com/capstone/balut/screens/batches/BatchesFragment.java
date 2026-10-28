package com.capstone.balut.screens.batches;

import android.text.*;
import android.widget.*;
import com.capstone.balut.logic.IncubationCalculator;
import com.capstone.balut.models.BatchRecord;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.utils.Ui;
import java.time.Instant;
import org.json.*;

public class BatchesFragment extends BaseFragment {
  private String query = "", filter = "Active";
  private LinearLayout results;

  @Override
  protected void draw() {
    Ui.title(content, "Your batches", "Original dates, clear locations and a complete history.");
    Ui.button(content, "Register a batch", () -> host().open(new BatchFormFragment()));
    EditText search =
        Ui.field(content, "Search name, batch ID or source", query, InputType.TYPE_CLASS_TEXT);
    Spinner status =
        Ui.select(content, "Show", new String[] {"Active", "Completed", "Archived", "All"});
    String[] filters = {"Active", "Completed", "Archived", "All"};
    for (int i = 0; i < filters.length; i++) if (filters[i].equals(filter)) status.setSelection(i);
    results = Ui.column(requireContext());
    Ui.add(content, results);
    fill();
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int count, int after) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            query = s.toString();
            fill();
          }

          public void afterTextChanged(Editable e) {}
        });
    status.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          public void onItemSelected(
              android.widget.AdapterView<?> p, android.view.View v, int i, long id) {
            filter = filters[i];
            fill();
          }

          public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
  }

  private void fill() {
    if (results == null) return;
    results.removeAllViews();
    JSONArray data = repo().list("batches");
    int count = 0;
    for (int i = 0; i < data.length(); i++) {
      BatchRecord b = new BatchRecord(data.optJSONObject(i));
      int current = active(b.id);
      if (filter.equals("Active") && (current == 0 || b.archived)
          || filter.equals("Completed") && (current > 0 || b.archived)
          || filter.equals("Archived") && !b.archived) continue;
      if (!(b.name + " " + b.id + " " + b.source)
          .toLowerCase(java.util.Locale.ROOT)
          .contains(query.toLowerCase(java.util.Locale.ROOT))) continue;
      LinearLayout card = Ui.card(results);
      Ui.add(card, Ui.text(requireContext(), b.name, 18, Ui.INK, true));
      Ui.body(
          card,
          b.id
              + "\n"
              + current
              + " active / "
              + b.total
              + " registered   •   "
              + (b.archived
                  ? "Archived"
                  : "Day "
                      + IncubationCalculator.elapsedDays(b.started, Instant.now().toString())));
      Ui.body(card, "Started " + IncubationCalculator.display(b.started));
      Ui.secondary(card, "View batch", () -> host().open(BatchDetailsFragment.create(b.id)));
      count++;
    }
    if (count == 0)
      Ui.notice(
          results,
          "No matching batches",
          "Register a collection, change the filter, or clear your search.");
  }
}
