package com.example.chatgptnova.archive;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;
import org.junit.*;

/** Generated fictional bytes only. No real export or filename is present. */
public final class ArchiveAssetFilesTest {
  private File directory;

  @Before
  public void before() throws Exception {
    directory = Files.createTempDirectory("nova-assets-test-").toFile();
  }

  @After
  public void after() {
    remove(directory);
  }

  static void remove(File f) {
    File[] children = f.listFiles();
    if (children != null) for (File child : children) remove(child);
    f.delete();
  }

  File file(byte[] data) throws Exception {
    File f = File.createTempFile("fictional-", ".dat", directory);
    Files.write(f.toPath(), data);
    return f;
  }

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

  ArchiveAssetFiles.Type type(byte[] bytes) throws Exception {
    return ArchiveAssetFiles.inspect(file(bytes), new ArchiveImporter.Control());
  }

  static void error(String code, Action action) throws Exception {
    try {
      action.run();
      fail("Expected fixed safety error");
    } catch (ArchiveError e) {
      assertEquals(code, e.code);
    }
  }

  interface Action {
    void run() throws Exception;
  }

  @Test
  public void pngAndJpegMagicIgnoreDatExtension() throws Exception {
    for (String format : List.of("png", "jpeg")) {
      ArchiveAssetFiles.Type t = type(image(format));
      assertEquals("image/" + format, t.mime);
      assertEquals(12, t.width);
      assertEquals(8, t.height);
      assertTrue(t.image());
    }
  }

  @Test
  public void pdfAndUnknownStayNonImages() throws Exception {
    ArchiveAssetFiles.Type pdf =
        type("%PDF-1.4\n%fictional\n%%EOF".getBytes(StandardCharsets.US_ASCII));
    assertEquals("application/pdf", pdf.mime);
    assertFalse(pdf.image());
    assertEquals(ArchiveAssetFiles.UNKNOWN, type(new byte[] {1, 2, 3, 4}).mime);
  }

  @Test
  public void boundedOoxmlClassifiesDocxAndXlsx() throws Exception {
    for (String family : List.of("word", "xl")) {
      byte[] body =
          zip(
              Map.of(
                  "[Content_Types].xml",
                  "<Types/>".getBytes(StandardCharsets.UTF_8),
                  family + "/content.xml",
                  "<fictional/>".getBytes(StandardCharsets.UTF_8)));
      assertEquals(
          family.equals("word") ? ArchiveAssetFiles.DOCX : ArchiveAssetFiles.XLSX, type(body).mime);
    }
  }

  @Test
  public void ambiguousOrMacroOrMissingManifestRemainUnknown() throws Exception {
    for (Map<String, byte[]> content :
        List.of(
            Map.of("word/a", new byte[] {1}),
            Map.of(
                "[Content_Types].xml",
                new byte[] {1},
                "word/a",
                new byte[] {1},
                "xl/a",
                new byte[] {1}),
            Map.of("[Content_Types].xml", new byte[] {1}, "word/vbaProject.bin", new byte[] {1})))
      assertEquals(ArchiveAssetFiles.UNKNOWN, type(zip(content)).mime);
  }

