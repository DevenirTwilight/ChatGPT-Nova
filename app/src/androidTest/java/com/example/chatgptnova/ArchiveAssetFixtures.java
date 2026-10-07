package com.example.chatgptnova;

import com.example.chatgptnova.archive.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Test-only newly generated data; never contains user binary or identity. */
final class ArchiveAssetFixtures {
  static final String IMAGE = "file_11111111111111111111111111111111",
      DOC = "file_22222222222222222222222222222222";

  static byte[] zip(Map<String, byte[]> entries) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream z = new ZipOutputStream(out)) {
      for (Map.Entry<String, byte[]> e : entries.entrySet()) {
        z.putNextEntry(new ZipEntry(e.getKey()));
        z.write(e.getValue());
        z.closeEntry();
      }
    }
    return out.toByteArray();
  }

  static byte[] fixture(boolean malformed) throws Exception {
    Map<String, Object> mapping = new LinkedHashMap<>();
    mapping.put("root", Collections.singletonMap("parent", null));
    Map<String, Object> image = new LinkedHashMap<>();
    image.put("asset_pointer", "sediment://" + IMAGE);
    image.put("content_type", "image_asset_pointer");
    Map<String, Object> imageMeta = new LinkedHashMap<>();
    imageMeta.put("id", IMAGE);
    imageMeta.put("name", "fictional.png");
    imageMeta.put("mime_type", "image/png");
    Map<String, Object> docMeta = new LinkedHashMap<>();
    docMeta.put("id", DOC);
    docMeta.put("name", "fictional.docx");
    docMeta.put("mime_type", ArchiveAssetFiles.DOCX);
    mapping.put(
        "one",
        node(
            "root",
            Arrays.asList("fictional before", image, "fictional after"),
            Arrays.asList(imageMeta, docMeta)));
    mapping.put(
        "two", node("one", Collections.singletonList(""), Arrays.asList(docMeta, imageMeta)));
    Map<String, Object> c = new LinkedHashMap<>();
    c.put("conversation_id", "synthetic-asset-conversation");
    c.put("title", "Synthetic assets");
    c.put("current_node", "two");
    c.put("mapping", mapping);
    Map<String, String> names = new LinkedHashMap<>();
    names.put(IMAGE + ".dat", "fictional.png");
    names.put(DOC + ".dat", "fictional.docx");
    Map<String, byte[]> entries = new LinkedHashMap<>();
    entries.put(
        "wrapper/conversation_asset_file_names.json",
        ArchiveModel.JSON.toJson(names).getBytes(StandardCharsets.UTF_8));
    entries.put(
        "wrapper/conversations.json",
        (malformed ? "[{" : ArchiveModel.JSON.toJson(Collections.singletonList(c)))
            .getBytes(StandardCharsets.UTF_8));
    entries.put("wrapper/" + IMAGE + ".dat", image("png"));
    Map<String, byte[]> office = new LinkedHashMap<>();
    office.put("[Content_Types].xml", "<Types/>".getBytes(StandardCharsets.UTF_8));
    office.put("word/document.xml", "<fictional/>".getBytes(StandardCharsets.UTF_8));
    entries.put("wrapper/" + DOC + ".dat", zip(office));
    entries.put("wrapper/unreferenced.dat", new byte[] {1, 2, 3});
    return zip(entries);
  }

  static final String JPEG = "file_33333333333333333333333333333333",
      PDF = "file_44444444444444444444444444444444",
      SHEET = "file_55555555555555555555555555555555";

  static byte[] allTypes() throws Exception {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(fixture(false)))) {
      ZipEntry e;
      while ((e = in.getNextEntry()) != null) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
        entries.put(e.getName(), out.toByteArray());
      }
    }
    Map<String, Object> mapping = new LinkedHashMap<>();
    mapping.put("root", Collections.singletonMap("parent", null));
    List<Map<String, Object>> attachments = new ArrayList<>();
    Map<String, String> names = new LinkedHashMap<>();
    String[] ids = {IMAGE, DOC, JPEG, PDF, SHEET};
    String[] displays = {
      "fictional.png", "fictional.docx", "fictional.jpg", "fictional.pdf", "fictional.xlsx"
    };
    for (int i = 0; i < ids.length; i++) {
      Map<String, Object> ref = new LinkedHashMap<>();
      ref.put("id", ids[i]);
      ref.put("name", displays[i]);
      attachments.add(ref);
      names.put(ids[i] + ".dat", displays[i]);
    }
    mapping.put("one", node("root", Collections.singletonList(""), attachments));
    Map<String, Object> c = new LinkedHashMap<>();
    c.put("conversation_id", "synthetic-all-types");
    c.put("title", "Synthetic types");
    c.put("mapping", mapping);
    c.put("current_node", "one");
    entries.put(
        "wrapper/conversations.json",
        ArchiveModel.JSON.toJson(Collections.singletonList(c)).getBytes(StandardCharsets.UTF_8));
    entries.put(
        "wrapper/conversation_asset_file_names.json",
        ArchiveModel.JSON.toJson(names).getBytes(StandardCharsets.UTF_8));
    entries.put("wrapper/" + JPEG + ".dat", image("jpeg"));
    // A fictional bounded PDF signature fixture, not a document-reader validity claim.
    entries.put("wrapper/" + PDF + ".dat", "%PDF-1.4\n%%EOF\n".getBytes(StandardCharsets.US_ASCII));
    Map<String, byte[]> sheet = new LinkedHashMap<>();
    sheet.put("[Content_Types].xml", "<Types/>".getBytes(StandardCharsets.UTF_8));
    sheet.put("xl/workbook.xml", "<fictional/>".getBytes(StandardCharsets.UTF_8));
    entries.put("wrapper/" + SHEET + ".dat", zip(sheet));
    return zip(entries);
  }

  static Map<String, Object> node(String parent, List<?> parts, List<?> attachments) {
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("content_type", "multimodal_text");
    content.put("parts", parts);
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("attachments", attachments);
    Map<String, Object> author = new LinkedHashMap<>();
    author.put("role", "user");
    Map<String, Object> message = new LinkedHashMap<>();
    message.put("author", author);
    message.put("content", content);
    message.put("metadata", meta);
    Map<String, Object> node = new LinkedHashMap<>();
    node.put("parent", parent);
    node.put("message", message);
    return node;
  }

  static byte[] image(String type) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (type.equals("png")) {
      out.write(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
      ByteArrayOutputStream header = new ByteArrayOutputStream();
      DataOutputStream h = new DataOutputStream(header);
      h.writeInt(12);
      h.writeInt(8);
      h.write(new byte[] {8, 2, 0, 0, 0});
      pngChunk(out, "IHDR", header.toByteArray());
      ByteArrayOutputStream compressed = new ByteArrayOutputStream();
      try (DeflaterOutputStream deflater = new DeflaterOutputStream(compressed)) {
        for (int y = 0; y < 8; y++) {
          deflater.write(0);
          for (int x = 0; x < 12; x++) deflater.write(new byte[] {24, 108, (byte) 150});
        }
      }
      pngChunk(out, "IDAT", compressed.toByteArray());
      pngChunk(out, "IEND", new byte[0]);
    } else {
      // Small valid baseline grayscale JPEG: two all-zero DCT blocks, custom tiny Huffman tables.
      out.write(new byte[] {(byte) 255, (byte) 216});
      byte[] quant = new byte[65];
      Arrays.fill(quant, (byte) 16);
      quant[0] = 0;
      jpegMarker(out, 0xdb, quant);
      jpegMarker(out, 0xc0, new byte[] {8, 0, 8, 0, 12, 1, 1, 0x11, 0});
      byte[] table = new byte[18];
      table[1] = 1;
      jpegMarker(out, 0xc4, table);
      table[0] = 0x10;
      jpegMarker(out, 0xc4, table);
      jpegMarker(out, 0xda, new byte[] {1, 1, 0, 0, 63, 0});
      out.write(new byte[] {15, (byte) 255, (byte) 217});
    }
    return out.toByteArray();
  }

  static void pngChunk(ByteArrayOutputStream out, String type, byte[] data) throws Exception {
    DataOutputStream writer = new DataOutputStream(out);
    writer.writeInt(data.length);
    byte[] name = type.getBytes(StandardCharsets.US_ASCII);
    writer.write(name);
    writer.write(data);
    CRC32 crc = new CRC32();
    crc.update(name);
    crc.update(data);
    writer.writeInt((int) crc.getValue());
  }

  static void jpegMarker(ByteArrayOutputStream out, int marker, byte[] data) throws Exception {
    DataOutputStream writer = new DataOutputStream(out);
    writer.writeByte(255);
    writer.writeByte(marker);
    writer.writeShort(data.length + 2);
    writer.write(data);
  }
}
