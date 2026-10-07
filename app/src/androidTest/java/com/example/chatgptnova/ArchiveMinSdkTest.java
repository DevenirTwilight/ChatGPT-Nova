package com.example.chatgptnova;

import static org.junit.Assert.*;

import android.content.Intent;
import android.print.PrintDocumentAdapter;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.chatgptnova.archive.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.*;
import org.junit.*;

/** Separate API26 foundation check: do not use Java9 fixture helpers or API31 shell evidence. */
public final class ArchiveMinSdkTest {
  private final android.app.Instrumentation instrument =
      InstrumentationRegistry.getInstrumentation();
  private static final String JSON =
      "[{\"conversation_id\":\"min-sdk-synthetic\",\"title\":\"Offline"
          + " Archive\",\"current_node\":\"a\",\"mapping\":{\"u\":{\"parent\":null,\"message\":{\"id\":\"u\",\"author\":{\"role\":\"user\"},\"content\":{\"content_type\":\"text\",\"parts\":[\"中文"
          + " café"
          + " 😀\"]}}},\"a\":{\"parent\":\"u\",\"message\":{\"id\":\"a\",\"author\":{\"role\":\"assistant\"},\"content\":{\"content_type\":\"text\",\"parts\":[\"```java\\n"
          + "code\\n"
          + "```\\n"
          + "\\n"
          + "|a|b|\\n"
          + "|-|-|\\n"
          + "|1|2|\\n"
          + "\\n"
          + "[link](https://example.org/)\"]}}}}}]";

  private long seed() throws Exception {
    instrument.getTargetContext().deleteDatabase(ArchiveStore.DATABASE);
    File f =
        File.createTempFile("archive-min-", ".json", instrument.getTargetContext().getCacheDir());
    java.nio.file.Files.write(f.toPath(), JSON.getBytes(StandardCharsets.UTF_8));
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      assertEquals(
          1,
          db.importFile(
                  f,
                  false,
                  "synthetic.json",
                  UUID.randomUUID().toString(),
                  new ArchiveImporter.Control())
              .newConversations);
      assertEquals(
          0,
          db.importFile(
                  f,
                  false,
                  "synthetic.json",
                  UUID.randomUUID().toString(),
                  new ArchiveImporter.Control())
              .newConversations);
      return db.list("offline", false, 1).get(0).id;
    } finally {
      f.delete();
    }
  }

  @Test
  public void privateDatabaseAndJavaMarkdownWorkAtMinSdk() throws Exception {
    long row = seed();
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      ArchiveModel.Conversation c = db.load(row);
      ArchiveTree.Selection s = ArchiveTree.select(c, false);
      assertEquals(2, s.messages.size());
      assertTrue(ArchiveRenderer.html(c, s).contains("<table>"));
      assertTrue(ArchiveRenderer.markdown(c, s).contains("```java"));
    }
  }

  @Test
  public void offlineWebViewAndPrintAdapterWorkAtMinSdk() throws Exception {
    long row = seed();
    AtomicBoolean ready = new AtomicBoolean();
    AtomicReference<SnapshotWebView> view = new AtomicReference<>();
    try (ActivityScenario<ArchiveReaderActivity> scenario =
        ActivityScenario.launch(
            new Intent(instrument.getTargetContext(), ArchiveReaderActivity.class)
                .putExtra("row", row))) {
      long end = android.os.SystemClock.uptimeMillis() + 60000;
      do {
        scenario.onActivity(
            a -> {
              try {
                java.lang.reflect.Field f = a.getClass().getDeclaredField("export");
                f.setAccessible(true);
                ready.set(((android.widget.Button) f.get(a)).isEnabled());
              } catch (Exception e) {
                throw new AssertionError(e);
              }
            });
        if (ready.get()) break;
        android.os.SystemClock.sleep(100);
      } while (android.os.SystemClock.uptimeMillis() < end);
      assertTrue(ready.get());
      scenario.onActivity(
          a -> {
            try {
              java.lang.reflect.Field f = a.getClass().getDeclaredField("reader");
              f.setAccessible(true);
              SnapshotWebView v = (SnapshotWebView) f.get(a);
              view.set(v);
              assertFalse(v.web.getSettings().getJavaScriptEnabled());
              assertTrue(v.web.getSettings().getBlockNetworkLoads());
              assertNull(v.snapshot);
              PrintDocumentAdapter adapter = v.printAdapter(() -> {});
              adapter.onStart();
              adapter.onFinish();
              assertNull(v.web.getParent());
            } catch (Exception e) {
              throw new AssertionError(e);
            }
          });
    }
  }
}
