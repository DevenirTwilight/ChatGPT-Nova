package com.example.chatgptnova;

import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/** Test APK only: bounded thread/phase evidence for stalled synthetic tests, no source data. */
final class ArchiveThreadEvidence extends TestWatcher {
  private Thread watchdog;
  volatile java.util.function.Supplier<String> phase = () -> "initializing";

  @Override
  protected void starting(Description description) {
    watchdog =
        new Thread(
            () -> {
              try {
                Thread.sleep(90_000);
                capture(description.getMethodName(), phase.get());
              } catch (InterruptedException expected) {
                Thread.currentThread().interrupt();
              }
            },
            "SyntheticArchiveWatchdog");
    watchdog.setDaemon(true);
    watchdog.start();
  }

  @Override
  protected void finished(Description description) {
    if (watchdog != null) watchdog.interrupt();
  }

  static void capture(String method) {
    capture(method, "condition-timeout");
  }

  static synchronized void capture(String method, String phase) {
    try {
      File root =
          InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null);
      if (root == null) return;
      File file = new File(root, "archive-test-threads.txt");
      if (file.length() > 2 * 1024 * 1024) return;
      try (PrintWriter out =
          new PrintWriter(
              new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
        out.println("synthetic-thread-evidence method=" + method + " phase=" + phase);
        int threads = 0;
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
          if (++threads > 128) break;
          Thread thread = entry.getKey();
          // Thread names may be customized by other libraries; only stable numeric identity/state.
          out.println("thread=" + thread.getId() + " state=" + thread.getState());
          int frames = 0;
          for (StackTraceElement frame : entry.getValue()) {
            if (++frames > 48) break;
            out.println(frame.toString());
          }
        }
      }
    } catch (Exception ignored) {
      // This diagnostic must never change a test outcome or print exception/source contents.
    }
  }
}
