package com.capstone.balut.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** The session token is encrypted with a non-exportable Android Keystore key. */
public class SessionStore {
  private final SharedPreferences prefs;

  public SessionStore(Context context) {
    prefs = context.getSharedPreferences("balut_settings", Context.MODE_PRIVATE);
  }

  public String get(String key, String fallback) {
    return prefs.getString(key, fallback);
  }

  public void set(String key, String value) {
    prefs.edit().putString(key, value).apply();
  }

  public String mode() {
    return get("mode", "simulation");
  }

  public String endpoint() {
    return get("url_" + mode(), mode().equals("simulation") ? "http://10.0.2.2:8787" : "");
  }

  public String prefix() {
    return mode() + "|" + endpoint();
  }

  private SecretKey key() throws Exception {
    KeyStore store = KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    if (!store.containsAlias("balut-session")) {
      KeyGenerator generator =
          KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
      generator.init(
          new KeyGenParameterSpec.Builder(
                  "balut-session", KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .build());
      generator.generateKey();
    }
    return (SecretKey) store.getKey("balut-session", null);
  }

  public void token(String token) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key());
    set(
        "token_" + prefix(),
        Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)
            + ":"
            + Base64.encodeToString(cipher.doFinal(token.getBytes("UTF-8")), Base64.NO_WRAP));
  }

  public String token() {
    try {
      String[] parts = get("token_" + prefix(), "").split(":");
      if (parts.length != 2) return "";
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          key(),
          new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
      return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), "UTF-8");
    } catch (Exception error) {
      return "";
    }
  }

  public void clearToken() {
    set("token_" + prefix(), "");
  }
}
