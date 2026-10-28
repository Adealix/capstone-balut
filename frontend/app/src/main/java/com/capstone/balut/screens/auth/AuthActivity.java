package com.capstone.balut.screens.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import com.capstone.balut.*;
import com.capstone.balut.data.repositories.FarmRepository;
import com.capstone.balut.utils.Ui;
import org.json.JSONObject;

public class AuthActivity extends AppCompatActivity {
  private FarmRepository repository;
  private LinearLayout page;
  private boolean setup = false;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    repository = ((BalutApplication) getApplication()).repository;
    draw();
    check();
  }

  private void draw() {
    page = Ui.form(this);
    page.setPadding(Ui.dp(this, 26), Ui.dp(this, 35), Ui.dp(this, 26), Ui.dp(this, 20));
    ScrollView scroll = Ui.scroll(page);
    scroll.setBackgroundColor(android.graphics.Color.rgb(244, 247, 245));
    setContentView(scroll);
    ViewCompat.setOnApplyWindowInsetsListener(
        scroll,
        (v, insets) -> {
          Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(b.left, b.top, b.right, b.bottom);
          return insets;
        });
    ImageView egg = new ImageView(this);
    egg.setImageResource(R.drawable.ic_egg);
    page.addView(egg, new LinearLayout.LayoutParams(Ui.dp(this, 60), Ui.dp(this, 70)));
    Ui.title(
        page,
        "Every batch,\naccounted for.",
        "Balut Companion\nTri-stage incubation and human-reviewed decisions.");
    Ui.notice(
        page,
        repository.settings.mode().equals("simulation")
            ? "Simulation workspace"
            : "Real-device workspace",
        repository.settings.mode().equals("simulation")
            ? "Practice the complete workflow without operating any hardware."
            : "Only supported, connected device functions will be available.");
    Ui.heading(page, setup ? "Create the farm Owner" : "Sign in");
    EditText name = setup ? Ui.field(page, "Your name", "", InputType.TYPE_CLASS_TEXT) : null;
    EditText username =
        Ui.field(
            page,
            "Username",
            "",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    EditText password =
        Ui.field(
            page,
            "Password" + (setup ? " (at least 8 characters)" : ""),
            "",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    Button submit = Ui.button(page, setup ? "Create Owner account" : "Sign in", () -> {});
    submit.setOnClickListener(
        v -> {
          try {
            if (username.getText().toString().trim().isEmpty()
                || password.getText().length() == 0
                || (setup
                    && (name.getText().toString().trim().isEmpty()
                        || password.getText().length() < 8))) {
              error(
                  "Complete your name and credentials. A new password needs at least eight"
                      + " characters.");
              return;
            }
            JSONObject body =
                new JSONObject()
                    .put("username", username.getText().toString().trim())
                    .put("password", password.getText().toString());
            if (setup) body.put("name", name.getText().toString());
            submit.setEnabled(false);
            submit.setText("Please wait…");
            repository.send(
                "POST",
                setup ? "/auth/setup" : "/auth/login",
                body,
                new FarmRepository.Callback() {
                  public void success(JSONObject response) {
                    try {
                      repository.rememberSession(response);
                      Intent main = new Intent(AuthActivity.this, MainActivity.class);
                      if (getIntent().hasExtra("batch_id"))
                        main.putExtra("batch_id", getIntent().getStringExtra("batch_id"));
                      startActivity(main);
                      finish();
                    } catch (Exception error) {
                      error("Could not securely save the session on this device.");
                      submit.setEnabled(true);
                    }
                  }

                  public void failure(int status, String message) {
                    submit.setEnabled(true);
                    submit.setText(setup ? "Create Owner account" : "Sign in");
                    error(message);
                  }
                });
          } catch (Exception error) {
            error("Check the form and try again.");
          }
        });
    Ui.secondary(page, "Connection settings", this::connection);
    Ui.body(
        page,
        "Server: "
            + repository.settings.endpoint()
            + "\nNo public registration. The Owner creates Operator accounts.");
  }

  private void check() {
    repository.send(
        "GET",
        "/health",
        null,
        new FarmRepository.Callback() {
          public void success(JSONObject result) {
            if (!result.optString("mode").equals(repository.settings.mode())) {
              error("The server is in a different mode. Change the connection settings.");
              return;
            }
            setup = result.optBoolean("setup_required");
            draw();
          }

          public void failure(int status, String message) {
            Ui.body(page, message + " Start the backend and check the connection settings.");
          }
        });
  }

  private void connection() {
    LinearLayout form = Ui.form(this);
    Spinner mode = Ui.select(form, "Workspace", new String[] {"simulation", "real"});
    mode.setSelection(repository.settings.mode().equals("simulation") ? 0 : 1);
    EditText url =
        Ui.field(
            form,
            "Backend address",
            repository.settings.endpoint(),
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    Ui.body(
        form,
        "Emulator simulation: http://10.0.2.2:8787\n"
            + "Real device: HTTPS address with a trusted certificate. Each workspace has separate"
            + " records.");
    new AlertDialog.Builder(this)
        .setTitle("Connect to your workspace")
        .setView(Ui.scroll(form))
        .setPositiveButton(
            "Save & check",
            (d, w) -> {
              String selected = mode.getSelectedItem().toString();
              repository.settings.set("mode", selected);
              repository.settings.set(
                  "url_" + selected, url.getText().toString().trim().replaceAll("/+$", ""));
              repository.loadCache();
              draw();
              check();
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void error(String message) {
    new AlertDialog.Builder(this)
        .setTitle("Check connection or details")
        .setMessage(message)
        .setPositiveButton("OK", null)
        .show();
  }
}
