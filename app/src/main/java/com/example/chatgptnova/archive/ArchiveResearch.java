package com.example.chatgptnova.archive;

import com.google.gson.Strictness;
import com.google.gson.stream.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

/** Explicit exported Deep Research artifacts, separate from chat branches and activity thoughts. */
public final class ArchiveResearch {
  public static final int COUNT_LIMIT = 128, PER_CONVERSATION_LIMIT = 32;
  public static final long ENTRY_LIMIT = 4L * 1024 * 1024, TOTAL_LIMIT = 32L * 1024 * 1024;

  private ArchiveResearch() {}

  public static final class Report {
    public final String identity, conversation, title, state, message;
    public final Double created;

    public Report(
        String identity,
        String conversation,
        String title,
        String state,
        String message,
        Double created) {
      this.identity = identity;
      this.conversation = conversation;
      this.title = title;
      this.state = state;
      this.message = message;
      this.created = created;
    }

    public ArchiveModel.Node node() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("parent", null);
      data.put("message", ArchiveModel.JSON.fromJson(message, Map.class));
      return new ArchiveModel.Node(identity, data);
    }
  }

  public interface Sink {
    void accept(Report report) throws ArchiveError;
  }

  public static void readZip(
      File file, Set<String> imported, ArchiveImporter.Control control, Sink sink)
      throws ArchiveError {
    control.check();
    ArchiveImporter.checkContainerSize(file.length());
    try {
      ArchiveZip.inspect(file, control, false);
      try (ZipFile zip = new ZipFile(file)) {
        ZipEntry inventory = null;
        Map<String, List<ZipEntry>> entries = new HashMap<>();
        for (Enumeration<? extends ZipEntry> es = zip.entries(); es.hasMoreElements(); ) {
          control.check();
          ZipEntry e = es.nextElement();
          String name = ArchiveImporter.safeName(e.getName());
          String leaf = name.substring(name.lastIndexOf('/') + 1);
          if (e.isDirectory()) continue;
          entries.computeIfAbsent(leaf, k -> new ArrayList<>()).add(e);
          if (leaf.equals("library_files.json")) {
            if (inventory != null) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
            inventory = e;
          }
        }
        if (inventory == null) return;
        long total = inventory.getSize();
        List<Map<String, Object>> records = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        try (CheckedInput in = new CheckedInput(zip.getInputStream(inventory), inventory, control);
            JsonReader json = reader(in)) {
          if (json.peek() != JsonToken.BEGIN_ARRAY)
            throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
          json.beginArray();
          int count = 0;
          ArchiveImporter parser = new ArchiveImporter(control);
          while (json.hasNext()) {
            if (++count > 10000) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
            Map<String, Object> r =
                ArchiveModel.object(parser.value(json, 0, new ArchiveImporter.Budget()));
            if (!"deep_research_report".equals(r.get("library_artifact_type"))) continue;
            String id = ArchiveModel.string(r.get("file_id")),
                thread = ArchiveModel.string(r.get("origination_thread_id"));
            if (!id.matches("file_[0-9a-fA-F]{32}") || thread.isEmpty() || thread.length() > 4096)
              throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
            if (!identities.add(id)) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
            if (!imported.contains(thread)) continue;
            if (records.size() >= COUNT_LIMIT) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
            records.add(r);
          }
          json.endArray();
          finish(json, in);
        }
        Map<String, Integer> perConversation = new HashMap<>();
        for (Map<String, Object> r : records) {
          control.check();
          String id = ArchiveModel.string(r.get("file_id")),
              thread = ArchiveModel.string(r.get("origination_thread_id"));
          if (perConversation.merge(thread, 1, Integer::sum) > PER_CONVERSATION_LIMIT)
            throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
          String title = "研究报告";
          List<ZipEntry> matches = entries.getOrDefault(id + ".dat", Collections.emptyList());
          Report report =
              new Report(
                  id, thread, title, matches.isEmpty() ? "missing" : "unsupported", "", null);
          if (matches.size() == 1
              && "application/json".equals(r.get("mime_type"))
              && "sediment".equals(r.get("content_backing_kind"))) {
            ZipEntry e = matches.get(0);
            total += e.getSize();
            if (total > TOTAL_LIMIT) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
            try {
              report = parse(zip, e, id, thread, control);
            } catch (ArchiveError failure) {
              if (failure.code.equals("A07_IMPORT_CANCELLED")
                  || failure.code.equals("A05_ARCHIVE_TOO_LARGE")) throw failure;
              // Never overwrite an already recovered body with a malformed re-import.
            } catch (ArchiveImporter.ControlIOException
                | ArchiveImporter.LimitIOException failure) {
              throw failure;
            } catch (IOException | RuntimeException malformed) {
              // A recognized report with unreadable JSON is explicitly unavailable.
            }
          }
          sink.accept(report);
        }
      }
    } catch (ArchiveError e) {
      throw e;
    } catch (ArchiveImporter.ControlIOException e) {
      throw new ArchiveError(e.code);
    } catch (ArchiveImporter.LimitIOException e) {
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    } catch (IOException | RuntimeException e) {
      throw new ArchiveError("A04_JSON_PARSE_FAILED");
    }
  }

  private static Report parse(
      ZipFile zip, ZipEntry entry, String id, String thread, ArchiveImporter.Control control)
      throws IOException, ArchiveError {
    Object version = null;
    String title = "";
    Map<String, Object> widget = Collections.emptyMap();
    ArchiveImporter parser = new ArchiveImporter(control);
    try (CheckedInput in = new CheckedInput(zip.getInputStream(entry), entry, control);
        JsonReader json = reader(in)) {
      if (json.peek() != JsonToken.BEGIN_OBJECT) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
      json.beginObject();
      Set<String> keys = new HashSet<>();
      while (json.hasNext()) {
        control.check();
        String k = json.nextName();
        if (!keys.add(k)) throw new ArchiveError("A04_JSON_PARSE_FAILED");
        if (k.equals("version")) version = parser.value(json, 0, new ArchiveImporter.Budget());
        else if (k.equals("title"))
          title = ArchiveModel.string(parser.value(json, 0, new ArchiveImporter.Budget()));
        else if (k.equals("widget_state"))
          widget = ArchiveModel.object(parser.value(json, 0, new ArchiveImporter.Budget()));
        else
          json.skipValue(); // No activity thoughts/search progress/source searches are recovered.
      }
      json.endObject();
      finish(json, in);
    }
    if (!(version instanceof Number)
        || ((Number) version).doubleValue() != 1
        || title.length() > 4096) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
    Map<String, Object> message = ArchiveModel.object(widget.get("report_message"));
    if (!"completed".equals(widget.get("status")))
      return new Report(id, thread, title, "pending", "", null);
    Map<String, Object> content = ArchiveModel.object(message.get("content"));
    if (!"assistant".equals(ArchiveModel.object(message.get("author")).get("role"))
        || !"text".equals(content.get("content_type"))
        || !Boolean.TRUE.equals(ArchiveModel.object(message.get("metadata")).get("is_complete"))
        || !(content.get("parts") instanceof List))
      throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
    List<?> parts = (List<?>) content.get("parts");
    boolean nonempty = false;
    for (Object part : parts) {
      if (!(part instanceof String)) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
      nonempty |= !((String) part).trim().isEmpty();
    }
    String raw = ArchiveModel.JSON.toJson(message);
    if (!nonempty) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
    if (raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1024 * 1024)
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    return new Report(
        id,
        thread,
        title.isEmpty() ? "研究报告" : title,
        "complete",
        raw,
        ArchiveModel.number(message.get("create_time")));
  }

  private static JsonReader reader(InputStream input) {
    JsonReader r =
        new JsonReader(
            new ArchiveImporter.GuardReader(
                new InputStreamReader(
                    input,
                    StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT))));
    r.setStrictness(Strictness.STRICT);
    return r;
  }

  private static void finish(JsonReader json, CheckedInput in) throws IOException, ArchiveError {
    if (json.peek() != JsonToken.END_DOCUMENT) throw new ArchiveError("A04_JSON_PARSE_FAILED");
    if (in.count != in.expected.getSize() || in.crc.getValue() != in.expected.getCrc())
      throw new ArchiveError("A02_INVALID_ZIP");
  }

  private static final class CheckedInput extends FilterInputStream {
    final ZipEntry expected;
    final ArchiveImporter.Control control;
    final CRC32 crc = new CRC32();
    long count;

    CheckedInput(InputStream in, ZipEntry e, ArchiveImporter.Control c) throws ArchiveError {
      super(in);
      expected = e;
      control = c;
      if (e.getSize() < 0 || e.getSize() > ENTRY_LIMIT)
        throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    }

    @Override
    public int read() throws IOException {
      byte[] b = new byte[1];
      return read(b, 0, 1) < 0 ? -1 : b[0] & 255;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      try {
        control.check();
      } catch (ArchiveError e) {
        throw new ArchiveImporter.ControlIOException(e.code);
      }
      int n = super.read(b, off, Math.min(len, 32768));
      if (n > 0) {
        count += n;
        crc.update(b, off, n);
        if (count > ENTRY_LIMIT) throw new ArchiveImporter.LimitIOException();
      }
      return n;
    }
  }
}
