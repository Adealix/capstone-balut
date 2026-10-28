package com.capstone.balut;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.capstone.balut.data.repositories.FarmRepository;
import com.capstone.balut.notifications.ReminderScheduler;
import com.capstone.balut.screens.*;
import com.capstone.balut.screens.alerts.AlertsFragment;
import com.capstone.balut.screens.auth.AuthActivity;
import com.capstone.balut.screens.batches.*;
import com.capstone.balut.screens.dashboard.DashboardFragment;
import com.capstone.balut.screens.home.HomeFragment;
import com.capstone.balut.screens.profile.ProfileFragment;
import com.capstone.balut.screens.scan.ScanFragment;
import com.capstone.balut.utils.FileExports;
import com.capstone.balut.utils.Ui;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
  public FarmRepository repository;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private boolean refreshing = false;
  private boolean attemptedPendingSync = false;
  private int selected = 0;
  private final Runnable poll =
      new Runnable() {
        public void run() {
          refresh();
          handler.postDelayed(this, 20000);
        }
      };
  private String exportText = "", exportKind = "";

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    repository = ((BalutApplication) getApplication()).repository;
    if (!repository.hasSession()) {
      openLogin();
      return;
    }
    setContentView(R.layout.activity_main);
    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(R.id.app_root),
        (view, insets) -> {
          Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return insets;
        });
    findViewById(R.id.alerts_button).setOnClickListener(v -> open(new AlertsFragment()));
    buildNavigation();
    if (saved == null) select(0);
    else selected = saved.getInt("selected", 0);
    updateBanner();
    if (android.os.Build.VERSION.SDK_INT >= 33
        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 70);
    if (getIntent().hasExtra("batch_id"))
      open(BatchDetailsFragment.create(getIntent().getStringExtra("batch_id")));
  }

  private void buildNavigation() {
    LinearLayout nav = findViewById(R.id.bottom_navigation);
    String[] labels = {"Home", "Dashboard", "Scan", "Batches", "Profile"};
    int[] icons = {
      R.drawable.ic_home,
      R.drawable.ic_dashboard,
      R.drawable.ic_scan,
      R.drawable.ic_batches,
      R.drawable.ic_profile
    };
    for (int i = 0; i < labels.length; i++) {
      final int index = i;
      LinearLayout tab = Ui.column(this);
      tab.setGravity(android.view.Gravity.CENTER);
      tab.setContentDescription(labels[i] + " tab");
      ImageView icon = new ImageView(this);
      icon.setImageResource(icons[i]);
      tab.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 22), Ui.dp(this, 22)));
      TextView label = Ui.text(this, labels[i], 11, Ui.MUTED, false);
      label.setGravity(android.view.Gravity.CENTER);
      tab.addView(label);
      nav.addView(tab, new LinearLayout.LayoutParams(0, -1, 1));
      tab.setOnClickListener(v -> select(index));
    }
  }

  public void select(int tab) {
    selected = tab;
    getSupportFragmentManager()
        .popBackStackImmediate(
            null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
    Fragment[] screens = {
      new HomeFragment(),
      new DashboardFragment(),
      new ScanFragment(),
      new BatchesFragment(),
      new ProfileFragment()
    };
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.screen_container, screens[tab])
        .commit();
    LinearLayout nav = findViewById(R.id.bottom_navigation);
    for (int i = 0; i < nav.getChildCount(); i++)
      nav.getChildAt(i).setBackgroundColor(i == tab ? Ui.PALE : Color.WHITE);
  }

  public void open(Fragment screen) {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.screen_container, screen)
        .addToBackStack(null)
        .commit();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putInt("selected", selected);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (repository != null && repository.hasSession()) handler.post(poll);
  }

  @Override
  protected void onPause() {
    super.onPause();
    handler.removeCallbacks(poll);
  }

  public void openLogin() {
    Intent login = new Intent(this, AuthActivity.class);
    if (getIntent().hasExtra("batch_id"))
      login.putExtra("batch_id", getIntent().getStringExtra("batch_id"));
    startActivity(login);
    finish();
  }

  public void refresh() {
    if (refreshing || !repository.hasSession()) return;
    refreshing = true;
    repository.refresh(
        new FarmRepository.Callback() {
          public void success(JSONObject result) {
            refreshing = false;
            updateBanner();
            ReminderScheduler.update(MainActivity.this, result);
            redraw();
            if (!attemptedPendingSync && repository.pending().length() > 0) {
              attemptedPendingSync = true;
              repository.synchronizePending(
                  new FarmRepository.Callback() {
                    public void success(JSONObject saved) {
                      refresh();
                    }

                    public void failure(int status, String detail) {
                      message(detail);
                    }
                  });
            }
          }

          public void failure(int status, String message) {
            refreshing = false;
            if (status == 0) attemptedPendingSync = false;
            updateBanner();
            if (status == 401) {
              repository.settings.clearToken();
              openLogin();
            } else redraw();
          }
        });
  }

  public void redraw() {
    Fragment current = getSupportFragmentManager().findFragmentById(R.id.screen_container);
    if (current instanceof BaseFragment && ((BaseFragment) current).refreshesAutomatically())
      ((BaseFragment) current).render();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    if (intent.hasExtra("batch_id") && repository.hasSession())
      open(BatchDetailsFragment.create(intent.getStringExtra("batch_id")));
  }

  private void updateBanner() {
    TextView banner = findViewById(R.id.connection_banner);
    if (banner == null) return;
    String mode =
        repository.settings.mode().equals("simulation")
            ? "Simulation • no hardware"
            : "Real device";
    banner.setText(
        mode
            + "  |  "
            + (repository.online ? "Connected" : "Offline • saved records")
            + (repository.pending().length() > 0
                ? "  |  " + repository.pending().length() + " pending"
                : ""));
    banner.setTextColor(repository.online ? Ui.GREEN : Ui.RED);
  }

  public void message(String text) {
    if (!isFinishing())
      new AlertDialog.Builder(this)
          .setTitle("Balut Companion")
          .setMessage(text)
          .setPositiveButton("OK", null)
          .show();
  }

  public void mutate(String method, String path, JSONObject data, Runnable finished) {
    repository.send(
        method,
        path,
        data,
        new FarmRepository.Callback() {
          public void success(JSONObject result) {
            if (finished != null) finished.run();
            refresh();
          }

          public void failure(int status, String message) {
            message(message);
            if (status == 409) refresh();
          }
        });
  }

  public void saveDocument(String filename, String mime, String text, String kind) {
    exportText = text;
    exportKind = kind;
    repository.database.save("export-text-" + repository.settings.prefix(), text);
    repository.database.save("export-kind-" + repository.settings.prefix(), kind);
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType(mime);
    intent.putExtra(Intent.EXTRA_TITLE, filename);
    startActivityForResult(intent, 501);
  }

  public void openRestore() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/json");
    startActivityForResult(intent, 502);
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    android.net.Uri uri = data.getData();
    if (request == 501) {
      final String content =
          exportText.isEmpty()
              ? repository.database.read("export-text-" + repository.settings.prefix())
              : exportText;
      final String kind =
          exportKind.isEmpty()
              ? repository.database.read("export-kind-" + repository.settings.prefix())
              : exportKind;
      new Thread(
              () -> {
                try {
                  FileExports.write(this, uri, content, kind);
                  runOnUiThread(() -> message("File saved."));
                } catch (Exception error) {
                  runOnUiThread(
                      () ->
                          message(
                              "Could not save the file. Choose a writable folder and try again."));
                }
              })
          .start();
    }
    if (request == 502)
      new Thread(
              () -> {
                try {
                  String content = FileExports.read(this, uri);
                  JSONObject backup = new JSONObject(content);
                  runOnUiThread(
                      () -> {
                        EditText confirm = new EditText(this);
                        confirm.setHint("Type RESTORE");
                        new AlertDialog.Builder(this)
                            .setTitle("Replace operational records?")
                            .setMessage(
                                "This replaces the selected server's batches, groups, alerts and"
                                    + " history. Accounts remain. Export a backup first.")
                            .setView(confirm)
                            .setPositiveButton(
                                "Restore",
                                (d, w) -> {
                                  if (confirm.getText().toString().equals("RESTORE"))
                                    mutate(
                                        "POST",
                                        "/restore",
                                        backup,
                                        () -> message("Backup restored."));
                                  else message("Restore canceled: confirmation did not match.");
                                })
                            .setNegativeButton("Cancel", null)
                            .show();
                      });
                } catch (Exception error) {
                  runOnUiThread(() -> message("Invalid or unreadable JSON backup."));
                }
              })
          .start();
  }
}
