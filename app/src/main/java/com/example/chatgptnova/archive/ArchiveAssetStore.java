package com.example.chatgptnova.archive;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Filesystem + normalized SQLite adapter. All mutation is under ArchiveStore.LOCK. */
final class ArchiveAssetStore implements AutoCloseable {
  private final SQLiteDatabase db;
  private final File filesDir;
  private final ArchiveImporter.Control control;
  private final ArchiveStore.Stats stats;
  private final String source;
  private final long time;
  private final Map<String, String> names;
  private final Map<String, String> entries = new HashMap<>();
  private final Map<String, Long> crcs = new HashMap<>();
  private final Map<String, Row> prepared = new HashMap<>(), written = new HashMap<>();
  private ArchiveAssetFiles.Batch batch;
  private boolean committed;

  static void create(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE assets (row_id INTEGER PRIMARY KEY, official_id TEXT NOT NULL UNIQUE,"
            + " original_name TEXT NOT NULL, display_name TEXT NOT NULL, detected_mime TEXT NOT"
            + " NULL, declared_mime TEXT NOT NULL, bytes INTEGER NOT NULL, sha256 TEXT NOT NULL,"
            + " relative_path TEXT NOT NULL, state TEXT NOT NULL, width INTEGER NOT NULL, height"
            + " INTEGER NOT NULL, zip_crc INTEGER, first_import INTEGER NOT NULL, latest_import"
            + " INTEGER NOT NULL, first_source TEXT NOT NULL REFERENCES import_sources(id),"
            + " latest_source TEXT NOT NULL REFERENCES import_sources(id))");
    db.execSQL(
        "CREATE TABLE message_assets (conversation INTEGER NOT NULL, node_key TEXT NOT NULL,"
            + " ordinal INTEGER NOT NULL, asset INTEGER NOT NULL REFERENCES assets(row_id), kind"
            + " TEXT NOT NULL, raw TEXT NOT NULL, PRIMARY KEY(conversation,node_key,ordinal),"
            + " FOREIGN KEY(conversation,node_key) REFERENCES messages(conversation,node_key) ON"
            + " DELETE CASCADE)");
    db.execSQL("CREATE INDEX message_asset_lookup ON message_assets(asset)");
  }

  static void recover(SQLiteDatabase db, File filesDir) throws IOException {
    Set<String> paths = new HashSet<>();
    try (Cursor q = db.rawQuery("SELECT relative_path FROM assets WHERE state='complete'", null)) {
      while (q.moveToNext()) paths.add(q.getString(0));
    }
    ArchiveAssetFiles.recover(filesDir, paths);
  }

  static final class Row {
    long id, bytes, crc = -1;
    String identity, originalName, displayName, mime, declaredMime, hash, path, state;
    int width, height;
  }

  private Row existing(String identity) {
    try (Cursor q =
        db.rawQuery(
            "SELECT"
                + " row_id,original_name,display_name,detected_mime,declared_mime,bytes,sha256,relative_path,state,width,height,zip_crc"
                + " FROM assets WHERE official_id=?",
            new String[] {identity})) {
      if (!q.moveToFirst()) return null;
      Row r = new Row();
      r.identity = identity;
      r.id = q.getLong(0);
      r.originalName = q.getString(1);
      r.displayName = q.getString(2);
      r.mime = q.getString(3);
      r.declaredMime = q.getString(4);
      r.bytes = q.getLong(5);
      r.hash = q.getString(6);
      r.path = q.getString(7);
      r.state = q.getString(8);
      r.width = q.getInt(9);
      r.height = q.getInt(10);
      if (!q.isNull(11)) r.crc = q.getLong(11);
      return r;
    }
  }

