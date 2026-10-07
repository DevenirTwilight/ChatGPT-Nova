package com.example.chatgptnova;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import com.example.chatgptnova.archive.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.*;

/** Compatible synthetic export + real Android adapters. Never real OpenAI history proof. */
public final class ArchiveTest extends FixtureActivity {
  private ActivityScenario<ArchiveActivity> archiveScenario;
  private ActivityScenario<ArchiveReaderActivity> readerScenario;
  private ArchiveActivity archive;
  private ArchiveReaderActivity reader;
  private Instrumentation.ActivityMonitor monitor;
  private static final Uri
      INPUT = Uri.parse("content://com.example.chatgptnova.test.archive.documents/input.zip"),
      SLOW = Uri.parse("content://com.example.chatgptnova.test.archive.documents/slow.zip");

  @Before
  public void before() throws Exception {
    start();
    instrument.getTargetContext().deleteDatabase(ArchiveStore.DATABASE);
    archiveScenario =
        ActivityScenario.launch(new Intent(instrument.getTargetContext(), ArchiveActivity.class));
    archiveScenario.onActivity(
        a -> {
          archive = a;
          prepareArchiveWindow(a);
        });
    android.accessibilityservice.AccessibilityServiceInfo info =
        instrument.getUiAutomation().getServiceInfo();
    info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
    instrument.getUiAutomation().setServiceInfo(info);
  }

  private void prepareArchiveWindow(Activity a) {
    a.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    a.setShowWhenLocked(true);
    a.setTurnScreenOn(true);
  }

  @After
  public void after() {
    if (monitor != null) instrument.removeMonitor(monitor);
    if (readerScenario != null) readerScenario.close();
    if (archiveScenario != null)
      archiveScenario.onActivity(
          a ->
              a.setRequestedOrientation(
                  android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED));
    if (archiveScenario != null) archiveScenario.close();
    if (scenario != null) scenario.close();
    ArchiveActivity.Task t = archive == null ? null : (ArchiveActivity.Task) field(archive, "task");
    if (t != null) waitFor("import worker cleanup", () -> t.done);
  }

  static Object field(Object o, String n) {
    try {
      java.lang.reflect.Field f = o.getClass().getDeclaredField(n);
      f.setAccessible(true);
      return f.get(o);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  static void waitFor(String n, Condition c) {
    long end = SystemClock.uptimeMillis() + 60000;
    do {
      if (c.ready()) return;
      SystemClock.sleep(100);
    } while (SystemClock.uptimeMillis() < end);
    fail("Timed out: " + n);
  }

  private AccessibilityNodeInfo find(AccessibilityNodeInfo n, String label) {
    if (n == null) return null;
    if (n.isVisibleToUser()
        && ((n.getText() != null && label.equalsIgnoreCase(n.getText().toString()))
            || (n.getContentDescription() != null
                && label.equalsIgnoreCase(n.getContentDescription().toString())))) return n;
    for (int i = 0; i < n.getChildCount(); i++) {
      AccessibilityNodeInfo f = find(n.getChild(i), label);
      if (f != null) return f;
    }
    return null;
  }

  private void click(String label) {
    waitFor(
        label,
        () -> {
          AccessibilityNodeInfo n =
              find(instrument.getUiAutomation().getRootInActiveWindow(), label);
          while (n != null && !n.isClickable()) n = n.getParent();
          return n != null && n.isEnabled() && n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        });
    instrument.waitForIdleSync();
  }

  private String status() {
    AtomicReference<String> s = new AtomicReference<>();
    main(() -> s.set(((TextView) field(archive, "status")).getText().toString()));
    return s.get();
  }

  private int visibleRows() {
    AtomicInteger n = new AtomicInteger();
    main(
        () -> {
          ListView list = (ListView) field(archive, "list");
          n.set(list.getAdapter() == null ? 0 : list.getAdapter().getCount());
        });
    return n.get();
  }

  private File input(byte[] bytes) throws Exception {
    File f =
        File.createTempFile("archive-test-", ".json", instrument.getTargetContext().getCacheDir());
    java.nio.file.Files.write(f.toPath(), bytes);
    return f;
  }

  private ArchiveStore.Stats store(byte[] bytes) throws Exception {
    File f = input(bytes);
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      return db.importFile(
          f, false, "synthetic.json", UUID.randomUUID().toString(), new ArchiveImporter.Control());
    } finally {
      f.delete();
    }
  }

  private long firstRow() {
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      return db.list("", false, 1).get(0).id;
    }
  }

