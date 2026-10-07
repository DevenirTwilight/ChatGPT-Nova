package com.example.chatgptnova.archive;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Attachment file primitives. Not connected to schema mapping or the UI yet. No source logs. */
public final class ArchiveAssetFiles {
  public static final long SINGLE_LIMIT = 32L * 1024 * 1024;
  public static final long TOTAL_LIMIT = 256L * 1024 * 1024;
  public static final int COUNT_LIMIT = 2048, PER_CONVERSATION_LIMIT = 256;
  public static final long PIXEL_LIMIT = 16_000_000;
  public static final int DIMENSION_LIMIT = 16384;
  public static final String UNKNOWN = "application/octet-stream";
  public static final String DOCX =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  public static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private ArchiveAssetFiles() {}

  /** Display only; never used as a filesystem name or identity. */
  public static String displayName(String original) {
    if (original == null) return "附件";
    String s = original.replaceAll("[\\p{Cntrl}/\\\\:]", "_").replace("..", "_").trim();
    if (s.length() > 160) s = s.substring(0, 160);
    return s.isEmpty() || s.equals(".") ? "附件" : s;
  }

  public static void checkPlan(long available, long projected) throws ArchiveError {
    if (projected < 0 || projected > TOTAL_LIMIT) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    if (available
        < projected + ArchiveStorageBudget.DB_WAL_ALLOWANCE + ArchiveStorageBudget.FIXED_RESERVE)
      throw new ArchiveError("A09_STORAGE_FAILED");
  }

  public static final class Type {
    public final String mime;
    public final int width, height;

    Type(String mime, int width, int height) {
      this.mime = mime;
      this.width = width;
      this.height = height;
    }

    public boolean image() {
      return mime.equals("image/png") || mime.equals("image/jpeg");
    }
  }

