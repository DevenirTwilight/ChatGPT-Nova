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

  static ArchiveModel.Node chat(String key, String parent, Double time, String text) {
    Map<String, Object> message = new LinkedHashMap<>();
    message.put("id", key);
    message.put("create_time", time);
    message.put("author", Map.of("role", "user"));
    message.put("content", Map.of("content_type", "text", "parts", List.of(text)));
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("parent", parent);
    data.put("message", message);
    return new ArchiveModel.Node(key, data);
  }

  static ArchiveModel.Conversation timelineConversation(List<ArchiveModel.Node> nodes)
      throws Exception {
    Map<String, Object> mapping = new LinkedHashMap<>();
    for (ArchiveModel.Node n : nodes) mapping.put(n.key, n.data);
    return new ArchiveModel.Conversation(
        Map.of(
            "conversation_id",
            "thread",
            "current_node",
            nodes.isEmpty() ? "" : nodes.get(nodes.size() - 1).key,
            "mapping",
            mapping));
  }

  static ArchiveResearch.Report report(String id, Double time, String text) {
    ArchiveModel.Node node = chat(id, null, 1d, text);
    Map<String, Object> message =
        new LinkedHashMap<>(ArchiveModel.object(node.data.get("message")));
    message.put("author", Map.of("role", "assistant"));
    return new ArchiveResearch.Report(
        id, "thread", "Fictional study", "complete", ArchiveModel.JSON.toJson(message), time);
  }

  static void ordered(String output, String... markers) {
    int prior = -1;
    for (String marker : markers) {
      int next = output.indexOf(marker);
      assertTrue(marker + " order", next > prior);
      prior = next;
    }
  }

  @Test
  public void timelineInterleavesReportsInEveryActualRenderModeWithoutMutatingGraph()
      throws Exception {
    ArchiveModel.Conversation c =
        timelineConversation(
            List.of(
                chat("a", null, 10d, "CHAT-A"),
                chat("b", "a", 30d, "CHAT-B"),
                chat("c", "b", 50d, "CHAT-C")));
    ArchiveResearch.Report later = report("later", 40d, "REPORT-LATER"),
        earlier = report("earlier", 20d, "REPORT-EARLIER");
    c.reports.add(later);
    c.reports.add(earlier);
    String original = c.nodes.get("b").raw;
    ArchiveTree.Selection s = ArchiveTree.select(c, false);
    for (ArchiveRenderer.AssetMode mode : ArchiveRenderer.AssetMode.values()) {
      String html = ArchiveRenderer.html(c, s, Collections.emptyMap(), mode);
      ordered(html, "CHAT-A", "REPORT-EARLIER", "CHAT-B", "REPORT-LATER", "CHAT-C");
      assertTrue(html.contains("按时间恢复位置"));
      assertFalse(html.contains("research-reports\""));
    }
    String md = ArchiveRenderer.markdown(c, s, Collections.emptyMap());
    ordered(md, "CHAT-A", "REPORT-EARLIER", "CHAT-B", "REPORT-LATER", "CHAT-C");
    assertEquals(original, c.nodes.get("b").raw);
    assertEquals(3, c.nodes.size());
    assertSame(later, c.reports.get(0));
  }

  @Test
  public void equalTimesPlaceAfterEqualChatAndOrderReportsDeterministically() throws Exception {
    ArchiveModel.Conversation c =
        timelineConversation(
            List.of(
                chat("a", null, 10d, "CHAT-A"),
                chat("b", "a", 20d, "CHAT-B"),
                chat("c", "b", 30d, "CHAT-C")));
    c.reports.add(report("z", 20d, "REPORT-Z"));
    c.reports.add(report("a", 20d, "REPORT-A"));
    ordered(
        ArchiveRenderer.markdown(c, ArchiveTree.select(c, false), Collections.emptyMap()),
        "CHAT-A",
        "CHAT-B",
        "REPORT-A",
        "REPORT-Z",
        "CHAT-C");
  }

  @Test
  public void unknownReportTimeRetainsBodyAndExplicitUnknownPosition() throws Exception {
    ArchiveModel.Conversation c =
        timelineConversation(
            List.of(chat("a", null, 10d, "CHAT-A"), chat("b", "a", 30d, "CHAT-B")));
    c.reports.add(report("missing", null, "REPORT-MISSING"));
    c.reports.add(report("valid", 20d, "REPORT-VALID"));
    c.reports.add(report("invalid", Double.POSITIVE_INFINITY, "REPORT-INVALID"));
    String html =
        ArchiveRenderer.html(
            c,
            ArchiveTree.select(c, false),
            Collections.emptyMap(),
            ArchiveRenderer.AssetMode.PRINT);
    ordered(html, "CHAT-A", "REPORT-VALID", "CHAT-B", "REPORT-INVALID", "REPORT-MISSING");
    assertTrue(html.contains("data-placement=\"unknown\""));
    assertTrue(html.contains("位置未确定"));
    String only =
        ArchiveRenderer.html(
            c,
            ArchiveTree.reportsOnly(),
            Collections.emptyMap(),
            ArchiveRenderer.AssetMode.PORTABLE);
    assertTrue(only.contains("data-placement=\"unknown\""));
    assertTrue(only.contains("位置未确定"));
  }

  @Test
  public void missingChatTimeNeverClaimsAReportSlot() throws Exception {
    ArchiveModel.Conversation c =
        timelineConversation(
            List.of(chat("a", null, null, "CHAT-A"), chat("b", "a", 30d, "CHAT-B")));
    c.reports.add(report("report", 20d, "REPORT-BODY"));
    String md = ArchiveRenderer.markdown(c, ArchiveTree.select(c, false), Collections.emptyMap());
    ordered(md, "CHAT-A", "CHAT-B", "REPORT-BODY");
    assertTrue(md.contains("位置未确定"));
    assertFalse(md.contains("按时间恢复位置"));
  }

  @Test
  public void nonMonotonicChatKeepsParentOrderAndOnlyAllowsConsistentTimeCuts() throws Exception {
    ArchiveModel.Conversation c =
        timelineConversation(
            List.of(
                chat("a", null, 100d, "CHAT-A"),
                chat("b", "a", 1d, "CHAT-B"),
                chat("c", "b", 30d, "CHAT-C")));
    c.reports.add(report("ambiguous", 20d, "REPORT-AMBIGUOUS"));
    c.reports.add(report("after", 110d, "REPORT-AFTER"));
    List<ArchiveTimeline.Entry> entries = ArchiveTimeline.select(c, ArchiveTree.select(c, false));
    assertEquals("time", entries.get(3).placement);
    assertEquals("unknown", entries.get(4).placement);
    ordered(
        ArchiveRenderer.markdown(c, ArchiveTree.select(c, false), Collections.emptyMap()),
        "CHAT-A",
        "CHAT-B",
        "CHAT-C",
        "REPORT-AFTER",
        "REPORT-AMBIGUOUS");
    ArchiveModel.Conversation cut =
        timelineConversation(
            List.of(
                chat("a", null, 10d, "CHAT-A"),
                chat("b", "a", 5d, "CHAT-B"),
                chat("c", "b", 30d, "CHAT-C"),
                chat("d", "c", 25d, "CHAT-D")));
    cut.reports.add(report("report", 20d, "REPORT-BODY"));
    ordered(
        ArchiveRenderer.markdown(cut, ArchiveTree.select(cut, false), Collections.emptyMap()),
        "CHAT-A",
        "CHAT-B",
        "REPORT-BODY",
        "CHAT-C",
        "CHAT-D");
  }

  @Test
  public void timeInsertionRespectsSelectedBranchAndReportShortcutOnlySortsReports()
      throws Exception {
    ArchiveModel.Conversation c =
        timelineConversation(
            List.of(chat("u", null, 10d, "CHAT-USER"), chat("main", "u", 30d, "CHAT-MAIN")));
    c.nodes.put("alt", chat("alt", "u", 20d, "CHAT-ALTERNATIVE"));
    c.reports.add(report("later", 25d, "REPORT-LATER"));
    c.reports.add(report("earlier", 15d, "REPORT-EARLIER"));
    String current =
        ArchiveRenderer.markdown(c, ArchiveTree.select(c, false), Collections.emptyMap());
    ordered(current, "CHAT-USER", "REPORT-EARLIER", "REPORT-LATER", "CHAT-MAIN");
    assertFalse(current.contains("CHAT-ALTERNATIVE"));
    ordered(
        ArchiveRenderer.markdown(c, ArchiveTree.select(c, true), Collections.emptyMap()),
        "CHAT-USER",
        "REPORT-EARLIER",
        "CHAT-ALTERNATIVE",
        "REPORT-LATER",
        "CHAT-MAIN");
    String only = ArchiveRenderer.markdown(c, ArchiveTree.reportsOnly(), Collections.emptyMap());
    ordered(only, "REPORT-EARLIER", "REPORT-LATER");
    assertFalse(only.contains("CHAT-"));
    assertTrue(only.contains("按报告时间排序"));
  }
}
