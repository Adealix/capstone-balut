package com.capstone.balut.screens.profile;

import android.widget.LinearLayout;
import com.capstone.balut.logic.IncubationCalculator;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.utils.Ui;
import org.json.*;

public class ActivityFragment extends BaseFragment {
  @Override
  protected void draw() {
    back();
    Ui.title(
        content,
        "Activity history",
        "Latest 1,000 server events. Historical batch records remain in backups.");
    JSONArray rows = repo().list("events");
    for (int i = 0; i < Math.min(rows.length(), 100); i++) {
      JSONObject e = rows.optJSONObject(i);
      LinearLayout card = Ui.card(content);
      Ui.add(card, Ui.text(requireContext(), e.optString("action"), 16, Ui.INK, true));
      Ui.body(
          card,
          IncubationCalculator.display(e.optString("at"))
              + "\n"
              + e.optString("actor")
              + " · "
              + e.optString("entity"));
      Ui.secondary(card, "View details", () -> host().message(e.optString("detail")));
    }
    if (rows.length() == 0) Ui.body(content, "No events available yet.");
  }
}
