package com.capstone.balut.screens.profile;

import com.capstone.balut.screens.BaseFragment;
import com.capstone.balut.utils.Ui;

public class HelpFragment extends BaseFragment {
  @Override
  protected void draw() {
    back();
    Ui.title(content, "A record for every egg", "A practical guide for the tri-stage prototype.");
    String[][] sections = {
      {
        "1. Register the collection",
        "Enter 1–30 eggs, source and the original incubation start. Choose an empty C1 tray"
            + " position. Day 0 begins at the saved time; Day 1 means 24 hours have elapsed."
      },
      {
        "2. Incubate in C1",
        "Two tray positions hold up to 30 eggs each. Turning schedules and environmental control"
            + " must run independently on the Pi. The app cannot compensate for unsupported"
            + " hardware."
      },
      {
        "3. Candle and review in C2",
        "Move the tray manually, then record its transfer. ML is pending; the Scan tab is a fixed 6"
            + " × 5 position map only. Use your own candling assessment for manual assignments."
      },
      {
        "4. Assign finishing groups",
        "Split groups into C3 levels 1–3 and rows A–E, with up to six eggs per row. Record each"
            + " purpose and operator-confirmed review target. The source date remains attached to"
            + " every group."
      },
      {
        "5. Review and record outcomes",
        "A target date requests human review. Acknowledging does not mark a harvest. Record actual"
            + " harvested, hatched, removed or discarded quantities separately."
      },
      {
        "Offline and synchronization",
        "Saved records remain readable. Only new registrations may be kept pending offline; they"
            + " reserve no capacity. Reconnect and synchronize in Settings. Transfers and hardware"
            + " commands require a confirmed connection."
      },
      {
        "Notifications",
        "Grant Android notification permission and enable reminders in Settings. Known local review"
            + " dates can notify while the app is closed, subject to Android restrictions."
            + " Immediate remote equipment push alerts are not implemented."
      },
      {
        "Real device and simulation",
        "Simulation has a visible banner and a separate backend database. It never operates"
            + " hardware. Real-device mode requires an HTTPS connection and a supported device"
            + " adapter; unavailable functions remain disabled."
      },
      {
        "Food and incubation decisions",
        "Candidate image labels do not prove edible penoy, microbial spoilage, hatchability or"
            + " readiness. Follow the client's and adviser's validated procedures. Shared motors"
            + " and shared chambers cannot act as independent per-row controls."
      },
      {
        "Connection troubleshooting",
        "For the Android emulator, start the backend and use http://10.0.2.2:8787. If the server"
            + " mode differs, select the matching workspace. A denied notification permission does"
            + " not remove overdue tasks from the app."
      }
    };
    for (String[] section : sections) {
      Ui.heading(content, section[0]);
      Ui.body(content, section[1]);
    }
  }
}
