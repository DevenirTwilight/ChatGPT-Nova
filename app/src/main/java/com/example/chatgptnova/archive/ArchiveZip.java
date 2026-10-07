package com.example.chatgptnova.archive;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/** Bounded central/local header inspection before ZipFile builds its eager directory index. */
public final class ArchiveZip {
  private ArchiveZip() {}

  public static void inspect(File file, ArchiveImporter.Control control, boolean nested)
      throws IOException, ArchiveError {
    int maxEntries = nested ? 2048 : 10000;
    long maxDirectory = nested ? 2L * 1024 * 1024 : 16L * 1024 * 1024;
    long maxExpanded = nested ? 64L * 1024 * 1024 : 512L * 1024 * 1024;
    try (RandomAccessFile in = new RandomAccessFile(file, "r")) {
      int tailSize = (int) Math.min(65557, in.length());
      if (tailSize < 22) invalid();
      byte[] tail = new byte[tailSize];
      in.seek(in.length() - tailSize);
      in.readFully(tail);
      int end = -1;
      for (int i = tail.length - 22; i >= 0; i--)
        if (u32(tail, i) == 0x06054b50L && i + 22 + u16(tail, i + 20) == tail.length) {
          end = i;
          break;
        }
      if (end < 0
          || u16(tail, end + 4) != 0
          || u16(tail, end + 6) != 0
          || u16(tail, end + 8) != u16(tail, end + 10)) invalid();
      int count = u16(tail, end + 10);
      long size = u32(tail, end + 12), offset = u32(tail, end + 16);
      if (count > maxEntries || size > maxDirectory || offset == 0xffffffffL) tooLarge();
      long directoryEnd = offset + size, actualEnd = in.length() - tailSize + end;
      if (directoryEnd != actualEnd || offset < 0) invalid();
      in.seek(offset);
      byte[] header = new byte[46], local = new byte[30];
      Set<String> names = new HashSet<>();
      List<long[]> ranges = new ArrayList<>();
      int records = 0;
      long expanded = 0;
      while (in.getFilePointer() < directoryEnd) {
        control.check();
        if (++records > maxEntries) tooLarge();
        if (directoryEnd - in.getFilePointer() < 46) invalid();
        in.readFully(header);
        if (u32(header, 0) != 0x02014b50L) invalid();
        int flags = u16(header, 8), method = u16(header, 10);
        // Only stored/deflate, optional deflate hints, UTF8 and data descriptor. No encryption.
        if ((flags & ~(0x800 | 8 | 6)) != 0 || (method != 0 && method != 8)) invalid();
        if (method == 0 && (flags & 6) != 0) invalid();
        if (u16(header, 34) != 0 || u16(header, 6) > 20) invalid();
        int unixMode = (int) (u32(header, 38) >>> 16) & 0170000;
        if (unixMode != 0 && unixMode != 0100000 && unixMode != 0040000) invalid();
        long compressed = u32(header, 20), bytes = u32(header, 24), position = u32(header, 42);
        if (compressed == 0xffffffffL || bytes == 0xffffffffL || position == 0xffffffffL)
          tooLarge();
        if (method == 0 && compressed != bytes) invalid();
        expanded += bytes;
        if (expanded > maxExpanded
            || (bytes > 1024 * 1024 && bytes > Math.max(1, compressed) * 200)) tooLarge();
        int nameSize = u16(header, 28), extra = u16(header, 30), comment = u16(header, 32);
        if (nameSize == 0 || nameSize > 4096) invalid();
        long next = in.getFilePointer() + nameSize + extra + comment;
        if (next > directoryEnd) invalid();
        byte[] nameBytes = new byte[nameSize];
        in.readFully(nameBytes);
        checkExtra(in, extra);
        String name;
        try {
          name =
              StandardCharsets.UTF_8
                  .newDecoder()
                  .onMalformedInput(CodingErrorAction.REPORT)
                  .onUnmappableCharacter(CodingErrorAction.REPORT)
                  .decode(ByteBuffer.wrap(nameBytes))
                  .toString();
        } catch (CharacterCodingException e) {
          throw new ArchiveError("A02_INVALID_ZIP");
        }
        if (!names.add(ArchiveImporter.safeName(name))) invalid();
        if (position + 30 > offset) invalid();
        in.seek(position);
        in.readFully(local);
        if (u32(local, 0) != 0x04034b50L
            || u16(local, 6) != flags
            || u16(local, 8) != method
            || u16(local, 26) != nameSize) invalid();
        byte[] localName = new byte[nameSize];
        in.readFully(localName);
        if (!Arrays.equals(nameBytes, localName)) invalid();
        checkExtra(in, u16(local, 28));
        long body = position + 30 + nameSize + u16(local, 28), bodyEnd = body + compressed;
        if (bodyEnd > offset) invalid();
        if ((flags & 8) == 0
            && (u32(local, 14) != u32(header, 16)
                || u32(local, 18) != compressed
                || u32(local, 22) != bytes)) invalid();
        ranges.add(new long[] {position, bodyEnd});
        in.seek(next);
      }
      if (records != count) invalid();
      ranges.sort(Comparator.comparingLong(r -> r[0]));
      long last = 0;
      for (long[] r : ranges) {
        if (r[0] < last) invalid();
        last = r[1];
      }
      // Deliberately retain the existing no-SFX contract; an empty ZIP has offset zero.
      if (!ranges.isEmpty() && ranges.get(0)[0] != 0) invalid();
    } catch (EOFException e) {
      throw new ArchiveError("A02_INVALID_ZIP");
    }
  }

  private static void checkExtra(RandomAccessFile in, int size) throws IOException, ArchiveError {
    long end = in.getFilePointer() + size;
    while (in.getFilePointer() < end) {
      if (end - in.getFilePointer() < 4) invalid();
      byte[] h = new byte[4];
      in.readFully(h);
      int id = u16(h, 0), length = u16(h, 2);
      if (in.getFilePointer() + length > end || id == 1) invalid();
      in.seek(in.getFilePointer() + length);
    }
  }

  static int u16(byte[] b, int p) {
    return (b[p] & 255) | ((b[p + 1] & 255) << 8);
  }

  static long u32(byte[] b, int p) {
    return (long) u16(b, p) | ((long) u16(b, p + 2) << 16);
  }

  private static void invalid() throws ArchiveError {
    throw new ArchiveError("A02_INVALID_ZIP");
  }

  private static void tooLarge() throws ArchiveError {
    throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
  }
}
