package com.example.chatgptnova;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.SystemClock;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.chatgptnova.archive.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.junit.*;

public final class ArchiveBatchTest {
  Context context;
  Instrumentation instrument;
  static final Uri OUT =
      Uri.parse("content://com.example.chatgptnova.test.archive.documents/batch.zip");

  @Before
  public void before() {
    instrument = InstrumentationRegistry.getInstrumentation();
    context = instrument.getTargetContext();
    ArchiveStore.deleteArchive(context);
  }

  @After
  public void after() {
    ArchiveStore.deleteArchive(context);
  }

  File seed(boolean many) throws Exception {
    File f =
        File.createTempFile("fictional-batch-", many ? ".json" : ".zip", context.getCacheDir());
    if (many) {
      List<Map<String, Object>> list = new ArrayList<>();
      for (int i = 0; i < 201; i++) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("conversation_id", "fictional-" + i);
        c.put("title", i < 2 ? "duplicate" : "other");
        c.put("current_node", "a");
        c.put(
            "mapping",
            ArchiveModel.JSON.fromJson(
                "{\"a\":{\"parent\":null,\"message\":{\"author\":{\"role\":\"assistant\"},\"content\":{\"content_type\":\"text\",\"parts\":[\"BATCH-FIRST"
                    + " BATCH-LAST\"]}}}}",
                Map.class));
        list.add(c);
      }
      Files.write(f.toPath(), ArchiveModel.JSON.toJson(list).getBytes(StandardCharsets.UTF_8));
    } else Files.write(f.toPath(), ArchiveResearchFixtures.zip(0));
    try (ArchiveStore s = new ArchiveStore(context)) {
      s.importFile(
          f, !many, "fictional", UUID.randomUUID().toString(), new ArchiveImporter.Control());
    }
    return f;
  }

  void await(ArchiveBatchTask task) {
    long end = SystemClock.uptimeMillis() + 60000;
    while (!task.done && SystemClock.uptimeMillis() < end) SystemClock.sleep(20);
    assertTrue(task.done);
    assertNull(task.error);
  }

  File saved() throws Exception {
    File f = File.createTempFile("batch-readback-", ".zip", context.getCacheDir());
    try (InputStream in = context.getContentResolver().openInputStream(OUT);
        OutputStream out = new FileOutputStream(f)) {
      byte[] b = new byte[32768];
      int n;
      while ((n = in.read(b)) != -1) out.write(b, 0, n);
    }
    return f;
  }

  @Test
  public void allPagesAndDuplicateTitlesExportSeparatelyWithFiltering() throws Exception {
    File input = seed(true);
    try {
      ArchiveBatchTask task = new ArchiveBatchTask(context, OUT, "", false, false);
      task.start();
      await(task);
      assertEquals(201, task.succeeded);
      assertEquals(0, task.failed);
      File f = saved();
      try {
        ArchiveBatch.verify(f, new ArchiveImporter.Control());
        try (ZipFile z = new ZipFile(f)) {
          assertEquals(403, z.size());
        }
      } finally {
        f.delete();
      }
      task = new ArchiveBatchTask(context, OUT, "duplicate", false, true);
      task.start();
      await(task);
      assertEquals(2, task.succeeded);
      f = saved();
      try (ZipFile z = new ZipFile(f)) {
        assertEquals(5, z.size());
      } finally {
        f.delete();
      }
    } finally {
      input.delete();
    }
  }

  @Test
  public void cancelledTaskDoesNotClaimSuccessfulSavedOutput() throws Exception {
    File input = seed(false);
    try {
      ArchiveBatchTask task = new ArchiveBatchTask(context, OUT, "", false, false);
      task.cancel();
      task.start();
      long end = SystemClock.uptimeMillis() + 10000;
      while (!task.done && SystemClock.uptimeMillis() < end) SystemClock.sleep(20);
      assertTrue(task.done);
      assertNotNull(task.error);
      assertTrue(task.error.startsWith("已取消"));
      assertEquals(0, task.succeeded);
      try (ArchiveStore store = new ArchiveStore(context)) {
        assertEquals(1, store.list("", false, 10).size());
      }
    } finally {
      input.delete();
    }
  }

  @Test
  public void nativeBatchEntryUsesOneSaveAndPreservesReportTimeline() throws Exception {
    File input = seed(false);
    Instrumentation.ActivityMonitor monitor =
        new Instrumentation.ActivityMonitor() {
          @Override
          public Instrumentation.ActivityResult onStartActivity(Intent i) {
            if (!Intent.ACTION_CREATE_DOCUMENT.equals(i.getAction())) return null;
            assertEquals("application/zip", i.getType());
            return new Instrumentation.ActivityResult(
                Activity.RESULT_OK, new Intent().setData(OUT));
          }
        };
    instrument.addMonitor(monitor);
    try (ActivityScenario<ArchiveActivity> scenario =
        ActivityScenario.launch(ArchiveActivity.class)) {
      scenario.onActivity(
          a -> {
            try {
              java.lang.reflect.Field f = ArchiveActivity.class.getDeclaredField("batchExport");
              f.setAccessible(true);
              ((Button) f.get(a)).performClick();

            } catch (Exception e) {
              throw new AssertionError(e);
            }
          });
      androidx.test.uiautomator.UiObject confirm =
          androidx.test.uiautomator.UiDevice.getInstance(instrument)
              .findObject(new androidx.test.uiautomator.UiSelector().text("当前分支"));
      assertTrue(confirm.waitForExists(5000));
      confirm.click();
      final ArchiveBatchTask[] task = {null};
      long end = SystemClock.uptimeMillis() + 10000;
      while (task[0] == null && SystemClock.uptimeMillis() < end) {
        scenario.onActivity(
            a -> {
              try {
                java.lang.reflect.Field f = ArchiveActivity.class.getDeclaredField("batchTask");
                f.setAccessible(true);
                task[0] = (ArchiveBatchTask) f.get(a);
              } catch (Exception e) {
                throw new AssertionError(e);
              }
            });
        SystemClock.sleep(20);
      }
      assertNotNull(task[0]);
      scenario.recreate();
      await(task[0]);
      File f = saved();
      try (ZipFile z = new ZipFile(f)) {
        String md = null;
        for (ZipEntry e : Collections.list(z.entries()))
          if (e.getName().endsWith(".md")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream in = z.getInputStream(e)) {
              byte[] b = new byte[8192];
              int n;
              while ((n = in.read(b)) != -1) bytes.write(b, 0, n);
            }
            md = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
          }
        assertNotNull(md);
        assertTrue(md.contains("RESEARCH-FIRST"));
        assertTrue(md.indexOf("RESEARCH-LAST") < md.indexOf("AFTER-RESEARCH-REPLY"));
        assertTrue(md.contains("按时间恢复位置"));
      } finally {
        f.delete();
      }
    } finally {
      instrument.removeMonitor(monitor);
      input.delete();
    }
  }
}
