package com.capstone.balut.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;

/** A small cache and outbox. Shared farm records remain authoritative on the server. */
public class LocalDatabase extends SQLiteOpenHelper {
  public LocalDatabase(Context context) {
    super(context, "balut_cache.db", null, 1);
  }

  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE cache(cache_key TEXT PRIMARY KEY, value TEXT NOT NULL)");
    db.execSQL(
        "CREATE TABLE outbox(id TEXT PRIMARY KEY, owner TEXT NOT NULL, payload TEXT NOT NULL)");
  }

  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // Add explicit migrations when the database version changes. Never delete farm data.
  }

  public synchronized void save(String key, String value) {
    ContentValues row = new ContentValues();
    row.put("cache_key", key);
    row.put("value", value);
    getWritableDatabase().insertWithOnConflict("cache", null, row, SQLiteDatabase.CONFLICT_REPLACE);
  }

  public synchronized String read(String key) {
    try (Cursor cursor =
        getReadableDatabase()
            .rawQuery("SELECT value FROM cache WHERE cache_key=?", new String[] {key})) {
      return cursor.moveToFirst() ? cursor.getString(0) : "";
    }
  }

  public synchronized void queue(String id, String owner, String payload) {
    ContentValues row = new ContentValues();
    row.put("id", id);
    row.put("owner", owner);
    row.put("payload", payload);
    getWritableDatabase().insertOrThrow("outbox", null, row);
  }

  public synchronized JSONArray pending(String owner) {
    JSONArray result = new JSONArray();
    try (Cursor cursor =
        getReadableDatabase()
            .rawQuery("SELECT id,payload FROM outbox WHERE owner=?", new String[] {owner})) {
      while (cursor.moveToNext()) {
        JSONObject row = new JSONObject();
        try {
          row.put("id", cursor.getString(0));
          row.put("payload", new JSONObject(cursor.getString(1)));
        } catch (Exception ignored) {
        }
        result.put(row);
      }
    }
    return result;
  }

  public synchronized void removePending(String id) {
    getWritableDatabase().delete("outbox", "id=?", new String[] {id});
  }
}