  /** Header/magic inspection, not a guarantee that an external document renderer can open it. */
  public static Type inspect(File file, ArchiveImporter.Control control)
      throws IOException, ArchiveError {
    if (file.length() == 0) throw new ArchiveError("A13_ASSET_DAMAGED");
    if (file.length() > SINGLE_LIMIT) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
      byte[] prefix = new byte[24];
      int length = in.read(prefix);
      if (starts(prefix, length, new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10})) {
        if (length < 24
            || be32(prefix, 8) != 13
            || !new String(prefix, 12, 4, StandardCharsets.US_ASCII).equals("IHDR"))
          throw new ArchiveError("A13_ASSET_DAMAGED");
        int width = be32(prefix, 16), height = be32(prefix, 20);
        dimensions(width, height);
        return new Type("image/png", width, height);
      }
      if (starts(prefix, length, new byte[] {(byte) 255, (byte) 216, (byte) 255}))
        return jpeg(file, control);
      if (starts(prefix, length, "%PDF-".getBytes(StandardCharsets.US_ASCII)))
        return new Type("application/pdf", 0, 0);
      if (starts(prefix, length, new byte[] {80, 75, 3, 4})) return office(file, control);
      return new Type(UNKNOWN, 0, 0);
    }
  }

  private static Type jpeg(File file, ArchiveImporter.Control control)
      throws IOException, ArchiveError {
    // Only bounded marker headers, no pixel allocation. Android must still validate actual decode.
    try (RandomAccessFile in = new RandomAccessFile(file, "r")) {
      in.seek(2);
      for (int markers = 0; markers < 4096; markers++) {
        control.check();
        if (in.getFilePointer() > 1024 * 1024 || in.readUnsignedByte() != 255) break;
        int marker;
        do {
          marker = in.readUnsignedByte();
        } while (marker == 255);
        if (marker == 0 || marker == 0xd9 || marker == 0xda) break;
        if (marker == 0x01 || (marker >= 0xd0 && marker <= 0xd7)) continue;
        int size = in.readUnsignedShort();
        if (size < 2 || in.getFilePointer() + size - 2 > in.length()) break;
        boolean sof =
            marker >= 0xc0 && marker <= 0xcf && marker != 0xc4 && marker != 0xc8 && marker != 0xcc;
        if (sof) {
          if (size < 8) break;
          in.readUnsignedByte();
          int height = in.readUnsignedShort(), width = in.readUnsignedShort();
          dimensions(width, height);
          return new Type("image/jpeg", width, height);
        }
        in.seek(in.getFilePointer() + size - 2);
      }
    } catch (EOFException e) {
      throw new ArchiveError("A13_ASSET_DAMAGED");
    }
    throw new ArchiveError("A13_ASSET_DAMAGED");
  }

  private static Type office(File file, ArchiveImporter.Control control)
      throws IOException, ArchiveError {
    ArchiveZip.inspect(file, control, true);
    boolean word = false, xl = false, types = false, macros = false;
    try (ZipFile zip = new ZipFile(file)) {
      Enumeration<? extends ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        control.check();
        ZipEntry e = entries.nextElement();
        String name = e.getName();
        if (name.equals("[Content_Types].xml")) {
          if (e.getSize() > 65536) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
          // Read a small required manifest and verify CRC/length, without parsing XML or entities.
          CRC32 crc = new CRC32();
          long bytes = 0;
          try (InputStream in = zip.getInputStream(e)) {
            byte[] b = new byte[4096];
            int n;
            while ((n = in.read(b)) != -1) {
              control.check();
              bytes += n;
              if (bytes > 65536) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
              crc.update(b, 0, n);
            }
          }
          if (bytes != e.getSize() || crc.getValue() != e.getCrc())
            throw new ArchiveError("A13_ASSET_DAMAGED");
          types = bytes > 0;
        }
        word |= name.startsWith("word/");
        xl |= name.startsWith("xl/");
        macros |= name.toLowerCase(Locale.ROOT).endsWith("vbaproject.bin");
      }
    }
    if (types && !macros && word != xl) return new Type(word ? DOCX : XLSX, 0, 0);
    return new Type(UNKNOWN, 0, 0);
  }

  private static int be32(byte[] b, int p) {
    return ((b[p] & 255) << 24)
        | ((b[p + 1] & 255) << 16)
        | ((b[p + 2] & 255) << 8)
        | (b[p + 3] & 255);
  }

  private static boolean starts(byte[] b, int length, byte[] signature) {
    if (length < signature.length) return false;
    for (int i = 0; i < signature.length; i++) if (b[i] != signature[i]) return false;
    return true;
  }

  private static void dimensions(int w, int h) throws ArchiveError {
    if (w <= 0 || h <= 0) throw new ArchiveError("A13_ASSET_DAMAGED");
    if (w > DIMENSION_LIMIT || h > DIMENSION_LIMIT || (long) w * h > PIXEL_LIMIT)
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
  }

  /** Stored relative path is exclusively a generated UUID, never a source identity/name. */
  public static File resolve(File root, String relative) throws IOException {
    if (relative == null || !relative.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.bin"))
      throw new IOException();
    File f = new File(root, relative);
    if (!f.getCanonicalFile().getParentFile().equals(root.getCanonicalFile()))
      throw new IOException();
    return f;
  }

  /**
   * Run under the same exclusive import/delete lock, after reading complete paths from SQLite. No
   * files are removed if the supplied DB reference set cannot be validated.
   */
  public static void recover(File filesDir, Set<String> committedNames) throws IOException {
    File root = new File(filesDir, "nova-archive-assets");
    for (String name : committedNames) resolve(root, name);
    File[] files = root.listFiles();
    if (files != null)
      for (File f : files) {
        if (!f.isDirectory()
            && f.getName().matches("[0-9a-f-]{36}\\.bin")
            && !committedNames.contains(f.getName())
            && !f.delete()) throw new IOException();
      }
    File pending = new File(filesDir, "nova-archive-pending");
    File[] batches = pending.listFiles();
    if (batches != null)
      for (File batch : batches) {
        if (!batch.getName().matches("[0-9a-f-]{36}")) continue;
        if (java.nio.file.Files.isSymbolicLink(batch.toPath())) {
          if (!batch.delete()) throw new IOException();
          continue;
        }
        File[] staged = batch.listFiles();
        if (staged != null)
          for (File f : staged) {
            if (!f.isDirectory() && f.getName().matches("[0-9a-f-]{36}\\.bin") && !f.delete())
              throw new IOException();
          }
        if (!batch.delete()) throw new IOException();
      }
  }

  public static final class Result {
    public final String relative, mime, sha256, state;
    public final long bytes;
    public final int width, height;

    private Result(String relative, Type type, String hash, long bytes, String state) {
      this.relative = relative;
      mime = type.mime;
      sha256 = hash;
      this.bytes = bytes;
      this.state = state;
      width = type.width;
      height = type.height;
    }

    static Result unavailable(String state) {
      return new Result("", new Type(UNKNOWN, 0, 0), "", 0, state);
    }
  }

  /** Caller holds ArchiveStore.LOCK and commits DB only AFTER file publication. */
  public static final class Batch implements AutoCloseable {
    private final File root, pending;
    private final ZipFile zip;
    private final ArchiveImporter.Control control;
    private final List<File> published = new ArrayList<>();
    private final Set<String> allowed;
    private final Map<String, Result> copied = new HashMap<>();
    private long total;
    private boolean committed, closed;

    /** referencedEntries must be established by explicit official schema, never guessed here. */
    @SuppressWarnings(
        "resource") // Ownership transfers to Batch.close, including constructor failure.
    public Batch(
        File container,
        File filesDir,
        Collection<String> referencedEntries,
        ArchiveImporter.Control control)
        throws IOException, ArchiveError {
      this.control = control;
      ArchiveImporter.checkContainerSize(container.length());
      ArchiveZip.inspect(container, control, false);
      allowed = new HashSet<>(referencedEntries);
      if (allowed.size() > COUNT_LIMIT) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
      root = new File(filesDir, "nova-archive-assets");
      File staging = new File(filesDir, "nova-archive-pending");
      if ((!root.isDirectory() && !root.mkdirs()) || (!staging.isDirectory() && !staging.mkdirs()))
        throw new ArchiveError("A09_STORAGE_FAILED");
      ZipFile opened = new ZipFile(container);
      File work = null;
      try {
        long projected = 0;
        for (String name : allowed) {
          control.check();
          if (!ArchiveImporter.safeName(name).equals(name))
            throw new ArchiveError("A02_INVALID_ZIP");
          ZipEntry e = opened.getEntry(name);
          if (e == null || e.isDirectory()) continue;
          if (e.getSize() > SINGLE_LIMIT) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
          projected += e.getSize();
          if (projected > TOTAL_LIMIT) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
        }
        checkPlan(filesDir.getUsableSpace(), projected);
        work = new File(staging, UUID.randomUUID().toString());
        if (!work.mkdir()) throw new ArchiveError("A09_STORAGE_FAILED");
      } catch (ArchiveError | RuntimeException e) {
        opened.close();
        throw e;
      }
      zip = opened;
      pending = work;
    }

    public Result copy(String entry) throws IOException, ArchiveError {
      if (closed || committed || !allowed.contains(entry)) throw new IllegalStateException();
      control.check();
      if (copied.containsKey(entry)) return copied.get(entry);
      ZipEntry e = zip.getEntry(entry);
      Result result;
      if (e == null || e.isDirectory()) result = Result.unavailable("missing");
      else if (e.getSize() == 0) result = Result.unavailable("damaged");
      else {
        String relative = UUID.randomUUID().toString() + ".bin";
        File staged = new File(pending, relative);
        CRC32 crc = new CRC32();
        MessageDigest hash;
        try {
          hash = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
          throw new AssertionError(impossible);
        }
        long count = 0;
        try (InputStream in = zip.getInputStream(e);
            FileOutputStream out = new FileOutputStream(staged)) {
          byte[] buffer = new byte[32768];
          int n;
          while ((n = in.read(buffer)) != -1) {
            control.check();
            count += n;
            total += n;
            if (count > SINGLE_LIMIT || total > TOTAL_LIMIT)
              throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
            ArchiveStorageBudget.checkReserve(root.getUsableSpace());
            crc.update(buffer, 0, n);
            hash.update(buffer, 0, n);
            out.write(buffer, 0, n);
          }
          out.getFD().sync();
        } catch (ZipException e1) {
          staged.delete();
          result = Result.unavailable("damaged");
          copied.put(entry, result);
          return result;
        }
        if (count != e.getSize() || crc.getValue() != e.getCrc()) {
          staged.delete();
          result = Result.unavailable("damaged");
        } else {
          Type type;
          try {
            type = inspect(staged, control);
          } catch (ArchiveError error) {
            if (!error.code.equals("A13_ASSET_DAMAGED") && !error.code.equals("A02_INVALID_ZIP"))
              throw error;
            staged.delete();
            result = Result.unavailable("damaged");
            copied.put(entry, result);
            return result;
          }
          File target = resolve(root, relative);
          if (target.exists() || !staged.renameTo(target))
            throw new ArchiveError("A09_STORAGE_FAILED");
          published.add(target);
          StringBuilder digest = new StringBuilder();
          for (byte b : hash.digest()) digest.append(String.format(Locale.ROOT, "%02x", b & 255));
          result = new Result(relative, type, digest.toString(), count, "complete");
        }
      }
      copied.put(entry, result);
      return result;
    }

    /**
     * Only call after successful DB endTransaction; a later UI/diagnostic failure must not undo it.
     */
    public void committed() {
      if (closed) throw new IllegalStateException();
      committed = true;
    }

    @Override
    public void close() throws IOException {
      if (closed) return;
      closed = true;
      try {
        zip.close();
      } finally {
        if (!committed) for (File f : published) f.delete();
        File[] files = pending.listFiles();
        if (files != null) for (File f : files) f.delete();
        pending.delete();
      }
    }
  }
}
