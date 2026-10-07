package com.example.chatgptnova.archive;

import java.util.*;

/** Shared, ordered interpretation of observed exported content; never guesses binary ownership. */
public final class ArchiveDisplay {
  private ArchiveDisplay() {}

  public static final class Ref {
    public final String identity, entry, declaredMime, displayName, raw;
    public final int ordinal;
    public final boolean inline;

    Ref(String value, Map<String, Object> metadata, int ordinal, boolean inline) {
      String normalized = value.startsWith("sediment://") ? value.substring(11) : value;
      // Only the verified official form. No URL decoding, query stripping or fuzzy filename
      // matching.
      boolean known = normalized.matches("file_[0-9a-fA-F]{32}");
      identity = known ? normalized : value;
      entry = known ? normalized + ".dat" : "";
      declaredMime = ArchiveModel.string(metadata.get("mime_type"));
      displayName = ArchiveAssetFiles.displayName(ArchiveModel.string(metadata.get("name")));
      raw = ArchiveModel.JSON.toJson(metadata);
      this.ordinal = ordinal;
      this.inline = inline;
    }

    public boolean recognized() {
      return !entry.isEmpty();
    }
  }

  public static final class Block {
    public final String text;
    public final Ref asset;

    private Block(String text, Ref ref) {
      this.text = text;
      asset = ref;
    }

    static Block text(String text) {
      return new Block(text, null);
    }

    static Block asset(Ref ref) {
      return new Block("", ref);
    }
  }

  /** Include references from hidden nodes in persistence; visibility is a separate decision. */
  public static List<Ref> references(ArchiveModel.Node node) throws ArchiveError {
    List<Ref> out = new ArrayList<>();
    for (Block block : parse(node)) if (block.asset != null) out.add(block.asset);
    return Collections.unmodifiableList(out);
  }

  public static List<Block> visible(ArchiveModel.Node node) throws ArchiveError {
    if (!node.displayable()) return Collections.emptyList();
    return Collections.unmodifiableList(parse(node));
  }

  private static List<Block> parse(ArchiveModel.Node node) throws ArchiveError {
    List<Block> out = new ArrayList<>();
    if (!node.hasMessage) return out;
    Map<String, Object> message = ArchiveModel.object(node.data.get("message"));
    Map<String, Object> content = ArchiveModel.object(message.get("content"));
    Set<String> inParts = new HashSet<>();
    int ordinal = 0;
    if (node.contentType.equals("reasoning_recap")) {
      for (String key : Arrays.asList("content", "text", "recap"))
        if (content.get(key) instanceof String) {
          out.add(Block.text((String) content.get(key)));
          break;
        }
    }
    Object parts = content.get("parts");
    if (out.isEmpty() && parts instanceof List) {
      for (Object part : (List<?>) parts) {
        if (part instanceof String) out.add(Block.text((String) part));
        else if (part instanceof Map) {
          Map<String, Object> data = ArchiveModel.object(part);
          boolean supported = false;
          if (data.get("text") instanceof String) {
            out.add(Block.text((String) data.get("text")));
            supported = true;
          }
          if (data.get("asset_pointer") instanceof String) {
            Ref ref = reference((String) data.get("asset_pointer"), data, ordinal++, true);
            out.add(Block.asset(ref));
            inParts.add(ref.identity);
            supported = true;
          }
          if (!supported) out.add(Block.text("[未支持的非文本内容；metadata 已保留]"));
        } else out.add(Block.text("[未支持的非文本内容；metadata 已保留]"));
      }
    } else if (out.isEmpty() && content.get("text") instanceof String)
      out.add(Block.text((String) content.get("text")));
    Object attachments = ArchiveModel.object(message.get("metadata")).get("attachments");
    if (attachments instanceof List)
      for (Object value : (List<?>) attachments) {
        Map<String, Object> data = ArchiveModel.object(value);
        String id = ArchiveModel.string(data.get("id"));
        if (id.isEmpty()) {
          if (!data.isEmpty()) out.add(Block.text("[附件引用无法识别；metadata 已保留]"));
          continue;
        }
        Ref ref = reference(id, data, ordinal, false);
        if (inParts.contains(ref.identity))
          continue; // Same image is also recorded in metadata in the real schema.
        ordinal++;
        out.add(Block.asset(ref));
      }
    if (ordinal > ArchiveAssetFiles.PER_CONVERSATION_LIMIT)
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    boolean hasAsset = false, hasText = false;
    for (Block b : out) {
      hasAsset |= b.asset != null;
      hasText |= !b.text.trim().isEmpty();
    }
    if (hasAsset && !hasText) out.add(0, Block.text("附件消息"));
    else if (out.isEmpty() && !content.isEmpty())
      out.add(
          Block.text(
              node.contentType.equals("reasoning_recap")
                  ? "[推理摘要无可读文本；metadata 已保留]"
                  : "[非文本或空内容；原始 metadata 已保留]"));
    return out;
  }

  private static Ref reference(String value, Map<String, Object> data, int ordinal, boolean inline)
      throws ArchiveError {
    if (value.length() > 4096) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    return new Ref(value, data, ordinal, inline);
  }
}
