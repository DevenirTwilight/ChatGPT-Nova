package com.example.chatgptnova;

import com.example.chatgptnova.archive.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

final class ArchiveResearchFixtures {
  static final String FILE = "file_44444444444444444444444444444444", THREAD = "synthetic-research";

  static Map<String, Object> map(Object... values) {
    Map<String, Object> out = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) out.put((String) values[i], values[i + 1]);
    return out;
  }

  static Map<String, Object> conversation(String title) {
    Map<String, Object> content =
        map("content_type", "text", "parts", Collections.singletonList("Fictional chat body"));
    Map<String, Object> message =
        map(
            "id",
            "chat-message",
            "author",
            map("role", "user"),
            "content",
            content,
            "create_time",
            1);
    Map<String, Object> node = map("parent", null, "message", message);
    return map(
        "conversation_id",
        THREAD,
        "title",
        title,
        "current_node",
        "chat",
        "update_time",
        101,
        "mapping",
        Collections.singletonMap("chat", node));
  }

  static Map<String, Object> record(String id, String thread) {
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("library_artifact_type", "deep_research_report");
    r.put("file_id", id);
    r.put("origination_thread_id", thread);
    r.put("mime_type", "application/json");
    r.put("content_backing_kind", "sediment");
    return r;
  }

  static void add(Map<String, byte[]> entries, String thread) {
    entries.put(
        "library_files.json",
        ArchiveModel.JSON
            .toJson(Collections.singletonList(record(FILE, thread)))
            .getBytes(StandardCharsets.UTF_8));
    Map<String, Object> message = new LinkedHashMap<>();
    message.put("id", "independent-report-not-in-chat");
    message.put("author", Collections.singletonMap("role", "assistant"));
    message.put("create_time", 20);
    message.put(
        "content",
        map(
            "content_type",
            "text",
            "parts",
            Collections.singletonList(
                "RESEARCH-FIRST 中文 café\n\n"
                    + "# Independent study\n\n"
                    + "```java\n"
                    + "RESEARCH-CODE-TAIL\n"
                    + "```\n\n"
                    + "|a|b|\n"
                    + "|-|-|\n"
                    + "|1|RESEARCH-TABLE-TAIL|\n\n"
                    + "RESEARCH-LAST")));
    message.put("metadata", Collections.singletonMap("is_complete", true));
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("version", 1);
    root.put("title", "Fictional research");
    root.put("backing_conversation_id", "independent-backing");
    root.put("widget_state", map("status", "completed", "report_message", message));
    root.put(
        "activity_messages",
        Collections.singletonList(Collections.singletonMap("content", "RESEARCH-HIDDEN-ACTIVITY")));
    entries.put(FILE + ".dat", ArchiveModel.JSON.toJson(root).getBytes(StandardCharsets.UTF_8));
  }

  static byte[] zip(int mode) throws Exception {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    Map<String, Object> c = conversation(mode == 2 ? "changed" : "Fictional research chat");
    entries.put(
        "conversations.json",
        ArchiveModel.JSON.toJson(Collections.singletonList(c)).getBytes(StandardCharsets.UTF_8));
    add(entries, THREAD);
    if (mode == 3) {
      com.google.gson.JsonObject root =
          ArchiveModel.JSON.fromJson(
              new String(entries.get(FILE + ".dat"), StandardCharsets.UTF_8),
              com.google.gson.JsonObject.class);
      com.google.gson.JsonArray parts = new com.google.gson.JsonArray();
      parts.add(emoji(200000));
      root.getAsJsonObject("widget_state")
          .getAsJsonObject("report_message")
          .getAsJsonObject("content")
          .add("parts", parts);
      entries.put(FILE + ".dat", ArchiveModel.JSON.toJson(root).getBytes(StandardCharsets.UTF_8));
      // STORED keeps this deliberately repetitive Unicode fixture below the ZIP ratio guard.
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
        for (Map.Entry<String, byte[]> item : entries.entrySet()) {
          ZipEntry entry = new ZipEntry(item.getKey());
          CRC32 crc = new CRC32();
          crc.update(item.getValue());
          entry.setMethod(ZipEntry.STORED);
          entry.setSize(item.getValue().length);
          entry.setCompressedSize(item.getValue().length);
          entry.setCrc(crc.getValue());
          zip.putNextEntry(entry);
          zip.write(item.getValue());
          zip.closeEntry();
        }
      }
      return bytes.toByteArray();
    }
    if (mode == 1) entries.put(FILE + ".dat", "{".getBytes(StandardCharsets.UTF_8));
    if (mode == 2) {
      List<Object> records = new ArrayList<>();
      for (int i = 0; i < 33; i++) records.add(record(String.format("file_%032x", i), THREAD));
      entries.put(
          "library_files.json", ArchiveModel.JSON.toJson(records).getBytes(StandardCharsets.UTF_8));
    }
    return ArchiveAssetFixtures.zip(entries);
  }

  static String emoji(int count) {
    StringBuilder out = new StringBuilder(count * 2);
    for (int i = 0; i < count; i++) out.appendCodePoint(0x1f642);
    return out.toString();
  }
}
