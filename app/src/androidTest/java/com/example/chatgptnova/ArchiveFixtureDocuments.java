package com.example.chatgptnova;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Test APK only. Slow pipe exercises cancellation and configuration changes during SAF IO. */
public final class ArchiveFixtureDocuments extends ContentProvider {
  @Override
  public boolean onCreate() {
    return true;
  }

  private File file(Uri uri) {
    String name = uri.getLastPathSegment();
    if (name == null || !name.matches("[a-zA-Z0-9_.-]+")) throw new IllegalArgumentException();
    return new File(getContext().getCacheDir(), "archive-fixture-" + name);
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
    File file = file(uri);
    if (mode.equals("r") && uri.getLastPathSegment().equals("slow.zip"))
      try {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        new Thread(
                () -> {
                  try (InputStream in = new FileInputStream(file);
                      OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                    byte[] b = new byte[256];
                    int n;
                    while ((n = in.read(b)) != -1) {
                      out.write(b, 0, n);
                      Thread.sleep(15);
                    }
                  } catch (Exception ignored) {
                  }
                },
                "SyntheticSlowDocument")
            .start();
        return pipe[0];
      } catch (IOException e) {
        throw new FileNotFoundException();
      }
    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode));
  }

  @Override
  public String getType(Uri uri) {
    return uri.getLastPathSegment().endsWith(".zip") ? "application/zip" : "application/json";
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String sel, String[] args, String order) {
    String[] cols =
        projection == null
            ? new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
            : projection;
    MatrixCursor q = new MatrixCursor(cols);
    Object[] row = new Object[cols.length];
    for (int i = 0; i < cols.length; i++) {
      if (cols[i].equals(OpenableColumns.DISPLAY_NAME)) row[i] = uri.getLastPathSegment();
      if (cols[i].equals(OpenableColumns.SIZE)) {
        String name = uri.getLastPathSegment();
        if (name.equals("unknown-size.zip")) row[i] = null;
        else if (name.equals("metadata300.zip")) row[i] = 300L * 1024 * 1024;
        else if (name.equals("oversize.zip")) row[i] = 513L * 1024 * 1024;
        else row[i] = file(uri).length();
      }
    }
    q.addRow(row);
    return q;
  }

  @Override
  public Uri insert(Uri uri, ContentValues v) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int update(Uri uri, ContentValues v, String s, String[] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int delete(Uri uri, String s, String[] a) {
    return file(uri).delete() ? 1 : 0;
  }
}
