package com.example.chatgptnova.archive;

import com.google.gson.Strictness;
import com.google.gson.stream.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

/**
 * Verified filename-map shape only: exact exported entry name -> original display name. Does not
 * infer message pointers or turn names into filesystem paths.
 */
public final class ArchiveAssetMap {
  public static final long BYTE_LIMIT = 4L * 1024 * 1024;

  private ArchiveAssetMap() {}

  public static Map<String, String> readZip(File container, ArchiveImporter.Control control)
      throws ArchiveError {
    ArchiveImporter.checkContainerSize(container.length());
    try {
      ArchiveZip.inspect(container, control, false);
      try (ZipFile zip = new ZipFile(container)) {
        ZipEntry selected = null;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
          control.check();
          ZipEntry e = entries.nextElement();
          String name = e.getName();
          if (!e.isDirectory()
              && name.substring(name.lastIndexOf('/') + 1)
                  .equals("conversation_asset_file_names.json")) {
            if (selected != null) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
            selected = e;
          }
        }
        if (selected == null) return Collections.emptyMap();
        if (selected.getSize() > BYTE_LIMIT) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
        try (InputStream in = zip.getInputStream(selected)) {
          return read(in, selected, control);
        }
      }
    } catch (ArchiveError e) {
      throw e;
    } catch (IOException | RuntimeException e) {
      throw new ArchiveError("A02_INVALID_ZIP");
    }
  }

  /** Also usable for a local read-only schema audit; never emits names/identities. */
  public static Map<String, String> read(
      InputStream source, ZipEntry expected, ArchiveImporter.Control control) throws ArchiveError {
    LimitedInput input = new LimitedInput(source, control);
    try (JsonReader json =
        new JsonReader(
            new ArchiveImporter.GuardReader(
                new InputStreamReader(
                    input,
                    StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)),
                32768))) {
      json.setStrictness(Strictness.STRICT);
      if (json.peek() != JsonToken.BEGIN_OBJECT) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
      Map<String, String> names = new LinkedHashMap<>();
      json.beginObject();
      while (json.hasNext()) {
        control.check();
        String key = json.nextName();
        if (key.isEmpty() || key.length() > 1024 || names.size() >= ArchiveAssetFiles.COUNT_LIMIT)
          throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
        if (names.containsKey(key)) throw new ArchiveError("A04_JSON_PARSE_FAILED");
        if (!ArchiveImporter.safeName(key).equals(key)) throw new ArchiveError("A02_INVALID_ZIP");
        if (json.peek() != JsonToken.STRING) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
        String original = json.nextString();
        if (original.length() > 4096) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
        names.put(key, original);
      }
      json.endObject();
      if (json.peek() != JsonToken.END_DOCUMENT) throw new ArchiveError("A04_JSON_PARSE_FAILED");
      control.check();
      if (expected != null
          && (input.count != expected.getSize() || input.crc.getValue() != expected.getCrc()))
        throw new ArchiveError("A02_INVALID_ZIP");
      return Collections.unmodifiableMap(names);
    } catch (ArchiveError e) {
      throw e;
    } catch (ArchiveImporter.LimitIOException e) {
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    } catch (SafetyIOException e) {
      throw new ArchiveError(e.code);
    } catch (IOException | RuntimeException e) {
      throw new ArchiveError("A04_JSON_PARSE_FAILED");
    }
  }

  private static final class SafetyIOException extends IOException {
    private static final long serialVersionUID = 1;
    final String code;

    SafetyIOException(String code) {
      this.code = code;
    }
  }

  private static final class LimitedInput extends FilterInputStream {
    private final ArchiveImporter.Control control;
    long count;
    final CRC32 crc = new CRC32();

    LimitedInput(InputStream in, ArchiveImporter.Control control) {
      super(in);
      this.control = control;
    }

    @Override
    public int read() throws IOException {
      byte[] b = new byte[1];
      return read(b, 0, 1) < 0 ? -1 : b[0] & 255;
    }

    @Override
    public int read(byte[] b, int off, int length) throws IOException {
      try {
        control.check();
      } catch (ArchiveError e) {
        throw new SafetyIOException(e.code);
      }
      int n = in.read(b, off, Math.min(length, 32768));
      if (n > 0) {
        count += n;
        if (count > BYTE_LIMIT) throw new SafetyIOException("A05_ARCHIVE_TOO_LARGE");
        crc.update(b, off, n);
      }
      return n;
    }
  }
}
