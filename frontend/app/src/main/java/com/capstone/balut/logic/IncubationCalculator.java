package com.capstone.balut.logic;

import java.time.*;
import java.time.format.DateTimeFormatter;

public final class IncubationCalculator {
  public static final ZoneId FARM_ZONE = ZoneId.of("Asia/Manila");

  public static long elapsedDays(String start, String current) {
    return Math.max(0, Duration.between(Instant.parse(start), Instant.parse(current)).toDays());
  }

  public static String display(String iso) {
    try {
      return Instant.parse(iso)
          .atZone(FARM_ZONE)
          .format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a"));
    } catch (Exception error) {
      return "Not scheduled";
    }
  }

  public static String formDate(String iso) {
    try {
      return Instant.parse(iso)
          .atZone(FARM_ZONE)
          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    } catch (Exception error) {
      return "";
    }
  }

  public static String fromForm(String value) {
    return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        .atZone(FARM_ZONE)
        .toInstant()
        .toString();
  }

  public static boolean due(String target) {
    try {
      return !Instant.parse(target).isAfter(Instant.now());
    } catch (Exception error) {
      return false;
    }
  }

  private IncubationCalculator() {}
}
