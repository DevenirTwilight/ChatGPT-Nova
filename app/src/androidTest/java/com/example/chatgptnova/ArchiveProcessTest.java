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
    ArchiveStore.deleteArchive(instrument.getTargetContext());
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
    File assets =
        File.createTempFile(
            "archive-process-assets-", ".zip", instrument.getTargetContext().getCacheDir());
    try {
      java.nio.file.Files.write(assets.toPath(), ArchiveAssetFixtures.allTypes());
      try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
        assertEquals(
            5,
            db.importFile(
                    assets,
                    true,
                    "synthetic.zip",
                    UUID.randomUUID().toString(),
                    new ArchiveImporter.Control())
                .newAssets);
      }
    } finally {
      assertTrue(assets.delete());
    }
    library =
        ActivityScenario.launch(new Intent(instrument.getTargetContext(), ArchiveActivity.class));
  }

  @Test
  public void verifyPrivateArchiveAfterProcessRestart() throws Exception {
    try (ArchiveStore db = new ArchiveStore(instrument.getTargetContext())) {
      assertEquals(101, db.list("", false, 102).size());
      ArchiveModel.Conversation c = db.load(db.list("Archive 中 café 0", false, 1).get(0).id);
      assertEquals(4, c.nodes.size());
      assertEquals(2, ArchiveTree.select(c, false).messages.size());
      assertTrue(c.nodes.get("u").text().contains("ARCHIVE-FIRST"));
      long assetRow = db.list("Synthetic types", false, 1).get(0).id;
      java.util.Map<String, ArchiveAsset> assets = db.assets(assetRow);
      assertEquals(5, assets.size());
      for (ArchiveAsset a : assets.values()) assertTrue(a.available());
      String html =
          ArchiveRenderer.html(
              db.load(assetRow),
              ArchiveTree.select(db.load(assetRow), false),
              assets,
              ArchiveRenderer.AssetMode.PORTABLE);
      assertTrue(html.contains("data:image/jpeg;base64,"));
      assertTrue(html.contains("fictional.xlsx"));
    }
    library =
        ActivityScenario.launch(new Intent(instrument.getTargetContext(), ArchiveActivity.class));
  }
}
