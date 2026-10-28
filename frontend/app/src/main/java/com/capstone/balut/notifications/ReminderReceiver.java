package com.capstone.balut.notifications;

import android.content.*;

public class ReminderReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if ("com.capstone.balut.REVIEW".equals(intent.getAction())) ReminderScheduler.deliver(context);
    else ReminderScheduler.schedule(context);
  }
}