  ArchiveAssetStore(
      SQLiteDatabase db,
      File filesDir,
      File input,
      boolean zip,
      String source,
      long time,
      ArchiveImporter.Control control,
      ArchiveStore.Stats stats)
      throws ArchiveError, IOException {
    this.db = db;
    this.filesDir = filesDir;
    this.control = control;
    this.stats = stats;
    this.source = source;
    this.time = time;
    names = zip ? ArchiveAssetMap.readZip(input, control) : Collections.emptyMap();
    Set<String> needed = new HashSet<>();
    new ArchiveImporter(control)
        .read(
            input,
            zip,
            c -> {
              Set<String> perConversation = new HashSet<>();
              for (ArchiveModel.Node node : c.nodes.values())
                for (ArchiveDisplay.Ref ref : ArchiveDisplay.references(node)) {
                  perConversation.add(ref.identity);
                  if (ref.recognized()) needed.add(ref.entry);
                }
              if (perConversation.size() > ArchiveAssetFiles.PER_CONVERSATION_LIMIT
                  || needed.size() > ArchiveAssetFiles.COUNT_LIMIT)
                throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
            });
    Set<String> toCopy = new HashSet<>();
    if (zip)
      try (ZipFile container = new ZipFile(input)) {
        Enumeration<? extends ZipEntry> all = container.entries();
        while (all.hasMoreElements()) {
          control.check();
          ZipEntry e = all.nextElement();
          String leaf = e.getName().substring(e.getName().lastIndexOf('/') + 1);
          if (!e.isDirectory() && needed.contains(leaf)) {
            if (entries.put(leaf, e.getName()) != null) throw new ArchiveError("A02_INVALID_ZIP");
            crcs.put(leaf, e.getCrc());
          }
        }
        for (String key : needed) {
          String identity = key.substring(0, key.length() - 4);
          Row old = existing(identity);
          String entry = entries.get(key);
          if (old != null
              && intact(old)
              && (entry == null
                  || (old.crc == container.getEntry(entry).getCrc()
                      && old.bytes == container.getEntry(entry).getSize()
                      && sourceHashMatches(container, container.getEntry(entry), old.hash))))
            prepared.put(identity, old);
          else if (entry != null) toCopy.add(entry);
        }
      }
    if (zip && !toCopy.isEmpty())
      batch = new ArchiveAssetFiles.Batch(input, filesDir, toCopy, control);
  }

