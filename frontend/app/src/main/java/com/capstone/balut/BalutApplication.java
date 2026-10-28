package com.capstone.balut;

import android.app.Application;
import com.capstone.balut.data.repositories.FarmRepository;

public class BalutApplication extends Application {
  public FarmRepository repository;

  @Override
  public void onCreate() {
    super.onCreate();
    repository = new FarmRepository(this);
  }
}
