package com.example.chatgptnova.archive;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** Field structures derived from audit, all values newly invented. */
public final class ArchiveDisplayTest {
  static final String FIRST = "file_11111111111111111111111111111111",
      SECOND = "file_22222222222222222222222222222222";

  static ArchiveModel.Node node(String type, List<Object> parts, List<Object> attachments) {
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("content_type", type);
    content.put("parts", parts);
    Map<String, Object> message = new LinkedHashMap<>();
    message.put("author", Map.of("role", "user"));
    message.put("content", content);
    message.put("metadata", Map.of("attachments", attachments));
    return new ArchiveModel.Node(
        "invented-node", Map.of("parent", "invented-parent", "message", message));
  }

  static Map<String, Object> image(String id) {
    return Map.of(
        "content_type",
        "image_asset_pointer",
        "asset_pointer",
        "sediment://" + id,
        "width",
        12,
        "height",
        8);
  }

  static Map<String, Object> attachment(String id) {
    return Map.of(
        "id", id, "name", "fictional.docx", "mime_type", ArchiveAssetFiles.DOCX, "size", 100);
  }

  @Test
  public void exactVerifiedIdentityAndPartOrdering() throws Exception {
    List<ArchiveDisplay.Block> blocks =
        ArchiveDisplay.visible(
            node(
                "multimodal_text",
                List.of("before", image(FIRST), Map.of("text", "middle"), image(SECOND), "after"),
                List.of()));
    assertEquals(5, blocks.size());
    assertEquals("before", blocks.get(0).text);
    assertEquals(FIRST, blocks.get(1).asset.identity);
    assertEquals(FIRST + ".dat", blocks.get(1).asset.entry);
    assertEquals("middle", blocks.get(2).text);
    assertEquals(SECOND, blocks.get(3).asset.identity);
    assertEquals("after", blocks.get(4).text);
  }

  @Test
  public void duplicateMetadataImageSuppressedWithoutRemovingDistinctAttachments()
      throws Exception {
    ArchiveModel.Node n =
        node(
            "multimodal_text",
            List.of("body", image(FIRST)),
            List.of(attachment(FIRST), attachment(SECOND)));
    List<ArchiveDisplay.Ref> refs = ArchiveDisplay.references(n);
    assertEquals(2, refs.size());
    assertTrue(refs.get(0).inline);
    assertFalse(refs.get(1).inline);
    assertEquals(1, refs.get(1).ordinal);
  }

  @Test
  public void attachmentOnlyPreservesEveryCardAndOrder() throws Exception {
    ArchiveModel.Node n = node("text", List.of(""), List.of(attachment(FIRST), attachment(SECOND)));
    List<ArchiveDisplay.Block> blocks = ArchiveDisplay.visible(n);
    assertEquals("附件消息", blocks.get(0).text);
    assertEquals(2, ArchiveDisplay.references(n).size());
    assertEquals(FIRST, ArchiveDisplay.references(n).get(0).identity);
    assertEquals(SECOND, ArchiveDisplay.references(n).get(1).identity);
  }

  @Test
  public void unknownPointerNeverNormalizedGuessedOrNetworkDecoded() throws Exception {
    for (String pointer :
        List.of(
            "file-service://" + FIRST,
            "sediment://" + FIRST + "?secret",
            "https://invalid.test/" + FIRST,
            FIRST + ".dat",
            "sediment://file_fake")) {
      ArchiveModel.Node n =
          node("multimodal_text", List.of(Map.of("asset_pointer", pointer)), List.of());
      assertFalse(ArchiveDisplay.references(n).get(0).recognized());
      assertEquals(pointer, ArchiveDisplay.references(n).get(0).identity);
    }
  }

  @Test
  public void hiddenThoughtsRetainReferencesButNeverShowBlocks() throws Exception {
    ArchiveModel.Node n = node("thoughts", List.of("fictional hidden", image(FIRST)), List.of());
    assertEquals(1, ArchiveDisplay.references(n).size());
    assertTrue(ArchiveDisplay.visible(n).isEmpty());
  }

  @Test
  public void unknownObjectsConservativeAndRecapTextPreserved() throws Exception {
    ArchiveModel.Node n = node("multimodal_text", List.of(Map.of("mystery", true)), List.of());
    assertTrue(ArchiveDisplay.visible(n).get(0).text.contains("未支持"));
    Map<String, Object> data = new LinkedHashMap<>(n.data);
    Map<String, Object> message = new LinkedHashMap<>(ArchiveModel.object(data.get("message")));
    message.put("content", Map.of("content_type", "reasoning_recap", "text", "invented recap"));
    data.put("message", message);
    assertEquals(
        "invented recap", ArchiveDisplay.visible(new ArchiveModel.Node("fake", data)).get(0).text);
  }

  @Test
  public void oversizedReferencesAndCountsRejected() throws Exception {
    ArchiveAssetFilesTest.error(
        "A05_ARCHIVE_TOO_LARGE",
        () ->
            ArchiveDisplay.references(
                node("text", List.of(), List.of(Map.of("id", "x".repeat(4097))))));
    List<Object> list = new ArrayList<>();
    for (int i = 0; i < 257; i++) list.add(attachment("file_" + String.format("%032x", i)));
    ArchiveAssetFilesTest.error(
        "A05_ARCHIVE_TOO_LARGE", () -> ArchiveDisplay.references(node("text", List.of(), list)));
  }

  @Test
  public void singleAssetBudgetMatchesAuditedPdfWithoutChangingJsonBudgets() {
    assertEquals(64L * 1024 * 1024, ArchiveAssetFiles.SINGLE_LIMIT);
    assertTrue(66707137L < ArchiveAssetFiles.SINGLE_LIMIT);
    assertEquals(256L * 1024 * 1024, ArchiveAssetFiles.TOTAL_LIMIT);
    assertEquals(64L * 1024 * 1024, ArchiveImporter.ENTRY_LIMIT);
    assertEquals(256L * 1024 * 1024, ArchiveImporter.TOTAL_LIMIT);
  }
}
