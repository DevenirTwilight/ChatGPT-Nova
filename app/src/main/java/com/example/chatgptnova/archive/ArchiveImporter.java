package com.example.chatgptnova.archive;

import com.google.gson.Strictness;
import com.google.gson.stream.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.*;

/** Bounded streaming ZIP/JSON parser. Never extracts a ZIP path or logs source data. */
public final class ArchiveImporter {
  public static final long ARCHIVE_CONTAINER_LIMIT = 512L * 1024 * 1024,
      ENTRY_LIMIT = 64L * 1024 * 1024,
      TOTAL_LIMIT = 256L * 1024 * 1024;

  // UTF-16 char budgets for one bounded object; independent of ZIP/entry byte limits.
  public static final int CONVERSATION_CHAR_LIMIT = 4 * 1024 * 1024;
  public static final int SERIALIZED_CONVERSATION_CHAR_LIMIT = 4 * 1024 * 1024;

  public static void checkContainerSize(long size) throws ArchiveError {
    if (size > ARCHIVE_CONTAINER_LIMIT) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
  }

  static void checkParsedBytes(long entryBytes, long selectedBytes) throws ArchiveError {
    if (entryBytes > ENTRY_LIMIT || selectedBytes > TOTAL_LIMIT)
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
  }

  public interface Sink {
    void accept(ArchiveModel.Conversation c) throws ArchiveError;
  }

  public static final class Control {
    public final AtomicBoolean cancelled = new AtomicBoolean();
    private final long started;

    public Control() {
      this(System.nanoTime());
    }

    Control(long started) {
      this.started = started;
    }

    public void check() throws ArchiveError {
      if (cancelled.get() || Thread.currentThread().isInterrupted())
        throw new ArchiveError("A07_IMPORT_CANCELLED");
      if (System.nanoTime() - started > 300_000_000_000L)
        throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    }
  }

  private final Control control;
  private long total;
  private int conversations, nodes;

  public ArchiveImporter(Control control) {
    this.control = control;
  }

  public int read(File file, boolean zip, Sink sink) throws ArchiveError {
    checkContainerSize(file.length());
    try {
      if (zip) readZip(file, sink);
      else
        try (InputStream in = new FileInputStream(file)) {
          parse(in, sink, null);
        }
      if (conversations == 0) throw new ArchiveError("A03_NO_CONVERSATIONS_DATA");
      return conversations;
    } catch (ArchiveError e) {
      throw e;
    } catch (ControlIOException e) {
      throw new ArchiveError(e.code);
    } catch (LimitIOException e) {
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    } catch (ZipException e) {
      throw new ArchiveError("A02_INVALID_ZIP");
    } catch (IOException | IllegalStateException | NumberFormatException e) {
      throw new ArchiveError(zip ? "A04_JSON_PARSE_FAILED" : "A04_JSON_PARSE_FAILED");
    }
  }

  static String safeName(String name) throws ArchiveError {
    if (name.length() > 1024
        || name.chars().anyMatch(c -> Character.isISOControl(c))
        || name.indexOf('\\') >= 0
        || name.startsWith("/")
        || name.matches("^[A-Za-z]:.*")) throw new ArchiveError("A02_INVALID_ZIP");
    StringBuilder out = new StringBuilder();
    for (String p : name.split("/")) {
      if (p.equals("..")) throw new ArchiveError("A02_INVALID_ZIP");
      if (p.isEmpty() || p.equals(".")) continue;
      if (out.length() > 0) out.append('/');
      out.append(p);
    }
    return out.toString();
  }

