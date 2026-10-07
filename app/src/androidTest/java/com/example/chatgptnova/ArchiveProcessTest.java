package com.example.chatgptnova;

import static org.junit.Assert.*;

import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import com.example.chatgptnova.archive.*;
import java.io.*;
import java.util.UUID;
import org.junit.*;

/** Run seed -> shell force-stop -> verify as separate instrumentations. */
public final class ArchiveProcessTest extends FixtureActivity {
  private ActivityScenario<ArchiveActivity> library;

  @Before
  public void before() {
    start();
  }

  @After
  public void after() {
    if (library != null) library.close();
    if (scenario != null) scenario.close();
  }

  @Test
  public void seedPrivateArchiveForProcessRestart() throws Exception {
    instrument.getTargetContext().deleteDatabase(ArchiveStore.DATABASE);
    File file =
        File.createTempFile(
            "archive-process-", ".json", instrument.getTargetContext().getCacheDir());
    java.nio.file.Files.write(file.toPath(), ArchiveFixtures.json(100, false));
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      assertEquals(
          100,
          db.importFile(
                  file,
                  false,
                  "synthetic.json",
                  UUID.randomUUID().toString(),
                  new ArchiveImporter.Control())
              .newConversations);
    } finally {
      file.delete();
    }
    library =
        ActivityScenario.launch(new Intent(instrument.getTargetContext(), ArchiveActivity.class));
  }

  @Test
  public void verifyPrivateArchiveAfterProcessRestart() throws Exception {
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      assertEquals(100, db.list("", false, 101).size());
      ArchiveModel.Conversation c = db.load(db.list("", false, 1).get(0).id);
      assertEquals(4, c.nodes.size());
      assertEquals(2, ArchiveTree.select(c, false).messages.size());
      assertTrue(c.nodes.get("u").text().contains("ARCHIVE-FIRST"));
    }
    library =
        ActivityScenario.launch(new Intent(instrument.getTargetContext(), ArchiveActivity.class));
  }
}
