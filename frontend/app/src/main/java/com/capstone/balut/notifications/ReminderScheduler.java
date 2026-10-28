package com.capstone.balut.notifications;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import com.capstone.balut.MainActivity;
import com.capstone.balut.R;
import java.time.*;
import org.json.*;

/** Inexact local reminders for known review dates. This is not a remote equipment alarm. */
public final class ReminderScheduler {
  private static SharedPreferences prefs(Context c) {
    return c.getSharedPreferences("balut_reminders", Context.MODE_PRIVATE);
  }

  public static void update(Context c, JSONObject snapshot) {
    JSONArray alerts = snapshot.optJSONArray("alerts"), groups = snapshot.optJSONArray("groups");
    if (alerts == null) return;
    try {
      JSONArray records = new JSONArray();
      for (int i = 0; i < alerts.length(); i++) {
        JSONObject a = alerts.getJSONObject(i);
        if (!a.optString("state").equals("open")) {
          ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE))
              .cancel(a.optString("id").hashCode());
          continue;
        }
        JSONObject r = new JSONObject(a.toString());
        if (groups != null)
          for (int j = 0; j < groups.length(); j++)
            if (groups.getJSONObject(j).optString("id").equals(a.optString("group_id")))
              r.put("batch_id", groups.getJSONObject(j).optString("batch_id"));
        records.put(r);
      }
      prefs(c).edit().putString("alerts", records.toString()).apply();
      schedule(c);
    } catch (Exception ignored) {
    }
  }

  public static boolean enabled(Context c) {
    return prefs(c).getBoolean("enabled", true);
  }

  public static void enabled(Context c, boolean enabled) {
    prefs(c).edit().putBoolean("enabled", enabled).apply();
    if (enabled) schedule(c);
    else cancel(c);
  }

  private static PendingIntent alarm(Context c) {
    return PendingIntent.getBroadcast(
        c,
        10,
        new Intent(c, ReminderReceiver.class).setAction("com.capstone.balut.REVIEW"),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  private static void cancel(Context c) {
    ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(alarm(c));
  }

  public static void clear(Context c) {
    prefs(c).edit().putString("alerts", "[]").apply();
    cancel(c);
    ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
  }

  public static void schedule(Context c) {
    cancel(c);
    if (!enabled(c)) return;
    long earliest = Long.MAX_VALUE;
    try {
      JSONArray rows = new JSONArray(prefs(c).getString("alerts", "[]"));
      for (int i = 0; i < rows.length(); i++) {
        JSONObject a = rows.getJSONObject(i);
        long due = Instant.parse(a.getString("due")).toEpochMilli();
        if (!a.optString("snoozed_until").isEmpty())
          due = Math.max(due, Instant.parse(a.getString("snoozed_until")).toEpochMilli());
        due =
            Math.max(due, prefs(c).getLong("last_" + a.getString("id"), 0) + 24 * 60 * 60 * 1000L);
        earliest = Math.min(earliest, due);
      }
    } catch (Exception ignored) {
    }
    if (earliest != Long.MAX_VALUE)
      ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE))
          .setAndAllowWhileIdle(
              AlarmManager.RTC_WAKEUP,
              Math.max(System.currentTimeMillis() + 3000, earliest),
              alarm(c));
  }

  public static void deliver(Context c) {
    if (!enabled(c)) return;
    try {
      JSONArray rows = new JSONArray(prefs(c).getString("alerts", "[]"));
      long current = System.currentTimeMillis();
      for (int i = 0; i < rows.length(); i++) {
        JSONObject a = rows.getJSONObject(i);
        long due = Instant.parse(a.getString("due")).toEpochMilli();
        if (!a.optString("snoozed_until").isEmpty())
          due = Math.max(due, Instant.parse(a.getString("snoozed_until")).toEpochMilli());
        if (due <= current
            && current - prefs(c).getLong("last_" + a.getString("id"), 0) >= 24 * 60 * 60 * 1000L) {
          notify(
              c,
              a.getString("id").hashCode(),
              a.getString("title"),
              "Scheduled review target reached. Open the batch and confirm the next action.",
              a.optString("batch_id"));
          prefs(c).edit().putLong("last_" + a.getString("id"), current).apply();
        }
      }
    } catch (Exception ignored) {
    }
    schedule(c);
  }

  public static void test(Context c) {
    notify(
        c,
        9001,
        "Test review reminder",
        "Notifications are working. This is a test, not a real harvest target.",
        "");
  }

  private static void notify(Context c, int id, String title, String message, String batch) {
    NotificationManager manager =
        (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    manager.createNotificationChannel(
        new NotificationChannel(
            "reviews", "Scheduled reviews", NotificationManager.IMPORTANCE_DEFAULT));
    if (android.os.Build.VERSION.SDK_INT >= 33
        && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return;
    Intent open =
        new Intent(c, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    if (!batch.isEmpty()) open.putExtra("batch_id", batch);
    PendingIntent intent =
        PendingIntent.getActivity(
            c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    manager.notify(
        id,
        new Notification.Builder(c, "reviews")
            .setSmallIcon(R.drawable.ic_egg)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new Notification.BigTextStyle().bigText(message))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build());
  }

  private ReminderScheduler() {}
}