  private void readZip(File file, Sink sink) throws IOException, ArchiveError {
    ArchiveZip.inspect(file, control, false);
    try (ZipFile zip = new ZipFile(file)) {
      Enumeration<? extends ZipEntry> entries = zip.entries();
      Set<String> names = new HashSet<>();
      List<ZipEntry> selected = new ArrayList<>();
      long declared = 0;
      int count = 0;
      while (entries.hasMoreElements()) {
        control.check();
        ZipEntry e = entries.nextElement();
        if (++count > 10000) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
        String name = safeName(e.getName());
        if (!names.add(name)) throw new ArchiveError("A02_INVALID_ZIP");
        long size = e.getSize(), compressed = e.getCompressedSize();
        if (size < 0 || compressed < 0) throw new ArchiveError("A02_INVALID_ZIP");
        declared += size;
        if (declared > 512L * 1024 * 1024) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
        if (size > 1024 * 1024 && size > Math.max(1, compressed) * 200)
          throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
        String leaf = name.substring(name.lastIndexOf('/') + 1);
        if (!e.isDirectory() && leaf.matches("(?i)conversations(?:[-_]?\\d+)?\\.json")) {
          if (size > ENTRY_LIMIT) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
          selected.add(e);
        }
      }
      selected.sort(Comparator.comparing(ZipEntry::getName));
      for (ZipEntry e : selected)
        try (InputStream in = zip.getInputStream(e)) {
          parse(in, sink, e);
        }
    }
  }

