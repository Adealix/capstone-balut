package com.capstone.balut;

import static org.junit.Assert.*;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.capstone.balut.data.repositories.FarmRepository;
import com.capstone.balut.screens.batches.BatchDetailsFragment;
import com.capstone.balut.screens.profile.SettingsFragment;
import com.capstone.balut.screens.profile.TraysFragment;
import java.time.Instant;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** In-memory screen fixtures: no requests, records, sessions or reminder changes on a live farm. */
@RunWith(AndroidJUnit4.class)
public class UiFeedbackTest {
  private BalutApplication application;
  private FarmRepository original;
  private FakeRepository fake;
  private ActivityScenario<MainActivity> scenario;

  private static JSONObject object(Object... values) {
    JSONObject result = new JSONObject();
    try {
      for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
    } catch (Exception error) {
      throw new IllegalArgumentException(error);
    }
    return result;
  }

  private static class FakeRepository extends FarmRepository {
    final JSONObject data;
    int calls = 0;
    boolean rejectSave = false;

    FakeRepository(Context context) {
      super(context);
      data =
          object(
              "farm_name",
              "Fixture farm",
              "user",
              object(
                  "id",
                  "fixture-owner",
                  "username",
                  "fixture-owner",
                  "name",
                  "Fixture Owner",
                  "role",
                  "Owner",
                  "active",
                  1),
              "batches",
              new JSONArray()
                  .put(
                      object(
                          "id",
                          "batch-fixture",
                          "name",
                          "Fixture batch",
                          "total",
                          12,
                          "started",
                          Instant.now().minusSeconds(604800).toString(),
                          "version",
                          1)),
              "groups",
              new JSONArray()
                  .put(
                      object(
                          "id",
                          "group-fixture",
                          "batch_id",
                          "batch-fixture",
                          "quantity",
                          12,
                          "location",
                          "C1-1",
                          "row_name",
                          "",
                          "candidate",
                          "Unreviewed",
                          "purpose",
                          "Unassigned",
                          "target",
                          "",
                          "version",
                          1)),
              "trays",
              new JSONArray()
                  .put(object("id", "T01", "state", "In use", "group_id", "group-fixture")));
      online = true;
    }

    @Override
    public boolean hasSession() {
      return true;
    }

    @Override
    public JSONObject snapshot() {
      return data;
    }

    @Override
    public JSONObject user() {
      return data.optJSONObject("user");
    }

    @Override
    public JSONArray list(String name) {
      JSONArray rows = data.optJSONArray(name);
      return rows == null ? new JSONArray() : rows;
    }

    @Override
    public JSONArray pending() {
      return new JSONArray();
    }

    @Override
    public void refresh(Callback callback) {
      /* No live requests or reminder scheduling. */
    }

    @Override
    public void send(String method, String path, JSONObject body, Callback callback) {
      calls++;
      if (rejectSave) callback.failure(503, "Fixture server unavailable.");
      else callback.success(object("saved", true));
    }
  }

  @Before
  public void openFixture() {
    application =
        (BalutApplication)
            InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
    original = application.repository;
    fake = new FakeRepository(application);
    application.repository = fake;
    scenario = ActivityScenario.launch(MainActivity.class);
  }

  @After
  public void closeFixture() {
    if (scenario != null) scenario.close();
    application.repository = original;
  }

  private static TextView text(View root, String wanted) {
    if (root instanceof TextView && ((TextView) root).getText().toString().equals(wanted))
      return (TextView) root;
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int i = 0; i < group.getChildCount(); i++) {
        TextView found = text(group.getChildAt(i), wanted);
        if (found != null) return found;
      }
    }
    return null;
  }

  private static EditText farmField(View root) {
    if (root instanceof EditText && "Farm name".contentEquals(root.getContentDescription()))
      return (EditText) root;
    if (root instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
        EditText found = farmField(((ViewGroup) root).getChildAt(i));
        if (found != null) return found;
      }
    return null;
  }

  @Test
  public void inventoryShowsExplicitStatusAndAssignment() {
    scenario.onActivity(
        activity -> {
          activity.open(new TraysFragment());
          activity.getSupportFragmentManager().executePendingTransactions();
          View root = activity.findViewById(R.id.screen_container);
          assertNotNull(text(root, "Status: In use"));
          assertNotNull(text(root, "Batch: Fixture batch\nLocation: C1-1"));
        });
  }

  @Test
  public void batchDetailsShowsAssignedCarrier() {
    scenario.onActivity(
        activity -> {
          activity.open(BatchDetailsFragment.create("batch-fixture"));
          activity.getSupportFragmentManager().executePendingTransactions();
          assertNotNull(
              text(activity.findViewById(R.id.screen_container), "Physical carrier: T01 · In use"));
        });
  }

  @Test
  public void successfulFarmSaveShowsPersistentConfirmation() {
    scenario.onActivity(
        activity -> {
          activity.open(new SettingsFragment());
          activity.getSupportFragmentManager().executePendingTransactions();
          View root = activity.findViewById(R.id.screen_container);
          farmField(root).setText("Renamed fixture farm");
          ((Button) text(root, "Save farm name")).performClick();
          assertEquals(1, fake.calls);
          assertNotNull(text(root, "Farm name saved."));
          assertTrue(text(root, "Save farm name").isEnabled());
        });
  }

  @Test
  public void failedSaveDoesNotClaimSuccess() {
    fake.rejectSave = true;
    scenario.onActivity(
        activity -> {
          activity.open(new SettingsFragment());
          activity.getSupportFragmentManager().executePendingTransactions();
          View root = activity.findViewById(R.id.screen_container);
          ((Button) text(root, "Save farm name")).performClick();
          assertNotNull(text(root, "Save failed: Fixture server unavailable."));
          assertNull(text(root, "Farm name saved."));
          assertTrue(text(root, "Save farm name").isEnabled());
        });
  }

  @Test
  public void blankFarmNameStaysInFormWithoutRequest() {
    scenario.onActivity(
        activity -> {
          activity.open(new SettingsFragment());
          activity.getSupportFragmentManager().executePendingTransactions();
          View root = activity.findViewById(R.id.screen_container);
          EditText field = farmField(root);
          field.setText("  ");
          ((Button) text(root, "Save farm name")).performClick();
          assertEquals(0, fake.calls);
          assertEquals("Enter a farm name.", field.getError().toString());
        });
  }
}
