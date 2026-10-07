package com.example.chatgptnova;

import com.example.chatgptnova.archive.ArchiveModel;
import java.io.*;
import java.util.*;
import java.util.zip.*;

final class ArchiveFixtures {
  static Map<String, Object> node(String id, String parent, String role, String text) {
    Map<String, Object> n = new LinkedHashMap<>();
    n.put("parent", parent);
    n.put(
        "message",
        Map.of(
            "id",
            id,
            "author",
            Map.of("role", role),
            "channel",
            "final",
            "create_time",
            2,
            "content",
            Map.of("content_type", "text", "parts", List.of(text)),
            "metadata",
            Map.of("future", true)));
    n.put("message", new LinkedHashMap<>(ArchiveModel.object(n.get("message"))));
    return n;
  }

  static Map<String, Object> conversation(String id, String title, int time, boolean longBody) {
    Map<String, Object> c = new LinkedHashMap<>();
    c.put("conversation_id", id);
    c.put("title", title);
    c.put("create_time", time);
    c.put("update_time", time + 1);
    c.put("current_node", "a");
    Map<String, Object> m = new LinkedHashMap<>();
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("parent", null);
    root.put("message", null);
    m.put("root", root);
    m.put("u", node("u", "root", "user", "ARCHIVE-FIRST 中文 café 😀"));
    String body =
        "# Answer\n\n"
            + "```java\n"
            + "CODE-TAIL\n"
            + "```\n\n"
            + "- list\n\n"
            + "> quote\n\n"
            + "|a|b|\n"
            + "|-|-|\n"
            + "|1|TABLE-TAIL|\n\n"
            + "[Reference](https://example.org/nova-fixture)\n\n"
            + "$E=mc^2$\n\n";
    if (longBody) body += ("Long Archive paragraph français Noël naïve 中文.\n\n").repeat(350);
    body += "ARCHIVE-LAST";
    m.put("a", node("a", "u", "assistant", body));
    if (longBody) {
      Map<String, Object> user =
          ArchiveModel.object(ArchiveModel.object(m.get("u")).get("message"));
      user.put(
          "content",
          Map.of(
              "content_type",
              "multimodal_text",
              "parts",
              List.of(
                  "ARCHIVE-FIRST 中文 café 😀",
                  Map.of("text", "SCHEMA-OBJECT-TEXT"),
                  Map.of("asset_pointer", "synthetic-asset"))));
      Map<String, Object> thought = node("thought", "u", "assistant", "");
      ArchiveModel.object(thought.get("message"))
          .put(
              "content",
              Map.of(
                  "content_type",
                  "thoughts",
                  "thoughts",
                  List.of(Map.of("text", "SYNTHETIC-HIDDEN-THOUGHT"))));
      Map<String, Object> recap = node("recap", "thought", "assistant", "");
      ArchiveModel.object(recap.get("message"))
          .put("content", Map.of("content_type", "reasoning_recap", "content", "SCHEMA-RECAP"));
      m.put("thought", thought);
      m.put("recap", recap);
      ArchiveModel.object(m.get("a")).put("parent", "recap");
    }
    m.put("b", node("b", "u", "assistant", "OTHER-BRANCH"));
    c.put("mapping", m);
    c.put("future", Map.of("keep", "metadata"));
    return c;
  }

  static byte[] json(int count, boolean longBody) {
    List<Object> data = new ArrayList<>();
    for (int i = 0; i < count; i++)
      data.add(conversation("synthetic-" + i, "Archive 中 café " + i, 100 + i, longBody));
    return ArchiveModel.JSON.toJson(data).getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  static byte[] zip(boolean slow) throws IOException {
    Map<String, Object> c = conversation("synthetic-0", "Archive 中 café 0", 100, true);
    if (slow) {
      Random random = new Random(7);
      StringBuilder text = new StringBuilder();
      for (int i = 0; i < 100000; i++) text.append((char) ('a' + random.nextInt(26)));
      ArchiveModel.object(c.get("mapping")).put("a", node("a", "u", "assistant", text.toString()));
    }
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream z = new ZipOutputStream(bytes)) {
      z.putNextEntry(new ZipEntry("other.txt"));
      z.write(new byte[] {1});
      z.closeEntry();
      z.putNextEntry(new ZipEntry("data/conversations_1.json"));
      z.write(
          ArchiveModel.JSON.toJson(List.of(c)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
      z.closeEntry();
    }
    return bytes.toByteArray();
  }
}
