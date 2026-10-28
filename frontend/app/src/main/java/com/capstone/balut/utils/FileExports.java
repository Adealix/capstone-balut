package com.capstone.balut.utils;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class FileExports {
  public static String csv(String value) {
    // Prevent a supplier/name field from becoming a spreadsheet formula on export.
    if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) value = "'" + value;
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  public static void write(Context context, Uri uri, String text, String kind) throws Exception {
    try (OutputStream stream = context.getContentResolver().openOutputStream(uri)) {
      if (stream == null) throw new IOException();
      if (!kind.equals("pdf")) {
        stream.write(text.getBytes(StandardCharsets.UTF_8));
        return;
      }
      PdfDocument document = new PdfDocument();
      try {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(11);
        paint.setColor(Color.rgb(23, 45, 40));
        int pageNumber = 1, y = 45;
        PdfDocument.Page page =
            document.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create());
        for (String line : text.split("\n")) {
          int start = 0;
          do {
            if (y > 790) {
              document.finishPage(page);
              page =
                  document.startPage(
                      new PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create());
              y = 45;
            }
            int length =
                line.isEmpty()
                    ? 0
                    : Math.max(1, paint.breakText(line.substring(start), true, 525, null));
            page.getCanvas().drawText(line.substring(start, start + length), 35, y, paint);
            y += 17;
            start += Math.max(1, length);
          } while (start < line.length());
        }
        document.finishPage(page);
        document.writeTo(stream);
      } finally {
        document.close();
      }
    }
  }

  public static String read(Context context, Uri uri) throws Exception {
    try (InputStream input = context.getContentResolver().openInputStream(uri);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      if (input == null) throw new IOException();
      byte[] buffer = new byte[4096];
      int length;
      while ((length = input.read(buffer)) != -1) {
        out.write(buffer, 0, length);
        if (out.size() > 8_000_000) throw new IOException("File too large");
      }
      return out.toString("UTF-8");
    }
  }

  private FileExports() {}
}
