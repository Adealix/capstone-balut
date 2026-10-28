package com.capstone.balut.logic;

import static org.junit.Assert.*;

import org.junit.Test;

public class IncubationCalculatorTest {
  @Test
  public void initialDayIsZero() {
    assertEquals(
        0, IncubationCalculator.elapsedDays("2026-10-01T00:00:00Z", "2026-10-01T23:59:59Z"));
  }

  @Test
  public void sevenWholeDays() {
    assertEquals(
        7, IncubationCalculator.elapsedDays("2026-10-01T00:00:00Z", "2026-10-08T00:00:00Z"));
  }

  @Test
  public void manilaFormHasExplicitZone() {
    assertEquals("2026-10-01T00:00:00Z", IncubationCalculator.fromForm("2026-10-01 08:00"));
  }

  @Test
  public void dateRoundTrip() {
    assertEquals(
        "2026-10-28 18:30",
        IncubationCalculator.formDate(IncubationCalculator.fromForm("2026-10-28 18:30")));
  }

  @Test
  public void negativeAgeNeverDisplayed() {
    assertEquals(
        0, IncubationCalculator.elapsedDays("2026-10-02T00:00:00Z", "2026-10-01T00:00:00Z"));
  }
}
