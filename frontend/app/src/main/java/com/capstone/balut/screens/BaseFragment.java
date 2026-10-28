package com.capstone.balut.screens;

import android.os.Bundle;
import android.view.*;
import android.widget.LinearLayout;
import androidx.fragment.app.Fragment;
import com.capstone.balut.*;
import com.capstone.balut.data.repositories.FarmRepository;
import com.capstone.balut.utils.Ui;
import org.json.*;

public abstract class BaseFragment extends Fragment {
  protected LinearLayout content;

  public MainActivity host() {
    return (MainActivity) requireActivity();
  }

  public FarmRepository repo() {
    return host().repository;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
    View view = inflater.inflate(R.layout.fragment_page, parent, false);
    content = view.findViewById(R.id.page_content);
    render();
    return view;
  }

  public final void render() {
    if (content == null || !isAdded()) return;
    content.removeAllViews();
    draw();
  }

  protected abstract void draw();

  public boolean refreshesAutomatically() {
    return true;
  }

  protected void back() {
    Ui.secondary(content, "Back", () -> host().getSupportFragmentManager().popBackStack());
  }

  protected JSONObject find(String table, String id) {
    JSONArray rows = repo().list(table);
    for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.optJSONObject(i);
      if (row.optString("id").equals(id)) return row;
    }
    return new JSONObject();
  }

  protected int active(String batch) {
    int n = 0;
    JSONArray rows = repo().list("groups");
    for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.optJSONObject(i);
      if (batch.isEmpty() || row.optString("batch_id").equals(batch)) n += row.optInt("quantity");
    }
    return n;
  }

  protected JSONObject json(Object... pairs) {
    JSONObject data = new JSONObject();
    try {
      for (int i = 0; i < pairs.length; i += 2) data.put((String) pairs[i], pairs[i + 1]);
    } catch (JSONException error) {
      throw new IllegalArgumentException(error);
    }
    return data;
  }
}
