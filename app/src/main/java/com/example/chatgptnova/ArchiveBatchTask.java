package com.example.chatgptnova;

import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import com.example.chatgptnova.archive.*;
import java.io.*;
import java.util.*;

/** Retained across rotation; stages and verifies locally before writing the chosen destination. */
final class ArchiveBatchTask {
  final Context context;
  final Uri destination;
  final String query;
  final boolean earliest, all;
  final ArchiveImporter.Control control = new ArchiveImporter.Control();
  volatile boolean done;
  volatile int total, processed, succeeded, failed;
  volatile String stage = "正在准备", error;
  private ArchiveActivity listener;
  private Thread worker;

  ArchiveBatchTask(Context context, Uri destination, String query, boolean earliest, boolean all) {
    this.context = context.getApplicationContext();
    this.destination = destination;
    this.query = query;
    this.earliest = earliest;
    this.all = all;
  }

  synchronized void attach(ArchiveActivity a) {
    listener = a;
    notifyUi();
  }

  synchronized void detach(ArchiveActivity a) {
    if (listener == a) listener = null;
  }

  synchronized void notifyUi() {
    ArchiveActivity a = listener;
    if (a != null) a.runOnUiThread(() -> a.updateBatch(this));
  }

  void start() {
    worker = new Thread(this::run, "NovaArchiveBatch");
    worker.start();
  }

  void cancel() {
    control.cancelled.set(true);
    if (worker != null) worker.interrupt();
  }

  private String hash(File file) throws Exception {
    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
    try (InputStream in = new FileInputStream(file)) {
      byte[] b = new byte[32768];
      int n;
      while ((n = in.read(b)) != -1) {
        control.check();
        digest.update(b, 0, n);
      }
    }
    return java.util.Base64.getEncoder().encodeToString(digest.digest());
  }

  private void run() {
    File temp = null;
    try {
      File dir = new File(context.getCacheDir(), "nova-archive-batch");
      if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException();
      temp = File.createTempFile("batch-", ".zip", dir);
      try (ArchiveStore store = new ArchiveStore(context)) {
        synchronized (ArchiveStore.LOCK) {
          List<ArchiveStore.Row> rows =
              store.list(query, earliest, ArchiveBatch.MAX_CONVERSATIONS + 1);
          total = rows.size();
          if (total == 0) throw new ArchiveError("A03_NO_CONVERSATIONS_DATA");
          if (total > ArchiveBatch.MAX_CONVERSATIONS)
            throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
          try (ArchiveBatch batch = new ArchiveBatch(new FileOutputStream(temp), control)) {
            for (ArchiveStore.Row row : rows) {
              control.check();
              ArchiveModel.Conversation c;
              Map<String, ArchiveAsset> assets;
              try {
                c = store.load(row.id);
                assets = store.assets(row.id);
              } catch (ArchiveError e) {
                if (e.code.equals("A07_IMPORT_CANCELLED")) throw e;
                batch.failure(row.title, e.code);
                processed++;
                failed = batch.failed;
                notifyUi();
                continue;
              }
              batch.add(c, all, assets);
              processed++;
              succeeded = batch.succeeded;
              failed = batch.failed;
              stage = "逐会话导出";
              notifyUi();
            }
            batch.finish();
          }
        }
      }
      if (succeeded == 0) throw new ArchiveError("A11_EXPORT_FAILED");
      stage = "核验 ZIP 内全部文件";
      notifyUi();
      ArchiveBatch.verify(temp, control);
      control.check();
      String stagedHash = hash(temp);
      stage = "保存已核验的 ZIP";
      notifyUi();
      try (InputStream in = new FileInputStream(temp);
          OutputStream out = context.getContentResolver().openOutputStream(destination, "wt")) {
        if (out == null) throw new IOException();
        byte[] b = new byte[32768];
        int n;
        while ((n = in.read(b)) != -1) {
          control.check();
          out.write(b, 0, n);
        }
      }
      // Verify the saved bytes as well, using the same private staging file.
      try (InputStream in = context.getContentResolver().openInputStream(destination);
          OutputStream out = new FileOutputStream(temp)) {
        if (in == null) throw new IOException();
        byte[] b = new byte[32768];
        int n;
        long bytes = 0;
        while ((n = in.read(b)) != -1) {
          control.check();
          bytes += n;
          if (bytes > 512L * 1024 * 1024) throw new IOException();
          out.write(b, 0, n);
        }
      }
      ArchiveBatch.verify(temp, control);
      if (!stagedHash.equals(hash(temp))) throw new IOException();
      stage = "完成";
    } catch (Exception e) {
      error =
          control.cancelled.get()
              ? "已取消批量导出"
              : e instanceof ArchiveError ? ((ArchiveError) e).code : "A11_EXPORT_FAILED";
      try {
        DocumentsContract.deleteDocument(context.getContentResolver(), destination);
      } catch (Exception ignored) {
        error += " · 请删除未完成的目标 ZIP";
      }
    } finally {
      if (temp != null) temp.delete();
      done = true;
      notifyUi();
    }
  }
}
