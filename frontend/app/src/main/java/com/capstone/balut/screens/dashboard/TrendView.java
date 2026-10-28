package com.capstone.balut.screens.dashboard;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import com.capstone.balut.utils.Ui;
import java.time.Instant;
import org.json.*;

/** A small temperature plot; a line is never drawn across a gap over two minutes. */
public class TrendView extends View {
  private final JSONArray rows;

  public TrendView(Context c, JSONArray rows) {
    super(c);
    this.rows = rows;
    setContentDescription(
        "Recorded temperature trend for compartments C1 and C3. Numeric readings appear above.");
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
    float pad = Ui.dp(getContext(), 28);
    pen.setColor(Ui.MUTED);
    pen.setTextSize(Ui.dp(getContext(), 11));
    canvas.drawText("38 °C", 0, pad, pen);
    canvas.drawText("37 °C", 0, getHeight() - pad, pen);
    for (String stage : new String[] {"C1", "C3"}) {
      float lastX = 0, lastY = 0;
      long previous = 0;
      int points = 0;
      for (int i = 0; i < rows.length(); i++) {
        JSONObject r = rows.optJSONObject(i);
        if (!r.optString("compartment").equals(stage)) continue;
        try {
          long t = Instant.parse(r.getString("at")).getEpochSecond();
          float x = pad + (getWidth() - pad - 8) * i / Math.max(1, rows.length() - 1);
          float y =
              getHeight()
                  - pad
                  - (float) (r.getDouble("temperature") - 37) * (getHeight() - 2 * pad);
          pen.setColor(stage.equals("C1") ? Ui.GREEN : Ui.GOLD);
          pen.setStrokeWidth(Ui.dp(getContext(), 2));
          if (points > 0 && t - previous <= 120) canvas.drawLine(lastX, lastY, x, y, pen);
          canvas.drawCircle(x, y, Ui.dp(getContext(), 2), pen);
          lastX = x;
          lastY = y;
          previous = t;
          points++;
        } catch (Exception ignored) {
        }
      }
    }
    pen.setTextSize(Ui.dp(getContext(), 12));
    pen.setColor(Ui.GREEN);
    canvas.drawText("C1", pad, getHeight() - 4, pen);
    pen.setColor(Ui.GOLD);
    canvas.drawText("C3", pad + 60, getHeight() - 4, pen);
  }
}
