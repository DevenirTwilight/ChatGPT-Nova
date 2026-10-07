package com.example.chatgptnova.archive;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;
import org.junit.Test;

public final class ArchiveAssetMapTest {
  static Map<String, String> read(String json) throws Exception {
    return ArchiveAssetMap.read(
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
        null,
        new ArchiveImporter.Control());
  }

  @Test
  public void verifiedStringMapRetainsDisplayMetadataAndExactIdentity() throws Exception {
    Map<String, String> actual =
        read(
            "{\"file_00000000000000000000000000000001.dat\":\"虚构"
                + " 😀.png\",\"file_00000000000000000000000000000002.dat\":\"虚构 😀.png\"}");
    assertEquals(2, actual.size());
    assertEquals("虚构 😀.png", actual.get("file_00000000000000000000000000000001.dat"));
    assertThrows(UnsupportedOperationException.class, () -> actual.clear());
  }

  @Test
  public void duplicateKeysTypesAndMalformedRejected() throws Exception {
    for (String json :
        List.of("{\"a.dat\":\"fake\",\"a.dat\":\"fake2\"}", "{\"a.dat\":\"unfinished\""))
      ArchiveAssetFilesTest.error("A04_JSON_PARSE_FAILED", () -> read(json));
    for (String json : List.of("[]", "{\"a.dat\":{}}", "{\"a.dat\":null}"))
      ArchiveAssetFilesTest.error("A08_SCHEMA_UNSUPPORTED", () -> read(json));
  }

  @Test
  public void unsafeEntryAndDisplayPathsAreSeparate() throws Exception {
    ArchiveAssetFilesTest.error("A02_INVALID_ZIP", () -> read("{\"../bad.dat\":\"fake\"}"));
    Map<String, String> names = read("{\"a.dat\":\"../evil.pdf\"}");
    assertEquals("../evil.pdf", names.get("a.dat"));
    assertFalse(ArchiveAssetFiles.displayName(names.get("a.dat")).contains(".."));
  }

  @Test
  public void entryDiscoveryNoMapAndAmbiguousMaps() throws Exception {
    for (int variant = 0; variant < 3; variant++) {
      Map<String, byte[]> entries = new LinkedHashMap<>();
      entries.put("unrelated", new byte[] {1});
      if (variant > 0)
        entries.put(
            "conversation_asset_file_names.json",
            "{\"fake.dat\":\"fictional.png\"}".getBytes(StandardCharsets.UTF_8));
      if (variant == 2)
        entries.put(
            "nested/conversation_asset_file_names.json", "{}".getBytes(StandardCharsets.UTF_8));
      File zip = File.createTempFile("fictional-map-", ".zip");
      try {
        Files.write(zip.toPath(), ArchiveAssetFilesTest.zip(entries));
        if (variant == 2)
          ArchiveAssetFilesTest.error(
              "A08_SCHEMA_UNSUPPORTED",
              () -> ArchiveAssetMap.readZip(zip, new ArchiveImporter.Control()));
        else
          assertEquals(variant, ArchiveAssetMap.readZip(zip, new ArchiveImporter.Control()).size());
      } finally {
        zip.delete();
      }
    }
  }

  @Test
  public void stringCountDepthAndInvalidUtf8Bounded() throws Exception {
    ArchiveAssetFilesTest.error(
        "A05_ARCHIVE_TOO_LARGE", () -> read("{\"a.dat\":\"" + "x".repeat(4097) + "\"}"));
    Map<String, String> data = new LinkedHashMap<>();
    for (int i = 0; i < 2049; i++) data.put("fake" + i + ".dat", "fake.png");
    ArchiveAssetFilesTest.error(
        "A05_ARCHIVE_TOO_LARGE", () -> read(ArchiveModel.JSON.toJson(data)));
    byte[] bad = {(byte) 0xff};
    ArchiveAssetFilesTest.error(
        "A04_JSON_PARSE_FAILED",
        () ->
            ArchiveAssetMap.read(
                new ByteArrayInputStream(bad), null, new ArchiveImporter.Control()));
  }

  @Test
  public void mapCrcAndCancellationChecked() throws Exception {
    byte[] data = "{}".getBytes(StandardCharsets.UTF_8);
    ZipEntry e = new ZipEntry("conversation_asset_file_names.json");
    e.setSize(data.length);
    e.setCrc(0);
    ArchiveAssetFilesTest.error(
        "A02_INVALID_ZIP",
        () ->
            ArchiveAssetMap.read(new ByteArrayInputStream(data), e, new ArchiveImporter.Control()));
    ArchiveImporter.Control control = new ArchiveImporter.Control();
    control.cancelled.set(true);
    ArchiveAssetFilesTest.error(
        "A07_IMPORT_CANCELLED",
        () -> ArchiveAssetMap.read(new ByteArrayInputStream(data), null, control));
  }

  @Test
  public void lexicalStringAndByteBudgetCheckedBeforeGsonAllocation() throws Exception {
    ArchiveAssetFilesTest.error(
        "A05_ARCHIVE_TOO_LARGE", () -> read("{\"a.dat\":\"" + "x".repeat(32769) + "\"}"));
    InputStream huge =
        new InputStream() {
          long count;

          public int read() {
            return count++ < ArchiveAssetMap.BYTE_LIMIT + 1 ? ' ' : -1;
          }

          public int read(byte[] b, int off, int length) {
            int n = (int) Math.min(length, ArchiveAssetMap.BYTE_LIMIT + 1 - count);
            if (n <= 0) return -1;
            Arrays.fill(b, off, off + n, (byte) ' ');
            count += n;
            return n;
          }
        };
    ArchiveAssetFilesTest.error(
        "A05_ARCHIVE_TOO_LARGE",
        () -> ArchiveAssetMap.read(huge, null, new ArchiveImporter.Control()));
  }
}