  private void write(Uri uri, byte[] b) throws Exception {
    try (OutputStream out =
        instrument.getTargetContext().getContentResolver().openOutputStream(uri, "wt")) {
      out.write(b);
    }
  }

  private byte[] readFixtureDocument(Uri uri) throws Exception {
    try (InputStream in = instrument.getTargetContext().getContentResolver().openInputStream(uri)) {
      return in.readAllBytes();
    }
  }

  private void intercept(String action, Uri output, boolean cancelled) {
    if (monitor != null) instrument.removeMonitor(monitor);
    monitor =
        new Instrumentation.ActivityMonitor() {
          @Override
          public Instrumentation.ActivityResult onStartActivity(Intent intent) {
            if (!action.equals(intent.getAction())) return null;
            assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE));
            return new Instrumentation.ActivityResult(
                cancelled ? Activity.RESULT_CANCELED : Activity.RESULT_OK,
                new Intent().setData(output));
          }
        };
    instrument.addMonitor(monitor);
  }

  private void openReader() {
    readerScenario =
        ActivityScenario.launch(
            new Intent(instrument.getTargetContext(), ArchiveReaderActivity.class)
                .putExtra("row", firstRow()));
    readerScenario.onActivity(
        a -> {
          reader = a;
          prepareArchiveWindow(a);
        });
    waitFor(
        "reader ready",
        () -> {
          AtomicBoolean ready = new AtomicBoolean();
          main(() -> ready.set(((Button) field(reader, "export")).isEnabled()));
          return ready.get();
        });
  }

  @Test
  public void safZipImportListAndDiagnostics() throws Exception {
    write(INPUT, ArchiveFixtures.zip(false));
    intercept(Intent.ACTION_OPEN_DOCUMENT, INPUT, false);
    click("导入 ChatGPT 数据");
    waitFor("SAF import complete", () -> status().startsWith("导入完成"));
    waitFor("list", () -> visibleRows() == 1);
    assertEquals(1, visibleRows());
    String diagnostic = (String) field(archive, "diagnostic");
    assertTrue(diagnostic.contains("newConversations=1"));
    for (String value : List.of("synthetic-0", "ARCHIVE-FIRST", "input.zip", "café"))
      assertFalse(diagnostic.contains(value));
    File[] leftovers = new File(archive.getCacheDir(), "nova-archive-import").listFiles();
    assertTrue(leftovers == null || leftovers.length == 0);
  }

  @Test
  public void safJsonAndMalformedFilesHaveFixedErrors() throws Exception {
    Uri json = Uri.parse("content://com.example.chatgptnova.test.archive.documents/input.json");
    write(json, ArchiveFixtures.json(1, false));
    intercept(Intent.ACTION_OPEN_DOCUMENT, json, false);
    click("导入 ChatGPT 数据");
    waitFor("JSON imported", () -> status().startsWith("导入完成"));
    write(json, "[{SECRET-DO-NOT-LOG]".getBytes(StandardCharsets.UTF_8));
    click("导入 ChatGPT 数据");
    waitFor("JSON fixed error", () -> status().startsWith("A04_JSON_PARSE_FAILED"));
    assertFalse(status().contains("SECRET"));
    assertFalse(((String) field(archive, "diagnostic")).contains("SECRET"));
    write(INPUT, "invalid ZIP".getBytes(StandardCharsets.UTF_8));
    intercept(Intent.ACTION_OPEN_DOCUMENT, INPUT, false);
    click("导入 ChatGPT 数据");
    waitFor("ZIP fixed error", () -> status().startsWith("A02_INVALID_ZIP"));
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      assertEquals(1, db.list("", false, 10).size());
    }
  }

  @Test
  public void cancellationDuringDatabaseTransactionRollsBack() throws Exception {
    store(ArchiveFixtures.json(1, false));
    File f = input(ArchiveFixtures.json(1000, false));
    ArchiveImporter.Control control = new ArchiveImporter.Control();
    AtomicReference<ArchiveError> failure = new AtomicReference<>();
    String source = UUID.randomUUID().toString();
    Thread worker =
        new Thread(
            () -> {
              try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
                db.importFile(f, false, "synthetic.json", source, control);
              } catch (ArchiveError e) {
                failure.set(e);
              }
            });
    worker.start();
    waitFor(
        "transaction started",
        () -> {
          try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext());
              android.database.Cursor q =
                  db.getReadableDatabase()
                      .rawQuery(
                          "SELECT status FROM import_sources WHERE id=?", new String[] {source})) {
            return q.moveToFirst() && q.getString(0).equals("running");
          }
        });
    control.cancelled.set(true);
    worker.join(60000);
    assertFalse(worker.isAlive());
    assertNotNull(failure.get());
    assertEquals("A07_IMPORT_CANCELLED", failure.get().code);
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      assertEquals(1, db.list("", false, 1001).size());
    }
    f.delete();
  }

  @Test
  public void cancelledSafPickerDoesNotReadOrChangeDatabase() {
    intercept(Intent.ACTION_OPEN_DOCUMENT, INPUT, true);
    click("导入 ChatGPT 数据");
    waitFor("picker cancelled", () -> status().contains("未选择文件"));
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      assertTrue(db.list("", false, 10).isEmpty());
    }
  }

  @Test
  public void identityUpsertPersistenceAndMissingId() throws Exception {
    ArchiveStore.Stats first = store(ArchiveFixtures.json(1, false));
    assertEquals(1, first.newConversations);
    assertEquals(3, first.newMessages);
    ArchiveStore.Stats again = store(ArchiveFixtures.json(1, false));
    assertEquals(0, again.newConversations);
    assertEquals(0, again.updatedConversations);
    assertEquals(4, again.skipped);
    Map<String, Object> noid = ArchiveFixtures.conversation("", "No identity", 100, false);
    byte[] bytes = ArchiveModel.JSON.toJson(List.of(noid)).getBytes(StandardCharsets.UTF_8);
    store(bytes);
    store(bytes);
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      assertEquals(3, db.list("", false, 10).size());
      ArchiveModel.Conversation c = db.load(firstRow());
      assertEquals(4, c.nodes.size());
      assertEquals(2, ArchiveTree.select(c, false).messages.size());
      assertTrue(c.header.contains("metadata"));
    }
  }

  @Test
  public void malformedInputRollsBackEarlierConversations() throws Exception {
    store(ArchiveFixtures.json(1, false));
    String valid = ArchiveModel.JSON.toJson(ArchiveFixtures.conversation("new", "new", 200, false));
    try {
      store(("[" + valid + ",{broken]").getBytes(StandardCharsets.UTF_8));
      fail();
    } catch (ArchiveError e) {
      assertEquals("A04_JSON_PARSE_FAILED", e.code);
    }
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      assertEquals(1, db.list("", false, 10).size());
    }
  }

  @Test
  public void databaseCancellationDoesNotCommit() throws Exception {
    File f = input(ArchiveFixtures.json(100, false));
    ArchiveImporter.Control control = new ArchiveImporter.Control();
    control.cancelled.set(true);
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      try {
        db.importFile(f, false, "synthetic.json", UUID.randomUUID().toString(), control);
        fail();
      } catch (ArchiveError e) {
        assertEquals("A07_IMPORT_CANCELLED", e.code);
      }
      assertTrue(db.list("", false, 10).isEmpty());
    } finally {
      f.delete();
    }
  }

  @Test
  public void thousandConversationsSearchAndSort() throws Exception {
    assertEquals(1000, store(ArchiveFixtures.json(1000, false)).newConversations);
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      assertEquals(200, db.list("", false, 200).size());
      assertEquals(1000, db.list("CAFÉ", false, 1001).size());
      assertEquals(0, db.list("%", false, 1001).size());
      assertTrue(db.list("", true, 1).get(0).title.endsWith(" 0"));
      assertTrue(db.list("", false, 1).get(0).title.endsWith(" 999"));
    }
    main(() -> archive.refresh());
    waitFor("pagination", () -> visibleRows() == 200);
    click("下一页");
    waitFor(
        "next page",
        () -> {
          AtomicBoolean expected = new AtomicBoolean();
          main(
              () -> {
                List<ArchiveStore.Row> rows = (List<ArchiveStore.Row>) field(archive, "rows");
                expected.set(rows.size() == 200 && rows.get(0).title.endsWith(" 799"));
              });
          return expected.get();
        });
    click("上一页");
    waitFor(
        "previous page",
        () -> {
          AtomicBoolean expected = new AtomicBoolean();
          main(
              () -> {
                List<ArchiveStore.Row> rows = (List<ArchiveStore.Row>) field(archive, "rows");
                expected.set(rows.size() == 200 && rows.get(0).title.endsWith(" 999"));
              });
          return expected.get();
        });
    main(() -> ((EditText) field(archive, "search")).setText("café 999"));
    waitFor("UI title search", () -> visibleRows() == 1);
  }

  @Test
  public void readerHtmlAndMarkdownSafUseArchiveData() throws Exception {
    store(ArchiveFixtures.json(1, false));
    openReader();
    assertNull(((SnapshotWebView) field(reader, "reader")).snapshot);
    intercept(Intent.ACTION_CREATE_DOCUMENT, OUTPUT, false);
    click("导出");
    click("HTML");
    waitFor("HTML written", () -> readerStatus().startsWith("已保存"));
    String html = new String(readFixtureDocument(OUTPUT), StandardCharsets.UTF_8);
    assertTrue(html.startsWith("<!doctype html>"));
    assertTrue(html.contains("ARCHIVE-FIRST"));
    assertFalse(html.contains("OTHER-BRANCH"));
    assertTrue(html.contains("<table>"));
    evidence("archive.html", html.getBytes(StandardCharsets.UTF_8));
    click("导出");
    click("Markdown");
    waitFor(
        "MD written",
        () -> {
          try {
            return new String(readFixtureDocument(OUTPUT), StandardCharsets.UTF_8)
                .startsWith("# Archive");
          } catch (Exception e) {
            return false;
          }
        });
    String md = new String(readFixtureDocument(OUTPUT), StandardCharsets.UTF_8);
    assertTrue(md.contains("CODE-TAIL"));
    assertTrue(md.contains("```java"));
    assertTrue(md.contains("current-branch"));
    evidence("archive.md", md.getBytes(StandardCharsets.UTF_8));
    click("查看全部分支");
    waitFor("all nodes ready", () -> readerStatus().contains("all-nodes"));
    assertTrue(((String) field(reader, "html")).contains("OTHER-BRANCH"));
  }

  private String readerStatus() {
    AtomicReference<String> s = new AtomicReference<>();
    main(() -> s.set(((TextView) field(reader, "status")).getText().toString()));
    return s.get();
  }

  @Test
  public void offlineReaderPolicyAndCookieUnchanged() throws Exception {
    main(
        () ->
            android.webkit.CookieManager.getInstance()
                .setCookie(PAGE, "nova_archive_fixture=retained; Path=/; Secure"));
    store(ArchiveFixtures.json(1, false));
    openReader();
    AtomicReference<Intent> link = new AtomicReference<>();
    monitor =
        new Instrumentation.ActivityMonitor() {
          @Override
          public Instrumentation.ActivityResult onStartActivity(Intent intent) {
            if (!Intent.ACTION_VIEW.equals(intent.getAction())) return null;
            link.set(intent);
            return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
          }
        };
    instrument.addMonitor(monitor);
    main(
        () -> {
          SnapshotWebView v = (SnapshotWebView) field(reader, "reader");
          assertFalse(v.web.getSettings().getJavaScriptEnabled());
          assertFalse(v.web.getSettings().getDomStorageEnabled());
          assertFalse(v.web.getSettings().getAllowFileAccess());
          assertFalse(v.web.getSettings().getAllowContentAccess());
          assertTrue(v.web.getSettings().getBlockNetworkLoads());
          v.web
              .getWebViewClient()
              .shouldOverrideUrlLoading(v.web, "https://example.org/nova-fixture");
        });
    assertNotNull(link.get());
    assertEquals("https://example.org/nova-fixture", link.get().getDataString());
    assertTrue(
        android.webkit.CookieManager.getInstance()
            .getCookie(PAGE)
            .contains("nova_archive_fixture=retained"));
  }

  @Test
  public void importRetainedAcrossActivityRecreation() throws Exception {
    write(SLOW, ArchiveFixtures.zip(true));
    main(() -> archive.startImport(SLOW));
    waitFor("task running", () -> field(archive, "task") != null);
    ArchiveActivity.Task before = (ArchiveActivity.Task) field(archive, "task");
    ArchiveActivity old = archive;
    main(
        () ->
            archive.setRequestedOrientation(
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
    waitFor(
        "rotation recreates Archive",
        () -> {
          archiveScenario.onActivity(a -> archive = a);
          return archive != old;
        });
    assertSame(before, field(archive, "task"));
    assertFalse(before.control.cancelled.get());
    waitFor("retained import complete", () -> before.done);
    assertNull(before.error);
    assertEquals(1, before.stats.newConversations);
    waitFor("retained list", () -> visibleRows() == 1);
  }

  @Test
  public void explicitCancelDuringSafCopy() throws Exception {
    write(SLOW, ArchiveFixtures.zip(true));
    main(() -> archive.startImport(SLOW));
    ArchiveActivity.Task t = (ArchiveActivity.Task) field(archive, "task");
    click("取消导入");
    waitFor("cancel done", () -> t.done);
    assertEquals("A07_IMPORT_CANCELLED", t.error.code);
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      assertTrue(db.list("", false, 10).isEmpty());
    }
  }

  @Test
  public void destroyingArchiveCancelsImport() throws Exception {
    write(SLOW, ArchiveFixtures.zip(true));
    main(() -> archive.startImport(SLOW));
    ArchiveActivity.Task t = (ArchiveActivity.Task) field(archive, "task");
    archiveScenario.close();
    archiveScenario = null;
    waitFor("destroy cleanup", () -> t.done);
    assertEquals("A07_IMPORT_CANCELLED", t.error.code);
  }

  @Test
  public void readerRecreationAndDestroyReleaseWebViews() throws Exception {
    store(ArchiveFixtures.json(1, false));
    openReader();
    SnapshotWebView old = (SnapshotWebView) field(reader, "reader");
    readerScenario.recreate();
    readerScenario.onActivity(a -> reader = a);
    assertNull(old.web.getParent());
    waitFor("recreated reader ready", () -> readerStatus().contains("current-branch"));
    SnapshotWebView recreated = (SnapshotWebView) field(reader, "reader");
    readerScenario.close();
    readerScenario = null;
    assertNull(recreated.web.getParent());
  }

  @Test
  public void deleteArchiveKeepsOnlineSession() throws Exception {
    store(ArchiveFixtures.json(1, false));
    main(
        () ->
            android.webkit.CookieManager.getInstance()
                .setCookie(PAGE, "nova_archive_delete=retained; Path=/; Secure"));
    click("删除本地档案");
    click("删除");
    waitFor("deleted", () -> status().equals("本地档案已删除"));
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      assertTrue(db.list("", false, 10).isEmpty());
    }
    assertTrue(
        android.webkit.CookieManager.getInstance()
            .getCookie(PAGE)
            .contains("nova_archive_delete=retained"));
  }

  private File evidence(String name, byte[] bytes) throws Exception {
    File f = new File(instrument.getTargetContext().getExternalFilesDir(null), name);
    java.nio.file.Files.write(f.toPath(), bytes);
    return f;
  }

  private static String quote(String s) {
    return "'" + s.replace("'", "'\\''") + "'";
  }

  private byte[] shellBytes(String command) {
    ParcelFileDescriptor[] pipes = instrument.getUiAutomation().executeShellCommandRw("sh");
    try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pipes[0]);
        OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipes[1])) {
      out.write((command + "\n").getBytes(StandardCharsets.UTF_8));
      out.close();
      return in.readAllBytes();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void actualSystemPrintSavesArchivePdf() throws Exception {
    store(ArchiveFixtures.json(1, true));
    openReader();
    evidence(
        "archive-print-source.html",
        ((String) field(reader, "html")).getBytes(StandardCharsets.UTF_8));
    click("导出");
    click("打印 / PDF");
    waitFor(
        "print window",
        () -> {
          AccessibilityNodeInfo r = instrument.getUiAutomation().getRootInActiveWindow();
          return r != null && "com.android.printspooler".contentEquals(r.getPackageName());
        });
    waitFor(
        "destination ready",
        () -> {
          AccessibilityNodeInfo r = instrument.getUiAutomation().getRootInActiveWindow();
          return find(r, "Save as PDF") != null || find(r, "Select a printer") != null;
        });
    waitFor(
        "paginated preview",
        () -> {
          AccessibilityNodeInfo r = instrument.getUiAutomation().getRootInActiveWindow();
          if (r == null) return false;
          for (AccessibilityNodeInfo n :
              r.findAccessibilityNodeInfosByViewId("com.android.printspooler:id/page_number"))
            if (n.isVisibleToUser()
                && n.getText() != null
                && n.getText().toString().matches("[0-9]+/[0-9]+")) return true;
          return false;
        });
    if (find(instrument.getUiAutomation().getRootInActiveWindow(), "Save as PDF") == null) {
      waitFor(
          "destination picker",
          () -> {
            AccessibilityNodeInfo r = instrument.getUiAutomation().getRootInActiveWindow();
            if (r == null) return false;
            for (AccessibilityNodeInfo n :
                r.findAccessibilityNodeInfosByViewId(
                    "com.android.printspooler:id/destination_spinner"))
              if (n.isClickable()) return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return false;
          });
      waitFor(
          "destination menu",
          () ->
              find(instrument.getUiAutomation().getRootInActiveWindow(), "All printers…") != null);
      click("Save as PDF");
    }
    waitFor(
        "PDF ready",
        () -> {
          AccessibilityNodeInfo r = instrument.getUiAutomation().getRootInActiveWindow();
          if (r == null) return false;
          for (AccessibilityNodeInfo n :
              r.findAccessibilityNodeInfosByViewId("com.android.printspooler:id/print_button"))
            if (n.isEnabled()) return true;
          return false;
        });
    SystemClock.sleep(1000);
    waitFor(
        "save PDF",
        () -> {
          AccessibilityNodeInfo r = instrument.getUiAutomation().getRootInActiveWindow();
          if (r == null) return false;
          for (AccessibilityNodeInfo n :
              r.findAccessibilityNodeInfosByViewId("com.android.printspooler:id/print_button"))
            if (n.isEnabled()) return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
          return false;
        });
    String name = "Nova-archive-fixture-" + UUID.randomUUID() + ".pdf";
    waitFor(
        "PDF filename",
        () -> {
          AccessibilityNodeInfo r = instrument.getUiAutomation().getRootInActiveWindow();
          if (r == null) return false;
          for (String id :
              List.of(
                  "android:id/title",
                  "com.google.android.documentsui:id/filename",
                  "com.android.documentsui:id/filename"))
            for (AccessibilityNodeInfo n : r.findAccessibilityNodeInfosByViewId(id)) {
              Bundle a = new Bundle();
              a.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, name);
              return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, a);
            }
          return false;
        });
    click("Save");
    waitFor("print finished", () -> field(reader, "printer") == null);
    AtomicReference<byte[]> actual = new AtomicReference<>();
    waitFor(
        "saved actual PDF",
        () -> {
          String paths =
              new String(
                  shellBytes(
                      "find /sdcard/Download /sdcard/Documents -type f -name "
                          + quote(name)
                          + " 2>/dev/null"),
                  StandardCharsets.UTF_8);
          for (String p : paths.split("\\r?\\n"))
            if (p.startsWith("/sdcard/")) {
              byte[] bytes = shellBytes("cat " + quote(p));
              if (bytes.length > 1000
                  && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
                actual.set(bytes);
                return true;
              }
            }
          return false;
        });
    File saved = evidence("archive.pdf", actual.get());
    try (ParcelFileDescriptor fd =
            ParcelFileDescriptor.open(saved, ParcelFileDescriptor.MODE_READ_ONLY);
        android.graphics.pdf.PdfRenderer pdf = new android.graphics.pdf.PdfRenderer(fd)) {
      assertTrue(pdf.getPageCount() > 1);
    }
  }

  @Test
  public void actualPrintCancelCanRepeat() throws Exception {
    store(ArchiveFixtures.json(1, false));
    openReader();
    for (int i = 0; i < 2; i++) {
      click("导出");
      click("打印 / PDF");
      waitFor(
          "print for cancel",
          () -> {
            AccessibilityNodeInfo r = instrument.getUiAutomation().getRootInActiveWindow();
            return r != null && "com.android.printspooler".contentEquals(r.getPackageName());
          });
      long[] last = {0};
      waitFor(
          "cancel print",
          () -> {
            if (field(reader, "printer") == null) return true;
            long now = SystemClock.uptimeMillis();
            if (now - last[0] > 2000) {
              instrument.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
              last[0] = now;
            }
            return false;
          });
      assertNotNull(field(reader, "reader"));
    }
  }
}
