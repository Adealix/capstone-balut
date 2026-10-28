package com.capstone.balut;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.capstone.balut.data.local.LocalDatabase;
import com.capstone.balut.data.local.SessionStore;
import com.capstone.balut.notifications.ReminderScheduler;
import com.capstone.balut.utils.FileExports;
import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeviceIntegrationTest {
  private Context context() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext();
  }

  @Test
  public void cacheAndOutboxSurviveDatabaseReopen() throws Exception {
    LocalDatabase first = new LocalDatabase(context());
    first.save("instrumentation-test", "saved record");
    String id = java.util.UUID.randomUUID().toString();
    first.queue(id, "instrumentation-test", "{\"name\":\"fixture\"}");
    first.close();
    LocalDatabase reopened = new LocalDatabase(context());
    assertEquals("saved record", reopened.read("instrumentation-test"));
    assertEquals(1, reopened.pending("instrumentation-test").length());
    reopened.removePending(id);
    reopened.close();
  }

  @Test
  public void tokenIsEncryptedAndCanBeRecovered() throws Exception {
    SessionStore store = new SessionStore(context());
    String mode = store.mode(), url = store.endpoint();
    store.set("mode", "simulation");
    String simulation = store.endpoint();
    store.set("url_simulation", "http://127.0.0.1:9999");
    try {
      String token = java.util.UUID.randomUUID().toString();
      store.token(token);
      assertEquals(token, store.token());
      assertFalse(store.get("token_" + store.prefix(), "").contains(token));
      store.clearToken();
      assertEquals("", store.token());
    } finally {
      store.set("url_simulation", simulation);
      store.set("mode", mode);
      store.set("url_" + mode, url);
    }
  }

  @Test
  public void pdfExportIsValidAndPaginates() throws Exception {
    File file = new File(context().getCacheDir(), "qa-report.pdf");
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < 250; i++) text.append("Recorded batch line ").append(i).append('\n');
    FileExports.write(context(), Uri.fromFile(file), text.toString(), "pdf");
    assertTrue(file.length() > 1000);
    try (ParcelFileDescriptor descriptor =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        PdfRenderer renderer = new PdfRenderer(descriptor)) {
      assertTrue(renderer.getPageCount() > 1);
    }
  }

  @Test
  public void csvQuotesAndNeutralizesFormulaFields() {
    assertEquals("\"'\u003d2+2\"", FileExports.csv("=2+2"));
    assertEquals("\"a,\"\"b\"\"\"", FileExports.csv("a,\"b\""));
  }

  @Test
  public void localNotificationCanBePosted() {
    ReminderScheduler.test(context());
    android.app.NotificationManager manager =
        (android.app.NotificationManager) context().getSystemService(Context.NOTIFICATION_SERVICE);
    boolean found = false;
    for (android.service.notification.StatusBarNotification notification :
        manager.getActiveNotifications()) if (notification.getId() == 9001) found = true;
    assertTrue(found);
  }
}
