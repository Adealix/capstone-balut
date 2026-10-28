package com.capstone.balut.utils;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import com.capstone.balut.logic.IncubationCalculator;
import java.time.*;

/** Reusable visual pieces. Screen classes remain small and focus on the user's task. */
public final class Ui {
  public static final int INK = Color.rgb(23, 45, 40),
      GREEN = Color.rgb(25, 121, 91),
      MUTED = Color.rgb(91, 112, 104),
      PALE = Color.rgb(234, 243, 237),
      RED = Color.rgb(169, 53, 64),
      GOLD = Color.rgb(232, 182, 90);

  public static int dp(Context c, int value) {
    return Math.round(value * c.getResources().getDisplayMetrics().density);
  }

  public static GradientDrawable background(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(radius);
    return d;
  }

  public static TextView text(Context c, String value, int size, int color, boolean bold) {
    TextView t = new TextView(c);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setLineSpacing(dp(c, 3), 1);
    if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    return t;
  }

  public static void add(LinearLayout parent, View view) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = dp(parent.getContext(), 12);
    parent.addView(view, p);
  }

  public static LinearLayout column(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  public static LinearLayout card(LinearLayout parent) {
    Context c = parent.getContext();
    LinearLayout l = column(c);
    l.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
    l.setBackground(background(Color.WHITE, dp(c, 14)));
    add(parent, l);
    return l;
  }

  public static void title(LinearLayout p, String title, String subtitle) {
    add(p, text(p.getContext(), title, 29, INK, true));
    if (!subtitle.isEmpty()) add(p, text(p.getContext(), subtitle, 14, MUTED, false));
  }

  public static void heading(LinearLayout p, String title) {
    TextView t = text(p.getContext(), title, 18, INK, true);
    t.setPadding(0, dp(p.getContext(), 10), 0, 0);
    add(p, t);
  }

  public static void body(LinearLayout p, String value) {
    add(p, text(p.getContext(), value, 14, MUTED, false));
  }

  public static Button button(LinearLayout p, String label, Runnable click) {
    Button b = new Button(p.getContext());
    b.setText(label);
    b.setTextSize(14);
    b.setAllCaps(false);
    b.setTextColor(Color.WHITE);
    b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN));
    b.setMinHeight(dp(p.getContext(), 48));
    b.setOnClickListener(v -> click.run());
    add(p, b);
    return b;
  }

  public static Button secondary(LinearLayout p, String label, Runnable click) {
    Button b = button(p, label, click);
    b.setTextColor(GREEN);
    b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PALE));
    return b;
  }

  public static EditText field(LinearLayout p, String label, String value, int type) {
    Context c = p.getContext();
    add(p, text(c, label, 13, INK, true));
    EditText e = new EditText(c);
    e.setContentDescription(label);
    e.setId((label.hashCode() & 0x00ffffff) + 1);
    e.setSingleLine((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
    e.setInputType(type);
    e.setText(value);
    e.setTextSize(15);
    e.setTextColor(INK);
    e.setMinHeight(dp(c, 48));
    e.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
    GradientDrawable bg = background(Color.WHITE, dp(c, 8));
    bg.setStroke(dp(c, 1), Color.rgb(191, 209, 198));
    e.setBackground(bg);
    add(p, e);
    return e;
  }

  public static Spinner select(LinearLayout p, String label, String[] options) {
    add(p, text(p.getContext(), label, 13, INK, true));
    Spinner s = new Spinner(p.getContext());
    s.setContentDescription(label);
    s.setId((label.hashCode() & 0x00ffffff) + 1);
    s.setAdapter(
        new ArrayAdapter<>(p.getContext(), android.R.layout.simple_spinner_dropdown_item, options));
    s.setMinimumHeight(dp(p.getContext(), 48));
    add(p, s);
    return s;
  }

  public static EditText date(LinearLayout p, String label, String iso) {
    EditText e = field(p, label, IncubationCalculator.formDate(iso), InputType.TYPE_CLASS_DATETIME);
    e.setFocusable(false);
    e.setContentDescription(label);
    e.setOnClickListener(
        v -> {
          ZonedDateTime d;
          try {
            d =
                Instant.parse(IncubationCalculator.fromForm(e.getText().toString()))
                    .atZone(IncubationCalculator.FARM_ZONE);
          } catch (Exception error) {
            d = ZonedDateTime.now(IncubationCalculator.FARM_ZONE);
          }
          final ZonedDateTime chosen = d;
          new DatePickerDialog(
                  p.getContext(),
                  (picker, y, m, day) ->
                      new TimePickerDialog(
                              p.getContext(),
                              (time, h, min) ->
                                  e.setText(
                                      String.format(
                                          java.util.Locale.US,
                                          "%04d-%02d-%02d %02d:%02d",
                                          y,
                                          m + 1,
                                          day,
                                          h,
                                          min)),
                              chosen.getHour(),
                              chosen.getMinute(),
                              false)
                          .show(),
                  d.getYear(),
                  d.getMonthValue() - 1,
                  d.getDayOfMonth())
              .show();
        });
    return e;
  }

  public static LinearLayout form(Context c) {
    LinearLayout l = column(c);
    l.setPadding(dp(c, 20), dp(c, 8), dp(c, 20), dp(c, 8));
    return l;
  }

  public static ScrollView scroll(LinearLayout form) {
    ScrollView s = new ScrollView(form.getContext());
    s.addView(form);
    return s;
  }

  public static void notice(LinearLayout p, String title, String detail) {
    LinearLayout l = card(p);
    l.setBackground(background(PALE, dp(p.getContext(), 12)));
    add(l, text(p.getContext(), title, 16, GREEN, true));
    body(l, detail);
  }

  public static void metric(LinearLayout p, String number, String label) {
    LinearLayout l = card(p);
    add(l, text(p.getContext(), number, 32, INK, true));
    body(l, label);
  }

  private Ui() {}
}
