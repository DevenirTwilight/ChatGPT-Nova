package com.example.chatgptnova;

import static org.junit.Assert.*;

import android.content.*;
import android.net.Uri;
import android.webkit.WebResourceResponse;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.chatgptnova.archive.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;

/** Native offline reader/provider checks on fictional generated assets, not a real user's data. */
public final class ArchiveAssetReaderTest {
  Context context;
  ActivityScenario<ArchiveReaderActivity> scenario;
  ArchiveReaderActivity activity;
  long row;

  @Before
  public void before() throws Exception {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ArchiveStore.deleteArchive(context);
    File input = File.createTempFile("fictional-reader-", ".zip", context.getCacheDir());
    try (OutputStream out = new FileOutputStream(input)) {
      out.write(ArchiveAssetFixtures.fixture(false));
    }
    try (ArchiveStore store = new ArchiveStore(context)) {
      store.importFile(
          input,
          true,
          "fictional.zip",
          UUID.randomUUID().toString(),
          new ArchiveImporter.Control());
      row = store.list("", false, 10).get(0).id;
    }
    assertTrue(input.delete());
    scenario =
        ActivityScenario.launch(
            new Intent(context, ArchiveReaderActivity.class).putExtra("row", row));
    long deadline = System.nanoTime() + 30_000_000_000L;
    while (System.nanoTime() < deadline) {
      java.util.concurrent.atomic.AtomicBoolean ready =
          new java.util.concurrent.atomic.AtomicBoolean();
      scenario.onActivity(
          a -> {
            activity = a;
            try {
              Field f = ArchiveReaderActivity.class.getDeclaredField("export");
              f.setAccessible(true);
              ready.set(((android.widget.Button) f.get(a)).isEnabled());
            } catch (Exception e) {
              throw new AssertionError(e);
            }
          });
      if (ready.get()) return;
      Thread.sleep(100);
    }
    fail("Offline asset reader did not become ready");
  }

  @After
  public void after() {
    if (scenario != null) scenario.close();
    ArchiveStore.deleteArchive(context);
  }

  @Test
  public void readerLocalImageAndTraversalPolicy() throws Exception {
    Map<String, ArchiveAsset> assets;
    try (ArchiveStore store = new ArchiveStore(context)) {
      assets = store.assets(row);
    }
    ArchiveAsset image = assets.get(ArchiveAssetFixtures.IMAGE);
    assertNotNull(image);
    Method local = ArchiveReaderActivity.class.getDeclaredMethod("localImage", String.class);
    local.setAccessible(true);
    WebResourceResponse response =
        (WebResourceResponse)
            local.invoke(
                activity, ArchiveRenderer.LOCAL_ORIGIN + "/images/" + image.file.getName());
    assertNotNull(response);
    assertEquals("image/png", response.getMimeType());
    byte[] header = new byte[8];
    try (InputStream in = response.getData()) {
      assertEquals(8, in.read(header));
    }
    assertArrayEquals(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}, header);
    for (String denied :
        new String[] {
          "file:///private",
          ArchiveRenderer.LOCAL_ORIGIN + "/images/../nova-archive.db",
          ArchiveRenderer.LOCAL_ORIGIN + "/images/" + image.file.getName() + "?secret",
          "https://example.invalid/images/" + image.file.getName()
        }) assertNull(local.invoke(activity, denied));
    scenario.onActivity(
        a -> {
          try {
            Field f = ArchiveReaderActivity.class.getDeclaredField("reader");
            f.setAccessible(true);
            SnapshotWebView reader = (SnapshotWebView) f.get(a);
            assertFalse(reader.web.getSettings().getJavaScriptEnabled());
            assertFalse(reader.web.getSettings().getAllowFileAccess());
            assertFalse(reader.web.getSettings().getAllowContentAccess());
            assertTrue(reader.web.getSettings().getBlockNetworkLoads());
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        });
  }

  @Test
  public void narrowProviderSharesCorrectBytesAndDoesNotExposeDatabase() throws Exception {
    try (ArchiveStore store = new ArchiveStore(context)) {
      ArchiveAsset document = store.assets(row).get(ArchiveAssetFixtures.DOC);
      assertNotNull(document);
      Uri uri =
          androidx.core.content.FileProvider.getUriForFile(
              context, context.getPackageName() + ".archiveassets", document.file, document.name);
      assertEquals("content", uri.getScheme());
      assertEquals(document.mime, context.getContentResolver().getType(uri));
      assertFalse(uri.toString().contains(document.file.getPath()));
      try (InputStream in = context.getContentResolver().openInputStream(uri)) {
        assertNotNull(in);
        assertEquals('P', in.read());
        assertEquals('K', in.read());
      }
      assertThrows(
          IllegalArgumentException.class,
          () ->
              androidx.core.content.FileProvider.getUriForFile(
                  context,
                  context.getPackageName() + ".archiveassets",
                  context.getDatabasePath(ArchiveStore.DATABASE)));
      android.content.pm.ProviderInfo info =
          context
              .getPackageManager()
              .resolveContentProvider(context.getPackageName() + ".archiveassets", 0);
      assertNotNull(info);
      assertFalse(info.exported);
      assertTrue(info.grantUriPermissions);
    }
  }

  @Test
  public void recreationRetainsAssetCardsAndPrintModelWithoutPrivateExportLinks() throws Exception {
    scenario.recreate();
    try (ArchiveStore store = new ArchiveStore(context)) {
      ArchiveModel.Conversation c = store.load(row);
      Map<String, ArchiveAsset> assets = store.assets(row);
      ArchiveTree.Selection selection = ArchiveTree.select(c, false);
      String
          portable = ArchiveRenderer.html(c, selection, assets, ArchiveRenderer.AssetMode.PORTABLE),
          print = ArchiveRenderer.html(c, selection, assets, ArchiveRenderer.AssetMode.PRINT),
          markdown = ArchiveRenderer.markdown(c, selection, assets);
      assertTrue(portable.contains("data:image/png;base64,"));
      assertTrue(print.contains("/images/"));
      assertFalse(print.contains("/attachments/"));
      assertTrue(markdown.contains("fictional.docx"));
      assertTrue(portable.contains("附件消息"));
      assertFalse(portable.contains("/images/"));
      assertFalse(portable.contains(context.getFilesDir().getPath()));
    }
  }
}