  @Test
  public void corruptKnownImageAndEmptyFileRejected() throws Exception {
    error("A13_ASSET_DAMAGED", () -> type(new byte[0]));
    error("A13_ASSET_DAMAGED", () -> type(new byte[] {(byte) 255, (byte) 216, (byte) 255}));
    error("A13_ASSET_DAMAGED", () -> type(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}));
  }

  @Test
  public void imagePixelBudgetWithoutDecoding() throws Exception {
    byte[] png = image("png");
    png[16] = 0;
    png[17] = 0;
    png[18] = 0x40;
    png[19] = 0;
    png[20] = 0;
    png[21] = 0;
    png[22] = 0x40;
    png[23] = 0;
    error("A05_ARCHIVE_TOO_LARGE", () -> type(png));
  }

  @Test
  public void sanitizedNameIsOnlyDisplay() {
    String name = ArchiveAssetFiles.displayName("../C:\\secret/../file\u0000.pdf");
    assertFalse(name.contains(".."));
    assertFalse(name.contains("/"));
    assertFalse(name.contains("\\"));
    assertFalse(name.contains("\u0000"));
    assertFalse(name.contains(":"));
    assertEquals(160, ArchiveAssetFiles.displayName("x".repeat(200)).length());
    assertEquals("附件", ArchiveAssetFiles.displayName(null));
  }

  @Test
  public void privatePathsOnlyAcceptGeneratedNames() throws Exception {
    for (String path :
        List.of(
            "../file.bin",
            "/file.bin",
            "file.pdf",
            "a/b.bin",
            "",
            "00000000-0000-0000-0000-000000000000.bin/../x")) {
      try {
        ArchiveAssetFiles.resolve(directory, path);
        fail();
      } catch (IOException expected) {
      }
    }
    String valid = UUID.randomUUID() + ".bin";
    assertEquals(
        directory.getCanonicalFile(), ArchiveAssetFiles.resolve(directory, valid).getParentFile());
  }

  @Test
  public void availableSpaceAndAssetBudgetAreIndependent() throws Exception {
    long reserved = ArchiveStorageBudget.DB_WAL_ALLOWANCE + ArchiveStorageBudget.FIXED_RESERVE;
    ArchiveAssetFiles.checkPlan(reserved + 128, 128);
    error("A09_STORAGE_FAILED", () -> ArchiveAssetFiles.checkPlan(reserved + 127, 128));
    error(
        "A05_ARCHIVE_TOO_LARGE",
        () -> ArchiveAssetFiles.checkPlan(Long.MAX_VALUE, ArchiveAssetFiles.TOTAL_LIMIT + 1));
    error("A05_ARCHIVE_TOO_LARGE", () -> ArchiveAssetFiles.checkPlan(Long.MAX_VALUE, -1));
  }

  @Test
  public void copyOnlyExplicitCandidatesAndRepeatedRefUsesOnePhysicalFile() throws Exception {
    File z = file(zip(Map.of("asset.dat", image("png"), "unrelated.dat", new byte[] {1, 2, 3})));
    ArchiveAssetFiles.Result first;
    try (ArchiveAssetFiles.Batch batch =
        new ArchiveAssetFiles.Batch(
            z, directory, List.of("asset.dat"), new ArchiveImporter.Control())) {
      first = batch.copy("asset.dat");
      assertSame(first, batch.copy("asset.dat"));
      assertEquals("complete", first.state);
      assertEquals(64, first.sha256.length());
      assertEquals("image/png", first.mime);
      assertThrows(IllegalStateException.class, () -> batch.copy("unrelated.dat"));
      batch.committed();
    }
    File root = new File(directory, "nova-archive-assets");
    assertEquals(1, root.list().length);
    assertEquals(first.bytes, ArchiveAssetFiles.resolve(root, first.relative).length());
    z.delete();
    assertTrue(ArchiveAssetFiles.resolve(root, first.relative).isFile());
  }

  @Test
  public void rollbackDeletesNewFilesButPreservesPreviouslyCommitted() throws Exception {
    File z = file(zip(Map.of("asset.dat", image("png"))));
    String old;
    try (ArchiveAssetFiles.Batch b =
        new ArchiveAssetFiles.Batch(
            z, directory, List.of("asset.dat"), new ArchiveImporter.Control())) {
      old = b.copy("asset.dat").relative;
      b.committed();
    }
    try (ArchiveAssetFiles.Batch b =
        new ArchiveAssetFiles.Batch(
            z, directory, List.of("asset.dat"), new ArchiveImporter.Control())) {
      b.copy("asset.dat");
    }
    File root = new File(directory, "nova-archive-assets");
    assertArrayEquals(new String[] {old}, root.list());
    assertEquals(0, new File(directory, "nova-archive-pending").list().length);
  }

  @Test
  public void cancellationAfterPublicationRollsBack() throws Exception {
    File z = file(zip(Map.of("a.dat", image("png"), "b.dat", image("jpeg"))));
    ArchiveImporter.Control control = new ArchiveImporter.Control();
    try (ArchiveAssetFiles.Batch b =
        new ArchiveAssetFiles.Batch(z, directory, List.of("a.dat", "b.dat"), control)) {
      b.copy("a.dat");
      control.cancelled.set(true);
      error("A07_IMPORT_CANCELLED", () -> b.copy("b.dat"));
    }
    assertEquals(0, new File(directory, "nova-archive-assets").list().length);
  }

  @Test
  public void absentAndZeroAssetsStayUnavailableWithoutGuessing() throws Exception {
    File z = file(zip(Map.of("empty.dat", new byte[0], "other.dat", image("png"))));
    try (ArchiveAssetFiles.Batch b =
        new ArchiveAssetFiles.Batch(
            z, directory, List.of("missing.dat", "empty.dat"), new ArchiveImporter.Control())) {
      assertEquals("missing", b.copy("missing.dat").state);
      assertEquals("damaged", b.copy("empty.dat").state);
      b.committed();
    }
    assertEquals(0, new File(directory, "nova-archive-assets").list().length);
  }

  @Test
  public void compressedAssetCrcMismatchIsUnavailable() throws Exception {
    byte[] bytes = zip(Map.of("asset.dat", image("png")));
    int p = central(bytes);
    bytes[p + 16] ^= 1;
    File z = file(bytes);
    try (ArchiveAssetFiles.Batch b =
        new ArchiveAssetFiles.Batch(
            z, directory, List.of("asset.dat"), new ArchiveImporter.Control())) {
      assertEquals("damaged", b.copy("asset.dat").state);
      b.committed();
    }
    assertEquals(0, new File(directory, "nova-archive-assets").list().length);
  }

  static int central(byte[] b) {
    for (int i = 0; i < b.length - 4; i++) if (ArchiveZip.u32(b, i) == 0x02014b50L) return i;
    throw new AssertionError();
  }

  @Test
  public void allOuterPathsValidatedEvenUnreferenced() throws Exception {
    File z = file(zip(Map.of("../unrelated.dat", new byte[] {1}, "safe.dat", new byte[] {2})));
    error("A02_INVALID_ZIP", () -> ArchiveZip.inspect(z, new ArchiveImporter.Control(), false));
  }

  @Test
  public void duplicateCanonicalNamesRejectedBeforeIndex() throws Exception {
    File z = file(zip(Map.of("same.dat", new byte[] {1}, "./same.dat", new byte[] {2})));
    error("A02_INVALID_ZIP", () -> ArchiveZip.inspect(z, new ArchiveImporter.Control(), false));
  }

  @Test
  public void zipControlNamesRejected() throws Exception {
    File z = file(zip(Map.of("bad\n.dat", new byte[] {1})));
    error("A02_INVALID_ZIP", () -> ArchiveZip.inspect(z, new ArchiveImporter.Control(), false));
  }

  @Test
  public void symlinkCentralAttributesRejected() throws Exception {
    byte[] b = zip(Map.of("link.dat", new byte[] {1}));
    int p = central(b);
    b[p + 5] = 3;
    b[p + 40] = 0;
    b[p + 41] = (byte) 0xa0;
    File z = file(b);
    error("A02_INVALID_ZIP", () -> ArchiveZip.inspect(z, new ArchiveImporter.Control(), false));
  }

  @Test
  public void encryptedAndUnsupportedMethodRejected() throws Exception {
    for (int mode : List.of(0, 1)) {
      byte[] b = zip(Map.of("asset.dat", new byte[] {1}));
      int p = central(b);
      if (mode == 0) {
        b[p + 8] |= 1;
        b[6] |= 1;
      } else {
        b[p + 10] = 99;
        b[8] = 99;
      }
      File z = file(b);
      error("A02_INVALID_ZIP", () -> ArchiveZip.inspect(z, new ArchiveImporter.Control(), false));
    }
  }

  @Test
  public void localCentralNameMismatchRejected() throws Exception {
    byte[] b = zip(Map.of("asset.dat", new byte[] {1}));
    b[30] = 'z';
    File z = file(b);
    error("A02_INVALID_ZIP", () -> ArchiveZip.inspect(z, new ArchiveImporter.Control(), false));
  }

  @Test
  public void truncationAndNestedTraversalRejected() throws Exception {
    byte[] b = zip(Map.of("asset.dat", new byte[] {1}));
    File z = file(Arrays.copyOf(b, b.length - 1));
    error("A02_INVALID_ZIP", () -> ArchiveZip.inspect(z, new ArchiveImporter.Control(), false));
    error(
        "A02_INVALID_ZIP",
        () ->
            type(zip(Map.of("[Content_Types].xml", new byte[] {1}, "../word/a", new byte[] {1}))));
  }

  @Test
  public void nestedDirectoryAndExpandedBudgetBounded() throws Exception {
    Map<String, byte[]> many = new LinkedHashMap<>();
    for (int i = 0; i < 2049; i++) many.put("word/" + i, new byte[0]);
    File z = file(zip(many));
    error(
        "A05_ARCHIVE_TOO_LARGE", () -> ArchiveZip.inspect(z, new ArchiveImporter.Control(), true));
    byte[] b = zip(Map.of("large", new byte[] {1}));
    int p = central(b);
    long size = 65L * 1024 * 1024;
    for (int i = 0; i < 4; i++) b[p + 24 + i] = (byte) (size >> (8 * i));
    File f = file(b);
    error(
        "A05_ARCHIVE_TOO_LARGE", () -> ArchiveZip.inspect(f, new ArchiveImporter.Control(), true));
  }

  @Test
  public void candidateCountAndSingleSizeLimitBeforeCopy() throws Exception {
    File z = file(zip(Map.of("asset.dat", new byte[] {1})));
    List<String> refs = new ArrayList<>();
    for (int i = 0; i < 2049; i++) refs.add("asset" + i);
    error(
        "A05_ARCHIVE_TOO_LARGE",
        () -> new ArchiveAssetFiles.Batch(z, directory, refs, new ArchiveImporter.Control()));
    byte[] b = zip(Map.of("asset.dat", new byte[] {1}));
    int p = central(b);
    long size = ArchiveAssetFiles.SINGLE_LIMIT + 1;
    for (int i = 0; i < 4; i++) b[p + 24 + i] = (byte) (size >> (8 * i));
    File oversized = file(b);
    error(
        "A05_ARCHIVE_TOO_LARGE",
        () ->
            new ArchiveAssetFiles.Batch(
                oversized, directory, List.of("asset.dat"), new ArchiveImporter.Control()));
  }

  @Test
  public void staleRecoveryPreservesCommittedAndRejectsUnsafeReferenceSet() throws Exception {
    File root = new File(directory, "nova-archive-assets");
    assertTrue(root.mkdir());
    String old = UUID.randomUUID() + ".bin", orphan = UUID.randomUUID() + ".bin";
    Files.write(ArchiveAssetFiles.resolve(root, old).toPath(), new byte[] {1});
    Files.write(ArchiveAssetFiles.resolve(root, orphan).toPath(), new byte[] {2});
    File pending = new File(directory, "nova-archive-pending/" + UUID.randomUUID());
    assertTrue(pending.mkdirs());
    Files.write(new File(pending, UUID.randomUUID() + ".bin").toPath(), new byte[] {3});
    try {
      ArchiveAssetFiles.recover(directory, Set.of("../untrusted"));
      fail();
    } catch (IOException expected) {
    }
    assertEquals(2, root.list().length);
    assertTrue(pending.exists());
    ArchiveAssetFiles.recover(directory, Set.of(old));
    assertArrayEquals(new String[] {old}, root.list());
    assertFalse(pending.exists());
  }

  @Test
  public void unknownExtraFieldsAllowedButMalformedAndZip64Rejected() throws Exception {
    for (int id : new int[] {12345, 1}) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try (ZipOutputStream z = new ZipOutputStream(out)) {
        ZipEntry e = new ZipEntry("safe.dat");
        e.setExtra(new byte[] {(byte) id, (byte) (id >> 8), 0, 0});
        z.putNextEntry(e);
        z.write(new byte[] {1});
        z.closeEntry();
      }
      File f = file(out.toByteArray());
      // Java's writer may strip ZIP64 extras; directly mutate an unrelated extra tag instead.
      if (id == 12345) ArchiveZip.inspect(f, new ArchiveImporter.Control(), false);
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream z = new ZipOutputStream(out)) {
      ZipEntry e = new ZipEntry("safe.dat");
      e.setExtra(new byte[] {57, 48, 0, 0});
      z.putNextEntry(e);
      z.write(new byte[] {1});
      z.closeEntry();
    }
    byte[] b = out.toByteArray();
    int c = central(b), extra = c + 46 + ArchiveZip.u16(b, c + 28);
    b[extra] = 1;
    b[extra + 1] = 0;
    File zip64 = file(b);
    error("A02_INVALID_ZIP", () -> ArchiveZip.inspect(zip64, new ArchiveImporter.Control(), false));
    b[extra] = 57;
    b[extra + 1] = 48;
    b[extra + 2] = 1;
    File malformed = file(b);
    error(
        "A02_INVALID_ZIP",
        () -> ArchiveZip.inspect(malformed, new ArchiveImporter.Control(), false));
  }
}
