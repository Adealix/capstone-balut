package com.capstone.balut.data.repositories;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.capstone.balut.data.local.*;
import com.capstone.balut.data.remote.PiApiClient;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/** One data entry point for all screens. UI callbacks always return on the main thread. */
public class FarmRepository {
  public interface Callback {
    void success(JSONObject result);

    void failure(int status, String message);
  }

  public final SessionStore settings;
  public final LocalDatabase database;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final PiApiClient api = new PiApiClient();
  private JSONObject snapshot = new JSONObject();
  public boolean online = false;
  public String lastError = "";

  public FarmRepository(Context context) {
    settings = new SessionStore(context);
    database = new LocalDatabase(context);
    loadCache();
  }

  public void loadCache() {
    try {
      snapshot = new JSONObject(database.read(settings.prefix()));
    } catch (Exception error) {
      snapshot = new JSONObject();
    }
    online = false;
  }

  public JSONObject snapshot() {
    return snapshot;
  }

  public JSONArray list(String name) {
    JSONArray result = snapshot.optJSONArray(name);
    return result == null ? new JSONArray() : result;
  }

  public JSONObject user() {
    JSONObject result = snapshot.optJSONObject("user");
    return result == null ? new JSONObject() : result;
  }

  public boolean owner() {
    return user().optString("role").equals("Owner");
  }

  public String pendingOwner() {
    return settings.prefix() + "|" + user().optString("id");
  }

  public JSONArray pending() {
    return database.pending(pendingOwner());
  }

  public void send(String method, String path, JSONObject body, Callback callback) {
    sendId(method, path, body, UUID.randomUUID().toString(), callback);
  }

  public void sendId(String method, String path, JSONObject body, String id, Callback callback) {
    String endpoint = settings.endpoint(), token = settings.token(), mode = settings.mode();
    executor.execute(
        () -> {
          try {
            JSONObject result = api.request(endpoint, token, method, path, body, id);
            if (path.equals("/snapshot") && !result.optString("mode").equals(mode))
              throw new PiApiClient.ApiException(
                  409, "Server mode differs from the app. Choose the correct connection.");
            main.post(
                () -> {
                  if (!endpoint.equals(settings.endpoint()) || !mode.equals(settings.mode()))
                    return;
                  online = true;
                  lastError = "";
                  if (path.equals("/snapshot")) {
                    snapshot = result;
                    database.save(settings.prefix(), result.toString());
                    settings.set("synced_" + settings.prefix(), java.time.Instant.now().toString());
                  }
                  callback.success(result);
                });
          } catch (PiApiClient.ApiException error) {
            main.post(
                () -> {
                  if (error.status == 0) online = false;
                  lastError = error.getMessage();
                  callback.failure(error.status, error.getMessage());
                });
          }
        });
  }

  public void refresh(Callback callback) {
    send("GET", "/snapshot", null, callback);
  }

  public void rememberSession(JSONObject result) throws Exception {
    settings.token(result.getString("token"));
    settings.set("expires_" + settings.prefix(), result.getString("expires"));
    snapshot.put("user", result.getJSONObject("user"));
    database.save(settings.prefix(), snapshot.toString());
  }

  public boolean hasSession() {
    try {
      return !settings.token().isEmpty()
          && java.time.Instant.parse(settings.get("expires_" + settings.prefix(), ""))
              .isAfter(java.time.Instant.now());
    } catch (Exception error) {
      return false;
    }
  }

  public void queueBatch(String requestId, JSONObject payload) {
    database.queue(requestId, pendingOwner(), payload.toString());
  }

  public void synchronizePending(Callback callback) {
    JSONArray items = pending();
    if (items.length() == 0) {
      callback.success(new JSONObject());
      return;
    }
    JSONObject item = items.optJSONObject(0);
    String id = item.optString("id");
    sendId(
        "POST",
        "/batches",
        item.optJSONObject("payload"),
        id,
        new Callback() {
          public void success(JSONObject result) {
            database.removePending(id);
            synchronizePending(callback);
          }

          public void failure(int status, String message) {
            callback.failure(status, "Pending registration needs attention: " + message);
          }
        });
  }
}
