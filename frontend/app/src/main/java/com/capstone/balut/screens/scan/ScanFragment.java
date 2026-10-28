package com.capstone.balut.screens.scan;

import android.view.Gravity;
import android.widget.*;
import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.utils.Ui;

public class ScanFragment extends BaseFragment {
  @Override
  protected void draw() {
    Ui.title(content, "Candling workspace", "C2 • 6 columns × 5 rows • 30 egg positions");
    Ui.notice(
        content,
        "ML integration pending",
        "The Raspberry Pi camera and trained model are not connected. No classifications shown here"
            + " are predictions.");
    LinearLayout tray = Ui.card(content);
    Ui.heading(tray, "Fixed tray map");
    Ui.body(
        tray, "A1 is the top-left position when viewed from the configured camera orientation.");
    for (int row = 0; row < 5; row++) {
      LinearLayout line = new LinearLayout(requireContext());
      line.setOrientation(LinearLayout.HORIZONTAL);
      for (int column = 1; column <= 6; column++) {
        String position = "" + (char) ('A' + row) + column;
        TextView cell = Ui.text(requireContext(), position, 13, Ui.MUTED, true);
        cell.setGravity(Gravity.CENTER);
        cell.setBackground(Ui.background(Ui.PALE, Ui.dp(requireContext(), 7)));
        cell.setContentDescription(position + ", awaiting camera and ML");
        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(0, Ui.dp(requireContext(), 48), 1);
        p.setMargins(2, 2, 2, 2);
        line.addView(cell, p);
        cell.setOnClickListener(
            v ->
                host()
                    .message(
                        position
                            + ": camera and ML integration pending. No egg-presence or"
                            + " classification result is available."));
      }
      Ui.add(tray, line);
    }
    Ui.heading(content, "Human review stays in control");
    Ui.body(
        content,
        "Future candidate labels: Developing, Non-developing, Abnormal and Recheck. Each result"
            + " will retain its image, position, model version and human review decision.");
    Ui.body(
        content,
        "For now, record your own candling assessment when transferring a batch from C2 into"
            + " finishing rows. An abnormal image label does not establish microbial spoilage or"
            + " food safety.");
    Ui.secondary(content, "Open batches for manual review", () -> host().select(3));
  }
}
