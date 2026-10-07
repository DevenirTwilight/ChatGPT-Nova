package com.example.chatgptnova.archive;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;

public final class ArchiveAssetRendererTest {
  File dir;
  ArchiveModel.Conversation conversation;
  Map<String, ArchiveAsset> assets;

  @Before
  public void before() throws Exception {
    dir = Files.createTempDirectory("fictional-render-").toFile();
    File png = new File(dir, UUID.randomUUID() + ".bin");
    Files.write(png.toPath(), ArchiveAssetFilesTest.image("png"));
    File doc = new File(dir, UUID.randomUUID() + ".bin");
    Files.write(doc.toPath(), new byte[] {1, 2, 3});
    String one = ArchiveDisplayTest.FIRST, two = ArchiveDisplayTest.SECOND;
    ArchiveModel.Node node =
        ArchiveDisplayTest.node(
            "multimodal_text",
            List.of("before", ArchiveDisplayTest.image(one), "after"),
            List.of(ArchiveDisplayTest.attachment(one), ArchiveDisplayTest.attachment(two)));
    Map<String, Object> raw = new LinkedHashMap<>(node.data);
    raw.put("parent", null);
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("title", "Invented asset conversation");
    data.put("current_node", "node");
    data.put("mapping", Map.of("node", raw));
    conversation = new ArchiveModel.Conversation(data);
    assets =
        Map.of(
            one,
            new ArchiveAsset("fictional.png", "image/png", "complete", png.length(), 12, 8, png),
            two,
            new ArchiveAsset(
                "fictional.docx", ArchiveAssetFiles.DOCX, "complete", doc.length(), 0, 0, doc));
  }

  @After
  public void after() {
    ArchiveAssetFilesTest.remove(dir);
  }

  @Test
  public void portableEmbedsActualImageAndNeverPrivateLinksIdsOrPaths() throws Exception {
    String html =
        ArchiveRenderer.html(
            conversation,
            ArchiveTree.select(conversation, false),
            assets,
            ArchiveRenderer.AssetMode.PORTABLE);
    assertTrue(html.contains("data:image/png;base64,"));
    assertTrue(html.contains("fictional.docx"));
    assertTrue(html.contains("Archived locally in Nova"));
    assertTrue(html.indexOf("before") < html.indexOf("<figure>"));
    assertTrue(html.indexOf("<figure>") < html.indexOf("<p>after</p>"));
    assertFalse(html.contains("/attachments/"));
    assertFalse(html.contains("/images/"));
    assertFalse(html.contains(dir.getPath()));
    assertFalse(html.contains(ArchiveDisplayTest.FIRST));
    assertFalse(html.contains(ArchiveDisplayTest.SECOND));
    assertFalse(html.contains("<script"));
    String encoded = html.substring(html.indexOf("data:image/png;base64,") + 22);
    encoded = encoded.substring(0, encoded.indexOf('"'));
    assertArrayEquals(ArchiveAssetFilesTest.image("png"), Base64.getDecoder().decode(encoded));
  }

  @Test
  public void readerUsesPreciseLocalOriginAndPrintOmitsOpenLinks() throws Exception {
    String
        reader =
            ArchiveRenderer.html(
                conversation,
                ArchiveTree.select(conversation, false),
                assets,
                ArchiveRenderer.AssetMode.READER),
        print =
            ArchiveRenderer.html(
                conversation,
                ArchiveTree.select(conversation, false),
                assets,
                ArchiveRenderer.AssetMode.PRINT);
    assertTrue(reader.contains(ArchiveRenderer.LOCAL_ORIGIN + "/images/"));
    assertTrue(reader.contains("/attachments/"));
    assertFalse(reader.contains("file://"));
    assertFalse(reader.contains(dir.getPath()));
    assertTrue(print.contains("/images/"));
    assertFalse(print.contains("/attachments/"));
    assertTrue(print.contains("fictional.docx"));
  }

  @Test
  public void markdownKeepsDescriptorsWithoutInvalidLinksOrIdentities() throws Exception {
    String md =
        ArchiveRenderer.markdown(conversation, ArchiveTree.select(conversation, false), assets);
    assertTrue(md.contains("before"));
    assertTrue(md.contains("after"));
    assertTrue(md.contains("fictional.png"));
    assertTrue(md.contains("fictional.docx"));
    assertTrue(md.contains("单Markdown不携带binary"));
    assertFalse(md.contains(dir.getPath()));
    assertFalse(md.contains(ArchiveDisplayTest.FIRST));
    assertFalse(md.contains("nova-archive.invalid"));
  }

  @Test
  public void unavailableShownConsistentlyAndUnknownNotEmbedded() throws Exception {
    Map<String, ArchiveAsset> empty = Collections.emptyMap();
    assertTrue(
        ArchiveRenderer.html(
                conversation,
                ArchiveTree.select(conversation, false),
                empty,
                ArchiveRenderer.AssetMode.PORTABLE)
            .contains("附件未包含"));
    assertTrue(
        ArchiveRenderer.markdown(conversation, ArchiveTree.select(conversation, false), empty)
            .contains("附件未包含"));
    Map<String, ArchiveAsset> unknown = new HashMap<>(assets);
    ArchiveAsset image = assets.get(ArchiveDisplayTest.FIRST);
    unknown.put(
        ArchiveDisplayTest.FIRST,
        new ArchiveAsset(
            "fictional.dat", ArchiveAssetFiles.UNKNOWN, "complete", image.bytes, 0, 0, image.file));
    assertFalse(
        ArchiveRenderer.html(
                conversation,
                ArchiveTree.select(conversation, false),
                unknown,
                ArchiveRenderer.AssetMode.PORTABLE)
            .contains("<img "));
  }

  @Test
  public void portableOversizedImageIsDescriptorWithoutWholeFileRead() throws Exception {
    ArchiveAsset old = assets.get(ArchiveDisplayTest.FIRST);
    try (RandomAccessFile file = new RandomAccessFile(old.file, "rw")) {
      file.setLength(3L * 1024 * 1024);
    }
    Map<String, ArchiveAsset> map = new HashMap<>(assets);
    map.put(
        ArchiveDisplayTest.FIRST,
        new ArchiveAsset(
            "fictional.png", "image/png", "complete", old.file.length(), 12, 8, old.file));
    String html =
        ArchiveRenderer.html(
            conversation,
            ArchiveTree.select(conversation, false),
            map,
            ArchiveRenderer.AssetMode.PORTABLE);
    assertFalse(html.contains("data:image/png"));
    assertTrue(html.contains("超过单文件内嵌预算"));
    assertFalse(html.contains(old.file.getName()));
  }
}
