package com.example.chatgptnova;

import static org.junit.Assert.*;

import android.content.*;
import android.database.Cursor;
import android.os.SystemClock;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.chatgptnova.archive.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.*;

/** Real SQLite and native Reader on entirely fictional independently stored reports. */
public final class ArchiveResearchTest {
  Context context;

  @Before
  public void before() {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ArchiveStore.deleteArchive(context);
  }

  @After
  public void after() {
    ArchiveStore.deleteArchive(context);
  }

  File input(int mode) throws Exception {
    File f = File.createTempFile("fictional-research-", ".zip", context.getCacheDir());
    Files.write(f.toPath(), ArchiveResearchFixtures.zip(mode));
    return f;
  }

  ArchiveStore.Stats importFile(ArchiveStore s, File f) throws Exception {
    return s.importFile(
        f, true, "fictional.zip", UUID.randomUUID().toString(), new ArchiveImporter.Control());
  }

  long row(ArchiveStore s) {
    return s.list("", false, 10).get(0).id;
  }

  long count(ArchiveStore s) throws Exception {
    try (Cursor q =
        s.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM research_reports", null)) {
      q.moveToFirst();
      return q.getLong(0);
    }
  }

  @Test
  public void exactReportSurvivesDuplicateZipRemovalAndReopen() throws Exception {
    File f = input(0);
    long id;
    try (ArchiveStore s = new ArchiveStore(context)) {
      assertEquals(1, importFile(s, f).newReports);
      id = row(s);
      assertEquals(1, count(s));
      ArchiveModel.Conversation c = s.load(id);
      assertEquals(1, c.reports.size());
      assertTrue(c.reports.get(0).node().text().endsWith("RESEARCH-LAST"));
      ArchiveStore.Stats duplicate = importFile(s, f);
      assertEquals(0, duplicate.newReports);
      assertEquals(1, duplicate.skippedReports);
      assertEquals(1, count(s));
    }
    assertTrue(f.delete());
    try (ArchiveStore s = new ArchiveStore(context)) {
      ArchiveModel.Conversation c = s.load(id);
      assertEquals(1, c.reports.size());
      for (boolean all : Arrays.asList(false, true)) {
        String md = ArchiveRenderer.markdown(c, ArchiveTree.select(c, all), s.assets(id));
        assertTrue(md.contains(c.reports.get(0).node().text()));
        assertFalse(md.contains("RESEARCH-HIDDEN-ACTIVITY"));
        for (ArchiveRenderer.AssetMode m : ArchiveRenderer.AssetMode.values()) {
          String html = ArchiveRenderer.html(c, ArchiveTree.select(c, all), s.assets(id), m);
          assertTrue(html.contains("RESEARCH-LAST"));
          assertFalse(html.contains("RESEARCH-HIDDEN-ACTIVITY"));
        }
      }
    }
  }

  @Test
  public void schemaTwoUpgradePreservesMessagesAndReimportAddsReport() throws Exception {
    File f = input(0);
    try {
      try (ArchiveStore s = new ArchiveStore(context)) {
        importFile(s, f);
        s.getWritableDatabase().execSQL("DROP TABLE research_reports");
        s.getWritableDatabase().setVersion(2);
      }
      try (ArchiveStore s = new ArchiveStore(context)) {
        assertEquals(3, s.getReadableDatabase().getVersion());
        assertEquals(1, s.list("", false, 10).size());
        assertTrue(s.load(row(s)).reports.isEmpty());
        assertEquals(1, importFile(s, f).newReports);
        assertEquals(1, count(s));
      }
    } finally {
      f.delete();
    }
  }

  @Test
  public void damagedReimportPreservesBodyAndLimitOrCancelRollsBack() throws Exception {
    File good = input(0), bad = input(1), limit = input(2);
    try (ArchiveStore s = new ArchiveStore(context)) {
      importFile(s, good);
      long id = row(s);
      String raw = s.load(id).reports.get(0).message;
      ArchiveStore.Stats damage = importFile(s, bad);
      assertEquals(1, damage.unavailableReports);
      assertEquals(1, damage.skippedReports);
      assertEquals(raw, s.load(id).reports.get(0).message);
      try {
        importFile(s, limit);
        fail();
      } catch (ArchiveError e) {
        assertEquals("A05_ARCHIVE_TOO_LARGE", e.code);
      }
      assertEquals(1, count(s));
      assertEquals("Fictional research chat", s.load(id).title);
      assertEquals(raw, s.load(id).reports.get(0).message);
      ArchiveImporter.Control cancel = new ArchiveImporter.Control();
      cancel.cancelled.set(true);
      try {
        s.importFile(good, true, "fictional.zip", UUID.randomUUID().toString(), cancel);
        fail();
      } catch (ArchiveError e) {
        assertEquals("A07_IMPORT_CANCELLED", e.code);
      }
      assertEquals(1, count(s));
      assertEquals(raw, s.load(id).reports.get(0).message);
    } finally {
      good.delete();
      bad.delete();
      limit.delete();
    }
  }

  static Object field(Object o, String key) throws Exception {
    java.lang.reflect.Field f = o.getClass().getDeclaredField(key);
    f.setAccessible(true);
    return f.get(o);
  }

  @Test
  public void nativeReportsEntryAndRecreationUseOfflineBody() throws Exception {
    File f = input(0);
    long id;
    try (ArchiveStore s = new ArchiveStore(context)) {
      importFile(s, f);
      id = row(s);
    }
    f.delete();
    Intent intent =
        new Intent(context, ArchiveReaderActivity.class)
            .putExtra("row", id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try (ActivityScenario<ArchiveReaderActivity> scenario = ActivityScenario.launch(intent)) {
      await(scenario, false);
      scenario.onActivity(
          a -> {
            try {
              Button b = (Button) field(a, "reports");
              assertEquals("研究报告 (1)", b.getText().toString());
              assertTrue(b.performClick());
            } catch (Exception e) {
              throw new AssertionError(e);
            }
          });
      await(scenario, true);
      scenario.recreate();
      await(scenario, true);
      scenario.onActivity(
          a -> {
            try {
              SnapshotWebView v = (SnapshotWebView) field(a, "reader");
              assertFalse(v.web.getSettings().getJavaScriptEnabled());
              assertTrue(v.web.getSettings().getBlockNetworkLoads());
              String html = (String) field(a, "html");
              assertTrue(html.contains("RESEARCH-FIRST"));
              assertTrue(html.contains("RESEARCH-LAST"));
              assertFalse(html.contains("ARCHIVE-FIRST"));
              assertFalse(html.contains("RESEARCH-HIDDEN-ACTIVITY"));
              assertFalse(((Button) field(a, "branch")).isEnabled());
            } catch (Exception e) {
              throw new AssertionError(e);
            }
          });
    }
  }

  static void await(ActivityScenario<ArchiveReaderActivity> scenario, boolean report)
      throws Exception {
    long until = SystemClock.uptimeMillis() + 30000;
    AtomicReference<Boolean> ready = new AtomicReference<>(false);
    do {
      scenario.onActivity(
          a -> {
            try {
              String t = ((TextView) field(a, "status")).getText().toString();
              ready.set(
                  t.contains(report ? "research-reports" : "current-branch")
                      && ((Button) field(a, "export")).isEnabled());
            } catch (Exception e) {
              throw new AssertionError(e);
            }
          });
      if (ready.get()) return;
      SystemClock.sleep(100);
    } while (SystemClock.uptimeMillis() < until);
    fail("Research Reader did not finish");
  }
}
