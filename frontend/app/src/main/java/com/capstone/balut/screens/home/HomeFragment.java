package com.capstone.balut.screens.home;

import android.graphics.Color;
import android.widget.*;
import com.capstone.balut.logic.IncubationCalculator;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.screens.alerts.AlertsFragment;
import com.capstone.balut.screens.batches.*;
import com.capstone.balut.screens.profile.ReportsFragment;
import com.capstone.balut.utils.Ui;
import java.time.*;
import org.json.*;

public class HomeFragment extends BaseFragment {
  @Override
  protected void draw() {
    String name = repo().user().optString("name", "Operator");
    Ui.title(content, "Your farm, today.", "Welcome, " + name + ". Keep every batch on track.");
    LinearLayout overview = Ui.card(content);
    overview.setBackground(Ui.background(Ui.GREEN, Ui.dp(requireContext(), 16)));
    Ui.add(overview, Ui.text(requireContext(), String.valueOf(active("")), 44, Color.WHITE, true));
    Ui.add(
        overview,
        Ui.text(requireContext(), "eggs currently in incubation", 15, Color.WHITE, false));
    int activeBatches = 0;
    JSONArray batches = repo().list("batches");
    for (int i = 0; i < batches.length(); i++)
      if (active(batches.optJSONObject(i).optString("id")) > 0) activeBatches++;
    Ui.add(
        overview,
        Ui.text(
            requireContext(),
            activeBatches
                + " active batches  •  "
                + repo().snapshot().optString("farm_name", "Your farm"),
            13,
            Color.WHITE,
            false));
    Ui.button(content, "Register a batch", () -> host().open(new BatchFormFragment()));
    Ui.heading(content, "What needs attention");
    JSONArray alerts = repo().list("alerts");
    int due = 0;
    for (int i = 0; i < alerts.length(); i++) {
      JSONObject alert = alerts.optJSONObject(i);
      if (alert.optString("state").equals("resolved")
          || !IncubationCalculator.due(alert.optString("due"))) continue;
      if (due++ >= 2) continue;
      LinearLayout card = Ui.card(content);
      Ui.add(card, Ui.text(requireContext(), alert.optString("title"), 16, Ui.INK, true));
      Ui.body(
          card,
          "Review target: "
              + IncubationCalculator.display(alert.optString("due"))
              + "\n"
              + alert.optString("state"));
      Ui.secondary(card, "Open review", () -> host().open(new AlertsFragment()));
    }
    if (due == 0)
      Ui.notice(
          content,
          "No reviews due",
          "New scheduled reviews will appear here. A target date is a reminder for human review.");
    if (repo().pending().length() > 0)
      Ui.notice(
          content,
          "Registrations waiting to sync",
          repo().pending().length()
              + " pending registrations. Review them in Profile → Settings. They do not occupy a"
              + " confirmed tray yet.");
    Ui.heading(content, "Three stages, one record");
    Ui.body(
        content,
        "C1  Initial incubation and turning\n"
            + "C2  Candling and human review\n"
            + "C3  Finishing, scheduled reviews and outcomes");
    Ui.secondary(content, "View all batches", () -> host().select(3));
    Ui.secondary(content, "Reports & exports", () -> host().open(new ReportsFragment()));
    if (batches.length() == 0) {
      Ui.notice(
          content,
          "Start with your first collection",
          "Register up to 30 eggs in either of the two C1 tray positions. The original start date"
              + " follows every split group.");
      if (repo().owner() && repo().settings.mode().equals("simulation"))
        Ui.secondary(
            content,
            "Load sample farm records",
            () -> host().mutate("POST", "/demo/seed", json(), null));
    }
    Ui.body(
        content,
        "Last successful sync: "
            + IncubationCalculator.display(
                repo().settings.get("synced_" + repo().settings.prefix(), "")));
  }
}
