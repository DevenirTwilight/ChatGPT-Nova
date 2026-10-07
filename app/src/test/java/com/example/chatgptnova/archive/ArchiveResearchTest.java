package com.example.chatgptnova.archive;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;

public final class ArchiveResearchTest {
  static final String FILE = "file_33333333333333333333333333333333";

  static Map<String, Object> record(String file, String thread) {
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("library_artifact_type", "deep_research_report");
    r.put("file_id", file);
    r.put("origination_thread_id", thread);
    r.put("mime_type", "application/json");
    r.put("content_backing_kind", "sediment");
    return r;
  }

  static String body(String status, String type, boolean complete) {
    return "{\"version\":1,\"title\":\"Fictional"
               + " study\",\"backing_conversation_id\":\"unrelated-backing\",\"activity_messages\":[{\"privateThought\":\"HIDDEN-ACTIVITY\"}],\"widget_state\":{\"status\":\""
        + status
        + "\",\"report_message\":{\"id\":\"not-in-chat\",\"author\":{\"role\":\"assistant\"},\"content\":{\"content_type\":\""
        + type
        + "\",\"parts\":[\"REPORT-FIRST\\n"
        + "\\n"
        + "中文 café\\n"
        + "\\n"
        + "```java\\n"
        + "int x=1;\\n"
        + "```\\n"
        + "\\n"
        + "REPORT-LAST\"]},\"metadata\":{\"is_complete\":"
        + complete
        + "},\"create_time\":12}}}";
  }

  static List<ArchiveResearch.Report> read(
      List<Map<String, Object>> records, Map<String, String> bodies) throws Exception {
    Map<String, byte[]> es = new LinkedHashMap<>();
    es.put(
        "wrapper/library_files.json",
        ArchiveModel.JSON.toJson(records).getBytes(StandardCharsets.UTF_8));
    for (Map.Entry<String, String> e : bodies.entrySet())
      es.put(e.getKey(), e.getValue().getBytes(StandardCharsets.UTF_8));
    File f = File.createTempFile("fictional-research-", ".zip");
    try {
      Files.write(f.toPath(), ArchiveAssetFilesTest.zip(es));
      List<ArchiveResearch.Report> reports = new ArrayList<>();
      ArchiveResearch.readZip(
          f, Collections.singleton("thread"), new ArchiveImporter.Control(), r -> reports.add(r));
      return reports;
    } finally {
      f.delete();
    }
  }

  @Test
  public void exactArtifactThreadAndFileRecoverSeparateBody() throws Exception {
    ArchiveResearch.Report r =
        read(
                Collections.singletonList(record(FILE, "thread")),
                Collections.singletonMap(FILE + ".dat", body("completed", "text", true)))
            .get(0);
    assertEquals("complete", r.state);
    assertEquals("thread", r.conversation);
    assertTrue(r.node().text().endsWith("REPORT-LAST"));
    assertFalse(r.message.contains("HIDDEN-ACTIVITY"));
  }

  @Test
  public void unrelatedThreadNeverAttachesByFilenameOrText() throws Exception {
    assertTrue(
        read(
                Collections.singletonList(record(FILE, "different")),
                Collections.singletonMap(FILE + ".dat", body("completed", "text", true)))
            .isEmpty());
  }

  @Test
  public void pendingHiddenOrIncompleteIsNotPresentedAsReportBody() throws Exception {
    for (String state : Arrays.asList("pending", "badType", "incomplete")) {
      String b =
          body(
              state.equals("pending") ? "running" : "completed",
              state.equals("badType") ? "thoughts" : "text",
              !state.equals("incomplete"));
      ArchiveResearch.Report r =
          read(
                  Collections.singletonList(record(FILE, "thread")),
                  Collections.singletonMap(FILE + ".dat", b))
              .get(0);
      assertFalse(r.state.equals("complete"));
      assertTrue(r.message.isEmpty());
    }
  }

  @Test
  public void missingMalformedAndAmbiguousStayUnavailable() throws Exception {
    Map<String, String> none = Collections.emptyMap();
    assertEquals(
        "missing", read(Collections.singletonList(record(FILE, "thread")), none).get(0).state);
    assertEquals(
        "unsupported",
        read(
                Collections.singletonList(record(FILE, "thread")),
                Collections.singletonMap(FILE + ".dat", "{"))
            .get(0)
            .state);
    Map<String, String> two = new LinkedHashMap<>();
    two.put("one/" + FILE + ".dat", body("completed", "text", true));
    two.put("two/" + FILE + ".dat", body("completed", "text", true));
    assertEquals(
        "unsupported", read(Collections.singletonList(record(FILE, "thread")), two).get(0).state);
  }

  @Test
  public void duplicateIdentityAndVersionAreRejected() throws Exception {
    try {
      read(Arrays.asList(record(FILE, "thread"), record(FILE, "thread")), Collections.emptyMap());
      fail();
    } catch (ArchiveError e) {
      assertEquals("A08_SCHEMA_UNSUPPORTED", e.code);
    }
    String b =
        body("completed", "text", true).replace("\"version\":1", "\"version\":1,\"version\":1");
    assertEquals(
        "unsupported",
        read(
                Collections.singletonList(record(FILE, "thread")),
                Collections.singletonMap(FILE + ".dat", b))
            .get(0)
            .state);
  }

  @Test
  public void perConversationLimitIsEnforcedWithoutTruncation() throws Exception {
    List<Map<String, Object>> records = new ArrayList<>();
    for (int i = 0; i < 33; i++) records.add(record(String.format("file_%032x", i), "thread"));
    try {
      read(records, Collections.emptyMap());
      fail();
    } catch (ArchiveError e) {
      assertEquals("A05_ARCHIVE_TOO_LARGE", e.code);
    }
  }

  @Test
  public void reportsAppearIndependentlyOfChatBranchInHtmlMarkdownAndPrint() throws Exception {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("id", "thread");
    source.put("mapping", Collections.emptyMap());
    ArchiveModel.Conversation c = new ArchiveModel.Conversation(source);
    c.reports.addAll(
        read(
            Collections.singletonList(record(FILE, "thread")),
            Collections.singletonMap(FILE + ".dat", body("completed", "text", true))));
    for (boolean all : Arrays.asList(false, true)) {
      ArchiveTree.Selection s = ArchiveTree.select(c, all);
      String md = ArchiveRenderer.markdown(c, s, Collections.emptyMap());
      assertTrue(md.contains(c.reports.get(0).node().text()));
      assertFalse(md.contains("HIDDEN-ACTIVITY"));
      for (ArchiveRenderer.AssetMode mode : ArchiveRenderer.AssetMode.values()) {
        String html = ArchiveRenderer.html(c, s, Collections.emptyMap(), mode);
        assertTrue(html.contains("REPORT-FIRST"));
        assertTrue(html.contains("REPORT-LAST"));
        assertTrue(html.contains("研究报告"));
        assertFalse(html.contains("HIDDEN-ACTIVITY"));
      }
    }
  }

  @Test
  public void cancellationIsRequiredFailure() throws Exception {
    File f = File.createTempFile("fictional-cancel-", ".zip");
    try {
      Files.write(f.toPath(), ArchiveAssetFilesTest.zip(Collections.emptyMap()));
      ArchiveImporter.Control c = new ArchiveImporter.Control();
      c.cancelled.set(true);
      try {
        ArchiveResearch.readZip(f, Collections.singleton("thread"), c, r -> {});
        fail();
      } catch (ArchiveError e) {
        assertEquals("A07_IMPORT_CANCELLED", e.code);
      }
    } finally {
      f.delete();
    }
  }
}
