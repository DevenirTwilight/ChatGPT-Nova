package com.example.chatgptnova.archive;

import com.google.gson.Gson;
import java.util.*;

/** Owns exported mapping nodes, including roots, branches and uninterpreted metadata. */
public final class ArchiveModel {
  public static final Gson JSON =
      new com.google.gson.GsonBuilder()
          .serializeNulls()
          .setObjectToNumberStrategy(com.google.gson.ToNumberPolicy.BIG_DECIMAL)
          .create();

  private ArchiveModel() {}

  @SuppressWarnings("unchecked")
  public static Map<String, Object> object(Object value) {
    return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
  }

  public static String string(Object value) {
    return value instanceof String ? (String) value : "";
  }

  public static Double number(Object value) {
    if (!(value instanceof Number)) return null;
    double d = ((Number) value).doubleValue();
    return Double.isFinite(d) && Math.abs(d) < 253402300800d ? d : null;
  }

  public static final class Node {
    public final String key, id, parent, role, channel, contentType, status, raw;
    public final Double created;
    public final boolean hasMessage, malformedParent;
    public final Map<String, Object> data;

    public Node(String key, Map<String, Object> data) {
      this.key = key;
      this.data = data;
      raw = JSON.toJson(data);
      parent = string(data.get("parent"));
      malformedParent =
          !data.containsKey("parent")
              || (data.get("parent") != null && !(data.get("parent") instanceof String));
      Map<String, Object> m = object(data.get("message"));
      hasMessage = data.get("message") instanceof Map;
      id = string(m.get("id"));
      role = string(object(m.get("author")).get("role"));
      channel = string(m.get("channel"));
      contentType = string(object(m.get("content")).get("content_type"));
      status = string(m.get("status"));
      created = number(m.get("create_time"));
    }

    /** Visibility is independent of persistence: raw reasoning nodes remain in the tree/DB. */
    public boolean displayable() {
      return hasMessage && !contentType.equals("thoughts");
    }

    public String text() {
      if (!displayable()) return "";
      Map<String, Object> c = object(object(data.get("message")).get("content"));
      if (contentType.equals("reasoning_recap")) {
        for (String field : Arrays.asList("content", "text", "recap"))
          if (c.get(field) instanceof String) return (String) c.get(field);
      }
      Object p = c.get("parts");
      StringBuilder out = new StringBuilder();
      if (p instanceof List)
        for (Object part : (List<?>) p) {
          if (out.length() > 0) out.append('\n');
          if (part instanceof String) out.append(part);
          else if (part instanceof Map) {
            Map<String, Object> map = object(part);
            if (map.get("text") instanceof String) out.append(map.get("text"));
            else if (map.containsKey("asset_pointer")
                || map.containsKey("image_url")
                || map.containsKey("file_id")) out.append("[图片 / 附件 metadata 已保存在本地档案]");
            else out.append("[未支持的非文本内容；metadata 已保留]");
          } else out.append("[未支持的非文本内容；metadata 已保留]");
        }
      else if (c.get("text") instanceof String) out.append(c.get("text"));
      if (out.length() == 0 && !c.isEmpty())
        out.append(
            contentType.equals("reasoning_recap")
                ? "[推理摘要无可读文本；metadata 已保留]"
                : "[非文本或空内容；原始 metadata 已保留]");
      return out.toString();
    }
  }

  public static final class Conversation {
    public final String id, title, currentNode, header;
    public final Double created, updated;
    public final LinkedHashMap<String, Node> nodes;

    public Conversation(Map<String, Object> data) throws ArchiveError {
      id =
          string(data.get("conversation_id")).isEmpty()
              ? string(data.get("id"))
              : string(data.get("conversation_id"));
      title = string(data.get("title")).isEmpty() ? "无标题会话" : string(data.get("title"));
      currentNode = string(data.get("current_node"));
      if (id.length() > 4096 || currentNode.length() > 4096)
        throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
      created = number(data.get("create_time"));
      updated = number(data.get("update_time"));
      if (!(data.get("mapping") instanceof Map)) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
      Map<String, Object> h = new LinkedHashMap<>(data);
      h.remove("mapping");
      header = JSON.toJson(h);
      nodes = new LinkedHashMap<>();
      Map<String, Object> mapping = object(data.get("mapping"));
      if (mapping.size() > 10000) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
      for (Map.Entry<String, Object> e : mapping.entrySet()) {
        if (e.getKey().isEmpty() || !(e.getValue() instanceof Map))
          throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
        Node n = new Node(e.getKey(), object(e.getValue()));
        if (n.key.length() > 4096 || n.id.length() > 4096 || n.parent.length() > 4096)
          throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
        nodes.put(e.getKey(), n);
      }
    }

    public int messageCount() {
      int n = 0;
      for (Node v : nodes.values()) if (v.hasMessage) n++;
      return n;
    }
  }
}
