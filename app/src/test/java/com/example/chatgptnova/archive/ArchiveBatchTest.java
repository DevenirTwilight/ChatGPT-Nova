package com.example.chatgptnova.archive;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.junit.Test;

public final class ArchiveBatchTest {
  ArchiveModel.Conversation conversation(String id, String title) throws Exception {
    Map<String, Object> d = ArchiveCoreTest.data(id);
    d.put("title", title);
    return new ArchiveModel.Conversation(d);
  }

  String text(ZipFile z, String name) throws Exception {
    return new String(z.getInputStream(z.getEntry(name)).readAllBytes(), StandardCharsets.UTF_8);
  }

  @Test
  public void duplicateUnsafeUnicodeTitlesRemainSeparateAndVerified() throws Exception {
    File f = File.createTempFile("batch-test-", ".zip");
    try {
      try (ArchiveBatch b =
          new ArchiveBatch(new FileOutputStream(f), new ArchiveImporter.Control())) {
        b.add(conversation("1", "../同名😀/x"), false, Collections.emptyMap());
        b.failure("bad", "private error must not escape");
        b.add(conversation("2", "../同名😀/x"), true, Collections.emptyMap());
        b.finish();
        assertEquals(2, b.succeeded);
        assertEquals(1, b.failed);
      }
      ArchiveBatch.verify(f, new ArchiveImporter.Control());
      try (ZipFile z = new ZipFile(f)) {
        assertEquals(5, z.size());
        String manifest = text(z, "manifest.json");
        assertFalse(manifest.contains("private error"));
        assertTrue(manifest.contains("A11_EXPORT_FAILED"));
        assertTrue(
            text(z, ArchiveBatch.directory(1, "../同名😀/x") + "/conversation.md")
                .contains("question"));
        assertTrue(
            text(z, ArchiveBatch.directory(3, "../同名😀/x") + "/conversation.html")
                .contains("answer"));
        z.stream()
            .forEach(
                e -> {
                  assertFalse(e.getName().contains("../"));
                  assertFalse(e.getName().contains("\\"));
                });
      }
    } finally {
      f.delete();
    }
  }

  @Test
  public void reportsAndAttachmentsTravelWithOnlyTheirConversation() throws Exception {
    File f = File.createTempFile("batch-test-", ".zip"),
        image = File.createTempFile("asset-test-", ".png");
    Files.write(image.toPath(), new byte[] {1, 2, 3});
    try {
      ArchiveModel.Conversation c = conversation("thread", "Study");
      c.reports.add(
          ArchiveResearchTest.read(
                  Collections.singletonList(
                      ArchiveResearchTest.record(ArchiveResearchTest.FILE, "thread")),
                  Collections.singletonMap(
                      ArchiveResearchTest.FILE + ".dat",
                      ArchiveResearchTest.body("completed", "text", true)))
              .get(0));
      Map<String, Object> d = ArchiveCoreTest.data("images");
      Map<String, Object> mapping = ArchiveModel.object(d.get("mapping"));
      mapping.put(
          "a",
          ArchiveCoreTest.node(
              "a", "u", "assistant", List.of(Map.of("asset_pointer", "file_test"))));
      ArchiveModel.Conversation a = new ArchiveModel.Conversation(d);
      String identity =
          ArchiveDisplay.visible(a.nodes.get("a")).stream()
              .filter(v -> v.asset != null)
              .findFirst()
              .get()
              .asset
              .identity;
      try (ArchiveBatch b =
          new ArchiveBatch(new FileOutputStream(f), new ArchiveImporter.Control())) {
        b.add(c, false, Collections.emptyMap());
        b.add(
            a,
            false,
            Collections.singletonMap(
                identity, new ArchiveAsset("图.png", "image/png", "complete", 3, 1, 1, image)));
        b.finish();
      }
      ArchiveBatch.verify(f, new ArchiveImporter.Control());
      try (ZipFile z = new ZipFile(f)) {
        String md = text(z, ArchiveBatch.directory(1, "Study") + "/conversation.md");
        assertTrue(md.contains("REPORT-FIRST"));
        assertTrue(md.contains("REPORT-LAST"));
        assertFalse(md.contains("HIDDEN-ACTIVITY"));
        String dir = ArchiveBatch.directory(2, a.title);
        assertArrayEquals(
            new byte[] {1, 2, 3},
            z.getInputStream(z.getEntry(dir + "/attachments/0001.png")).readAllBytes());
        assertTrue(text(z, dir + "/conversation.md").contains("attachments/0001.png"));
        assertFalse(text(z, dir + "/conversation.md").contains("REPORT-FIRST"));
      }
    } finally {
      f.delete();
      image.delete();
    }
  }

  @Test
  public void failureTitlesAreBoundedWithoutChangingDocumentTitle() throws Exception {
    File f = File.createTempFile("batch-title-", ".zip");
    String title = "主题".repeat(10000);
    try {
      try (ArchiveBatch b =
          new ArchiveBatch(new FileOutputStream(f), new ArchiveImporter.Control())) {
        b.failure(title, "A11_EXPORT_FAILED");
        b.add(conversation("1", title), false, Collections.emptyMap());
        b.finish();
      }
      ArchiveBatch.verify(f, new ArchiveImporter.Control());
      try (ZipFile z = new ZipFile(f)) {
        Map<String, Object> m =
            ArchiveModel.object(ArchiveModel.JSON.fromJson(text(z, "manifest.json"), Map.class));
        for (Object item : (List<?>) m.get("conversations"))
          assertEquals(512, ((String) ArchiveModel.object(item).get("title")).length());
        assertTrue(text(z, ArchiveBatch.directory(2, title) + "/conversation.md").contains(title));
      }
    } finally {
      f.delete();
    }
  }

  @Test
  public void cancellationNeverCreatesSuccessfulManifest() throws Exception {
    File f = File.createTempFile("batch-test-", ".zip");
    ArchiveImporter.Control control = new ArchiveImporter.Control();
    try {
      try (ArchiveBatch b = new ArchiveBatch(new FileOutputStream(f), control)) {
        b.add(conversation("1", "one"), false, Collections.emptyMap());
        control.cancelled.set(true);
        try {
          b.finish();
          fail();
        } catch (ArchiveError e) {
          assertEquals("A07_IMPORT_CANCELLED", e.code);
        }
      }
      try (ZipFile z = new ZipFile(f)) {
        assertNull(z.getEntry("manifest.json"));
      }
    } finally {
      f.delete();
    }
  }

  @Test
  public void tamperedMemberFailsReadback() throws Exception {
    File f = File.createTempFile("batch-test-", ".zip"),
        bad = File.createTempFile("batch-bad-", ".zip");
    try {
      try (ArchiveBatch b =
          new ArchiveBatch(new FileOutputStream(f), new ArchiveImporter.Control())) {
        b.add(conversation("1", "one"), false, Collections.emptyMap());
        b.finish();
      }
      try (ZipFile z = new ZipFile(f);
          ZipOutputStream out = new ZipOutputStream(new FileOutputStream(bad))) {
        for (ZipEntry e : Collections.list(z.entries())) {
          out.putNextEntry(new ZipEntry(e.getName()));
          out.write(
              e.getName().endsWith(".md") ? new byte[] {9} : z.getInputStream(e).readAllBytes());
          out.closeEntry();
        }
      }
      try {
        ArchiveBatch.verify(bad, new ArchiveImporter.Control());
        fail();
      } catch (IOException expected) {
      }
    } finally {
      f.delete();
      bad.delete();
    }
  }
}
