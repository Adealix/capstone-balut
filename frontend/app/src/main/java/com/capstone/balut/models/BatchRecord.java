package com.capstone.balut.models;

import org.json.JSONObject;

/** Simple read model; request and validation code stays outside the screen. */
public class BatchRecord {
  public final String id, name, source, started, notes;
  public final int total, version;
  public final boolean archived;

  public BatchRecord(JSONObject row) {
    id = row.optString("id");
    name = row.optString("name");
    source = row.optString("source");
    started = row.optString("started");
    notes = row.optString("notes");
    total = row.optInt("total");
    version = row.optInt("version");
    archived = row.optInt("archived") == 1;
  }
}
