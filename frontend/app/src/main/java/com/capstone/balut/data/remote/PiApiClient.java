package com.capstone.balut.data.remote;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Synchronous by design: the repository always calls this on its background executor. */
public class PiApiClient {
  public static class ApiException extends Exception {
    public final int status;

    public ApiException(int status, String message) {
      super(message);
      this.status = status;
    }
  }

  public JSONObject request(
      String endpoint, String token, String method, String path, JSONObject body, String requestId)
      throws ApiException {
    HttpURLConnection connection = null;
    try {
      if (endpoint.isEmpty()) throw new ApiException(0, "Configure the device address first.");
      URL url = new URL(endpoint + path);
      if (!url.getProtocol().equals("https")
          && !(url.getProtocol().equals("http")
              && (url.getHost().equals("10.0.2.2")
                  || url.getHost().equals("127.0.0.1")
                  || url.getHost().equals("localhost"))))
        throw new ApiException(
            0, "Use HTTPS for a real network device. HTTP is limited to the emulator host.");
      connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(8000);
      connection.setReadTimeout(10000);
      connection.setRequestMethod(method);
      connection.setRequestProperty("Accept", "application/json");
      if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
      if (!requestId.isEmpty()) connection.setRequestProperty("X-Request-ID", requestId);
      if (body != null) {
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        try (java.io.OutputStream stream = connection.getOutputStream()) {
          stream.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
      }
      int status = connection.getResponseCode();
      InputStream stream = status < 400 ? connection.getInputStream() : connection.getErrorStream();
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      if (stream != null)
        try (InputStream input = stream) {
          byte[] buffer = new byte[4096];
          int length;
          while ((length = input.read(buffer)) != -1) {
            bytes.write(buffer, 0, length);
            if (bytes.size() > 8_000_000)
              throw new ApiException(0, "Response exceeds the app size limit.");
          }
        }
      String text = bytes.toString("UTF-8");
      JSONObject result =
          text.startsWith("[")
              ? new JSONObject().put("items", new JSONArray(text))
              : new JSONObject(text);
      if (status >= 400) {
        Object detail = result.opt("detail");
        throw new ApiException(
            status,
            detail instanceof JSONArray
                ? "Check the required fields, dates, and quantities."
                : result.optString("detail", "The server could not complete this request."));
      }
      return result;
    } catch (ApiException error) {
      throw error;
    } catch (Exception error) {
      throw new ApiException(
          0, "Cannot reach the backend. Check its address and make sure the server is running.");
    } finally {
      if (connection != null) connection.disconnect();
    }
  }
}