  private boolean sourceHashMatches(ZipFile zip, ZipEntry entry, String expected)
      throws IOException, ArchiveError {
    MessageDigest hash;
    try {
      hash = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
    CRC32 crc = new CRC32();
    long count = 0;
    try (InputStream in = zip.getInputStream(entry)) {
      byte[] buffer = new byte[32768];
      int n;
      while ((n = in.read(buffer)) != -1) {
        control.check();
        count += n;
        if (count > ArchiveAssetFiles.SINGLE_LIMIT) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
        hash.update(buffer, 0, n);
        crc.update(buffer, 0, n);
      }
    } catch (ZipException e) {
      return false;
    }
    if (count != entry.getSize() || crc.getValue() != entry.getCrc()) return false;
    StringBuilder digest = new StringBuilder();
    for (byte b : hash.digest()) digest.append(String.format(Locale.ROOT, "%02x", b & 255));
    return digest.toString().equals(expected);
  }

  private boolean intact(Row r) throws IOException, ArchiveError {
    if (!r.state.equals("complete")) return false;
    File f = ArchiveAssetFiles.resolve(new File(filesDir, "nova-archive-assets"), r.path);
    if (!f.isFile() || f.length() != r.bytes) return false;
    MessageDigest hash;
    try {
      hash = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
    try (InputStream in = new FileInputStream(f)) {
      byte[] b = new byte[32768];
      int n;
      while ((n = in.read(b)) != -1) {
        control.check();
        hash.update(b, 0, n);
      }
    }
    StringBuilder out = new StringBuilder();
    for (byte b : hash.digest()) out.append(String.format(Locale.ROOT, "%02x", b & 255));
    return out.toString().equals(r.hash);
  }

  void attach(long conversation, Collection<ArchiveModel.Node> nodes)
      throws ArchiveError, IOException {
    for (ArchiveModel.Node node : nodes) {
      db.delete(
          "message_assets",
          "conversation=? AND node_key=?",
          new String[] {Long.toString(conversation), node.key});
      for (ArchiveDisplay.Ref ref : ArchiveDisplay.references(node)) {
        Row row = asset(ref);
        ContentValues link = new ContentValues();
        link.put("conversation", conversation);
        link.put("node_key", node.key);
        link.put("ordinal", ref.ordinal);
        link.put("asset", row.id);
        link.put("kind", ref.inline ? "inline" : "attachment");
        link.put("raw", ref.raw);
        db.insertOrThrow("message_assets", null, link);
        stats.assetReferences++;
      }
    }
  }

  private Row asset(ArchiveDisplay.Ref ref) throws ArchiveError, IOException {
    if (written.containsKey(ref.identity)) return written.get(ref.identity);
    Row old = existing(ref.identity), r = prepared.get(ref.identity);
    boolean reused = r != null;
    if (r == null && old != null && intact(old) && !entries.containsKey(ref.entry)) {
      r = old;
      reused = true;
    }
    if (r == null) {
      String entry = entries.get(ref.entry);
      ArchiveAssetFiles.Result result =
          entry != null && batch != null
              ? batch.copy(entry)
              : ArchiveAssetFiles.Result.unavailable("missing");
      // Never replace an existing verified binary with a failed reimport candidate.
      if (!result.state.equals("complete") && old != null && intact(old)) {
        r = old;
        reused = true;
      } else {
        r = new Row();
        r.identity = ref.identity;
        r.id = old == null ? 0 : old.id;
        r.mime = result.mime;
        r.hash = result.sha256;
        r.path = result.relative;
        r.bytes = result.bytes;
        r.state = result.state;
        r.width = result.width;
        r.height = result.height;
        if (result.state.equals("complete") && result.mime.startsWith("image/")) {
          android.graphics.BitmapFactory.Options options =
              new android.graphics.BitmapFactory.Options();
          options.inJustDecodeBounds = true;
          android.graphics.BitmapFactory.decodeFile(
              ArchiveAssetFiles.resolve(new File(filesDir, "nova-archive-assets"), result.relative)
                  .getPath(),
              options);
          if (options.outWidth <= 0
              || options.outHeight <= 0
              || options.outWidth > ArchiveAssetFiles.DIMENSION_LIMIT
              || options.outHeight > ArchiveAssetFiles.DIMENSION_LIMIT
              || (long) options.outWidth * options.outHeight > ArchiveAssetFiles.PIXEL_LIMIT) {
            r.state = "damaged";
            r.path = "";
            r.bytes = 0;
            r.hash = "";
          }
        }
        r.crc = crcs.getOrDefault(ref.entry, -1L);
        // Native image bounds are a second validation stage after the streaming detector.
        // A failed replacement at either stage must retain the verified previous binary.
        if (!r.state.equals("complete") && old != null && intact(old)) {
          r = old;
          reused = true;
        }
      }
    }
    r.originalName = names.getOrDefault(ref.entry, ref.displayName);
    r.displayName = ArchiveAssetFiles.displayName(r.originalName);
    r.declaredMime = ref.declaredMime;
    if (!r.declaredMime.isEmpty()
        && !r.declaredMime.equalsIgnoreCase(r.mime)
        && r.state.equals("complete")) stats.mimeMismatchCount++;
    ContentValues cv = new ContentValues();
    cv.put("official_id", ref.identity);
    cv.put("original_name", r.originalName);
    cv.put("display_name", r.displayName);
    cv.put("detected_mime", r.mime);
    cv.put("declared_mime", r.declaredMime);
    cv.put("bytes", r.bytes);
    cv.put("sha256", r.hash);
    cv.put("relative_path", r.path);
    cv.put("state", r.state);
    cv.put("width", r.width);
    cv.put("height", r.height);
    cv.put("zip_crc", r.crc);
    cv.put("latest_import", time);
    cv.put("latest_source", source);
    if (r.id == 0) {
      cv.put("first_import", time);
      cv.put("first_source", source);
      r.id = db.insertOrThrow("assets", null, cv);
      stats.newAssets++;
    } else {
      db.update("assets", cv, "row_id=?", new String[] {Long.toString(r.id)});
      if (reused) stats.skippedAssets++;
      else stats.updatedAssets++;
    }
    if (!reused && r.state.equals("complete")) stats.assetBytes += r.bytes;
    if (!r.state.equals("complete")) stats.unavailableAssets++;
    written.put(ref.identity, r);
    return r;
  }

  /**
   * If SQLite endTransaction fails ambiguously, retain files until DB-grounded startup recovery.
   */
  void preserveOnUncertainCommit() {
    if (batch != null) batch.committed();
  }

  void committed() {
    committed = true;
    if (batch != null) batch.committed();
  }

  @Override
  public void close() throws IOException {
    if (batch != null) batch.close();
    if (committed) recover(db, filesDir);
    File[] files = new File(filesDir, "nova-archive-assets").listFiles();
    if (files != null) for (File f : files) if (f.isFile()) stats.assetStorageBytes += f.length();
  }
}