  private void parse(InputStream source, Sink sink, ZipEntry entry)
      throws IOException, ArchiveError {
    BoundedInput in = new BoundedInput(source);
    GuardReader guard =
        new GuardReader(
            new InputStreamReader(
                in,
                StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)));
    try (JsonReader json = new JsonReader(guard)) {
      json.setStrictness(Strictness.STRICT);
      if (json.peek() == JsonToken.BEGIN_ARRAY) array(json, sink);
      else if (json.peek() == JsonToken.BEGIN_OBJECT) {
        json.beginObject();
        Map<String, Object> single = new LinkedHashMap<>();
        boolean wrapper = false;
        Set<String> keys = new HashSet<>();
        Budget budget = new Budget();
        while (json.hasNext()) {
          String key = json.nextName();
          budget.chars += key.length();
          if (budget.chars > CONVERSATION_CHAR_LIMIT || keys.size() > 100000)
            throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
          if (!keys.add(key)) throw new ArchiveError("A04_JSON_PARSE_FAILED");
          if (key.equals("conversations")) {
            if (json.peek() != JsonToken.BEGIN_ARRAY)
              throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
            wrapper = true;
            array(json, sink);
          } else single.put(key, value(json, 0, budget));
        }
        json.endObject();
        if (!wrapper) accept(single, sink);
      } else throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
      if (json.peek() != JsonToken.END_DOCUMENT) throw new ArchiveError("A04_JSON_PARSE_FAILED");
      control.check();
      if (entry != null && (in.count != entry.getSize() || in.crc.getValue() != entry.getCrc()))
        throw new ArchiveError("A02_INVALID_ZIP");
    } catch (LimitIOException e) {
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    } catch (MalformedJsonException | EOFException e) {
      throw new ArchiveError("A04_JSON_PARSE_FAILED");
    }
  }

  private void array(JsonReader json, Sink sink) throws IOException, ArchiveError {
    json.beginArray();
    while (json.hasNext()) {
      control.check();
      Object data = value(json, 0, new Budget());
      if (!(data instanceof Map)) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
      accept(ArchiveModel.object(data), sink);
    }
    json.endArray();
  }

  private void accept(Map<String, Object> data, Sink sink) throws ArchiveError {
    if (++conversations > 10000) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    ArchiveModel.Conversation c = new ArchiveModel.Conversation(data);
    if (ArchiveModel.JSON.toJson(data).length() > SERIALIZED_CONVERSATION_CHAR_LIMIT)
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    nodes += c.nodes.size();
    if (nodes > 200000) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    if (c.header.getBytes(StandardCharsets.UTF_8).length > 1024 * 1024)
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    for (ArchiveModel.Node n : c.nodes.values())
      if (n.raw.getBytes(StandardCharsets.UTF_8).length > 1024 * 1024)
        throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    sink.accept(c);
  }

  private static final class Budget {
    int values, chars;
  }

  private Object value(JsonReader json, int depth, Budget b) throws IOException, ArchiveError {
    control.check();
    if (depth > 64 || ++b.values > 100000 || b.chars > CONVERSATION_CHAR_LIMIT)
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    switch (json.peek()) {
      case BEGIN_OBJECT:
        {
          Map<String, Object> map = new LinkedHashMap<>();
          json.beginObject();
          while (json.hasNext()) {
            String k = json.nextName();
            b.chars += k.length();
            if (map.containsKey(k)) throw new ArchiveError("A04_JSON_PARSE_FAILED");
            map.put(k, value(json, depth + 1, b));
          }
          json.endObject();
          return map;
        }
      case BEGIN_ARRAY:
        {
          List<Object> list = new ArrayList<>();
          json.beginArray();
          while (json.hasNext()) list.add(value(json, depth + 1, b));
          json.endArray();
          return list;
        }
      case STRING:
        {
          String s = json.nextString();
          b.chars += s.length();
          if (b.chars > CONVERSATION_CHAR_LIMIT) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
          return s;
        }
      case NUMBER:
        {
          String s = json.nextString();
          if (s.length() > 128) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
          BigDecimal n = new BigDecimal(s);
          if (Math.abs((long) n.scale()) > 10000) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
          return n;
        }
      case BOOLEAN:
        return json.nextBoolean();
      case NULL:
        json.nextNull();
        return null;
      default:
        throw new ArchiveError("A04_JSON_PARSE_FAILED");
    }
  }

  private final class BoundedInput extends FilterInputStream {
    long count;
    final CRC32 crc = new CRC32();

    BoundedInput(InputStream in) {
      super(in);
    }

    @Override
    public int read() throws IOException {
      byte[] b = new byte[1];
      int n = read(b, 0, 1);
      return n < 0 ? -1 : b[0] & 255;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      try {
        control.check();
      } catch (ArchiveError e) {
        throw new ControlIOException(e.code);
      }
      int n = in.read(b, off, Math.min(len, 32768));
      if (n > 0) {
        count += n;
        total += n;
        crc.update(b, off, n);
        try {
          checkParsedBytes(count, total);
        } catch (ArchiveError e) {
          throw new LimitIOException();
        }
      }
      return n;
    }
  }

  static final class LimitIOException extends IOException {
    private static final long serialVersionUID = 1;
  }

  private static final class ControlIOException extends IOException {
    private static final long serialVersionUID = 1;
    final String code;

    ControlIOException(String code) {
      this.code = code;
    }
  }

  /** Reject oversized lexical strings/nesting BEFORE Gson allocates a complete token. */
  static final class GuardReader extends FilterReader {
    boolean quoted, escape;
    private final int stringLimit;
    int depth, stringLength, tokenLength;

    GuardReader(Reader r) {
      this(r, 512 * 1024);
    }

    GuardReader(Reader r, int stringLimit) {
      super(r);
      this.stringLimit = stringLimit;
    }

    @Override
    public int read(char[] b, int off, int len) throws IOException {
      int n = super.read(b, off, Math.min(len, 4096));
      for (int i = off; i < off + Math.max(0, n); i++) {
        char c = b[i];
        if (quoted) {
          if (++stringLength > stringLimit) throw new LimitIOException();
          if (escape) escape = false;
          else if (c == '\\') escape = true;
          else if (c == '"') {
            quoted = false;
            stringLength = 0;
          }
        } else if (c == '"') {
          quoted = true;
          tokenLength = 0;
        } else if (c == '{' || c == '[') {
          if (++depth > 64) throw new LimitIOException();
          tokenLength = 0;
        } else if (c == '}' || c == ']') {
          depth--;
          tokenLength = 0;
        } else if (c == ',' || c == ':' || Character.isWhitespace(c)) tokenLength = 0;
        else if (++tokenLength > 128) throw new LimitIOException();
      }
      return n;
    }
  }
}
