package com.example.chatgptnova.archive;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import com.example.chatgptnova.archive.ArchiveModel.*;
import java.text.Normalizer;
import java.util.*;

/** Private SQLite schema v1. No WebView/session access and no external storage. */
public final class ArchiveStore extends SQLiteOpenHelper {
  public static final Object LOCK = new Object();
  public static final String DATABASE = "nova-archive.db";

  public ArchiveStore(Context context) {
    super(context.getApplicationContext(), DATABASE, null, 1);
    setWriteAheadLoggingEnabled(true);
  }

  @Override
  public void onConfigure(SQLiteDatabase db) {
    db.setForeignKeyConstraintsEnabled(true);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE import_sources (id TEXT PRIMARY KEY, filename TEXT NOT NULL, started INTEGER"
            + " NOT NULL, finished INTEGER, status TEXT NOT NULL, error TEXT, stats TEXT,"
            + " schema_version INTEGER NOT NULL DEFAULT 1)");
    db.execSQL(
        "CREATE TABLE conversations (row_id INTEGER PRIMARY KEY, official_id TEXT UNIQUE, title"
            + " TEXT NOT NULL, search_title TEXT NOT NULL, created REAL, updated REAL, current_node"
            + " TEXT, header TEXT NOT NULL, first_import INTEGER NOT NULL, latest_import INTEGER"
            + " NOT NULL, first_source TEXT NOT NULL REFERENCES import_sources(id), latest_source"
            + " TEXT NOT NULL REFERENCES import_sources(id))");
    db.execSQL(
        "CREATE TABLE messages (conversation INTEGER NOT NULL REFERENCES conversations(row_id) ON"
            + " DELETE CASCADE, node_key TEXT NOT NULL, message_id TEXT, parent TEXT, is_message"
            + " INTEGER NOT NULL, role TEXT, channel TEXT, content_type TEXT, status TEXT, created"
            + " REAL, raw TEXT NOT NULL, first_source TEXT NOT NULL REFERENCES import_sources(id),"
            + " latest_source TEXT NOT NULL REFERENCES import_sources(id), PRIMARY"
            + " KEY(conversation,node_key))");
    db.execSQL("CREATE INDEX message_identity ON messages(conversation,message_id)");
    db.execSQL("CREATE INDEX conversation_dates ON conversations(updated,created)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int old, int next) {
    throw new SQLiteException("A08_SCHEMA_UNSUPPORTED");
  }

  public static final class Stats {
    public int newConversations,
        updatedConversations,
        newMessages,
        updatedMessages,
        skipped,
        parsed,
        failed;
    public long bytes,
        duration,
        copyDurationMs,
        totalDurationMs,
        temporaryBytes,
        freeStorageBefore,
        freeStorageAfter,
        sampledHeapPeakBytes,
        databaseBytes,
        sampledWalPeakBytes;
    public int mappingNodes,
        messageNodes,
        currentBranchMessages,
        displayableCurrentBranchMessages,
        fallbackConversations;
    public final Map<String, Integer> contentTypes = new TreeMap<>();

    private void observe(Conversation c, SQLiteDatabase db) {
      mappingNodes += c.nodes.size();
      messageNodes += c.messageCount();
      for (Node n : c.nodes.values())
        if (n.hasMessage) {
          String type =
              Arrays.asList("text", "multimodal_text", "thoughts", "reasoning_recap")
                      .contains(n.contentType)
                  ? n.contentType
                  : "other";
          contentTypes.put(type, contentTypes.getOrDefault(type, 0) + 1);
        }
      ArchiveTree.Selection selection = ArchiveTree.select(c, false);
      if (selection.scope.equals("current-branch")) {
        currentBranchMessages += selection.messages.size();
        for (Node n : selection.messages) if (n.displayable()) displayableCurrentBranchMessages++;
      } else fallbackConversations++;
      Runtime rt = Runtime.getRuntime();
      sampledHeapPeakBytes = Math.max(sampledHeapPeakBytes, rt.totalMemory() - rt.freeMemory());
      sampledWalPeakBytes =
          Math.max(sampledWalPeakBytes, new java.io.File(db.getPath() + "-wal").length());
    }

    private final Set<Long> newRows = new HashSet<>(), updatedRows = new HashSet<>();

    public String diagnostic() {
      return "schema=1\nnewConversations="
          + newConversations
          + "\nupdatedConversations="
          + updatedConversations
          + "\nnewMessages="
          + newMessages
          + "\nupdatedMessages="
          + updatedMessages
          + "\nskipped="
          + skipped
          + "\nparsed="
          + parsed
          + "\nfailed="
          + failed
          + "\nbytes="
          + bytes
          + "\ndurationMs="
          + duration
          + "\nmappingNodes="
          + mappingNodes
          + "\nmessageNodes="
          + messageNodes
          + "\ncurrentBranchMessages="
          + currentBranchMessages
          + "\ndisplayableCurrentBranchMessages="
          + displayableCurrentBranchMessages
          + "\nfallbackConversations="
          + fallbackConversations
          + "\ncontentTypeCounts="
          + ArchiveModel.JSON.toJson(contentTypes)
          + "\ncopyDurationMs="
          + copyDurationMs
          + "\ntotalDurationMs="
          + totalDurationMs
          + "\ntemporaryBytes="
          + temporaryBytes
          + "\nfreeStorageBefore="
          + freeStorageBefore
          + "\nfreeStorageAfter="
          + freeStorageAfter
          + "\nsampledHeapPeakBytes="
          + sampledHeapPeakBytes
          + "\ndatabaseBytes="
          + databaseBytes
          + "\nsampledWalPeakBytes="
          + sampledWalPeakBytes;
    }
  }

  public Stats importFile(
      java.io.File file,
      boolean zip,
      String filename,
      String importId,
      ArchiveImporter.Control control)
      throws ArchiveError {
    synchronized (LOCK) {
      return importLocked(file, zip, filename, importId, control);
    }
  }

  private Stats importLocked(
      java.io.File file,
      boolean zip,
      String filename,
      String importId,
      ArchiveImporter.Control control)
      throws ArchiveError {
    long start = System.currentTimeMillis();
    Stats stats = new Stats();
    stats.bytes = file.length();
    SQLiteDatabase db;
    try {
      db = getWritableDatabase();
      db.setMaximumSize(512L * 1024 * 1024);
      ContentValues source = new ContentValues();
      source.put("id", importId);
      source.put("filename", filename);
      source.put("started", start);
      source.put("status", "running");
      db.insertOrThrow("import_sources", null, source);
    } catch (RuntimeException e) {
      throw new ArchiveError("A06_DATABASE_WRITE_FAILED");
    }
    ArchiveError failure = null;
    boolean transaction = false;
    try {
      db.beginTransaction();
      transaction = true;
      new ArchiveImporter(control)
          .read(
              file,
              zip,
              c -> {
                control.check();
                ArchiveStorageBudget.checkReserve(
                    new java.io.File(db.getPath()).getParentFile().getUsableSpace());
                upsert(db, c, importId, start, stats);
                stats.parsed++;
                stats.observe(c, db);
              });
      control.check();
      db.setTransactionSuccessful();
    } catch (ArchiveError e) {
      failure = e;
    } catch (SQLiteFullException e) {
      failure = new ArchiveError("A09_STORAGE_FAILED");
    } catch (RuntimeException e) {
      failure = new ArchiveError("A06_DATABASE_WRITE_FAILED");
    } finally {
      if (transaction)
        try {
          db.endTransaction();
        } catch (RuntimeException e) {
          failure = new ArchiveError("A06_DATABASE_WRITE_FAILED");
        }
    }
    stats.sampledWalPeakBytes =
        Math.max(stats.sampledWalPeakBytes, new java.io.File(db.getPath() + "-wal").length());
    stats.duration = System.currentTimeMillis() - start;
    if (failure != null) {
      stats.newConversations =
          stats.updatedConversations = stats.newMessages = stats.updatedMessages = 0;
      stats.failed = 1;
    }
    try {
      ContentValues done = new ContentValues();
      done.put("finished", System.currentTimeMillis());
      done.put("status", failure == null ? "success" : "failed");
      done.put("error", failure == null ? "" : failure.code);
      done.put("stats", stats.diagnostic());
      db.update("import_sources", done, "id=?", new String[] {importId});
    } catch (RuntimeException e) {
      if (failure == null) failure = new ArchiveError("A06_DATABASE_WRITE_FAILED");
    }
    if (failure != null) throw failure;
    return stats;
  }

  private void upsert(SQLiteDatabase db, Conversation next, String source, long time, Stats stats)
      throws ArchiveError {
    long row = 0;
    if (!next.id.isEmpty())
      try (Cursor q =
          db.rawQuery(
              "SELECT row_id FROM conversations WHERE official_id=?", new String[] {next.id})) {
        if (q.moveToFirst()) row = q.getLong(0);
      }
    Conversation old = row == 0 ? null : load(db, row);
    ArchiveMerge plan = new ArchiveMerge(old, next);
    ContentValues cv = new ContentValues();
    Conversation c = plan.merged;
    if (!c.id.isEmpty()) cv.put("official_id", c.id);
    cv.put("title", c.title);
    cv.put("search_title", normalize(c.title));
    putNumber(cv, "created", c.created);
    putNumber(cv, "updated", c.updated);
    cv.put("current_node", c.currentNode);
    cv.put("header", c.header);
    cv.put("latest_import", time);
    cv.put("latest_source", source);
    if (row == 0) {
      cv.put("first_import", time);
      cv.put("first_source", source);
      row = db.insertOrThrow("conversations", null, cv);
      stats.newRows.add(row);
      stats.newConversations = stats.newRows.size();
    } else {
      db.update("conversations", cv, "row_id=?", new String[] {Long.toString(row)});
      if (!stats.newRows.contains(row) && (plan.headerChanged || !plan.writes.isEmpty())) {
        stats.updatedRows.add(row);
        stats.updatedConversations = stats.updatedRows.size();
      }
    }
    long payload = c.header.length();
    for (Node n : c.nodes.values()) payload += n.raw.length();
    if (payload > ArchiveImporter.SERIALIZED_CONVERSATION_CHAR_LIMIT)
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    if (old != null)
      for (Node n : next.nodes.values())
        if (old.nodes.containsKey(n.key)) {
          ContentValues seen = new ContentValues();
          seen.put("latest_source", source);
          db.update(
              "messages",
              seen,
              "conversation=? AND node_key=?",
              new String[] {Long.toString(row), n.key});
        }
    for (Node n : plan.writes) {
      ContentValues v = new ContentValues();
      v.put("conversation", row);
      v.put("node_key", n.key);
      v.put("message_id", n.id);
      v.put("parent", n.parent);
      v.put("is_message", n.hasMessage ? 1 : 0);
      v.put("role", n.role);
      v.put("channel", n.channel);
      v.put("content_type", n.contentType);
      v.put("status", n.status);
      putNumber(v, "created", n.created);
      v.put("raw", n.raw);
      v.put("latest_source", source);
      boolean exists = old != null && old.nodes.containsKey(n.key);
      if (exists)
        db.update(
            "messages",
            v,
            "conversation=? AND node_key=?",
            new String[] {Long.toString(row), n.key});
      else {
        v.put("first_source", source);
        db.insertOrThrow("messages", null, v);
      }
    }
    for (Node n : plan.writes)
      if (n.hasMessage) {
        if (old == null || !old.nodes.containsKey(n.key)) stats.newMessages++;
        else stats.updatedMessages++;
      }
    stats.skipped += plan.skipped;
  }

  private static void putNumber(ContentValues v, String k, Double n) {
    if (n == null) v.putNull(k);
    else v.put(k, n);
  }

  public Conversation load(long row) throws ArchiveError {
    try {
      return load(getReadableDatabase(), row);
    } catch (RuntimeException e) {
      throw new ArchiveError("A06_DATABASE_WRITE_FAILED");
    }
  }

  private Conversation load(SQLiteDatabase db, long row) throws ArchiveError {
    Map<String, Object> header;
    try (Cursor q =
        db.rawQuery(
            "SELECT header FROM conversations WHERE row_id=?", new String[] {Long.toString(row)})) {
      if (!q.moveToFirst()) throw new ArchiveError("A03_NO_CONVERSATIONS_DATA");
      header =
          new LinkedHashMap<>(
              ArchiveModel.object(ArchiveModel.JSON.fromJson(q.getString(0), Map.class)));
    }
    Map<String, Object> mapping = new LinkedHashMap<>();
    long size = ArchiveModel.JSON.toJson(header).length();
    try (Cursor q =
        db.rawQuery(
            "SELECT node_key,raw FROM messages WHERE conversation=? ORDER BY node_key",
            new String[] {Long.toString(row)})) {
      while (q.moveToNext()) {
        String raw = q.getString(1);
        size += raw.length();
        if (size > ArchiveImporter.SERIALIZED_CONVERSATION_CHAR_LIMIT)
          throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
        mapping.put(q.getString(0), ArchiveModel.JSON.fromJson(raw, Map.class));
      }
    }
    header.put("mapping", mapping);
    return new Conversation(header);
  }

  public static String normalize(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
  }

  public static final class Row {
    public long id;
    public String title;
    public Double created, updated;
    public int count;
  }

  public List<Row> list(String search, boolean earliest, int limit) {
    return list(search, earliest, limit, 0);
  }

  public List<Row> list(String search, boolean earliest, int limit, int offset) {
    String pattern =
        "%" + normalize(search).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    String order =
        earliest
            ? "COALESCE(c.created,c.first_import/1000.0) ASC,c.row_id ASC"
            : "COALESCE(c.updated,c.created,c.latest_import/1000.0) DESC,c.row_id DESC";
    List<Row> rows = new ArrayList<>();
    try (Cursor q =
        getReadableDatabase()
            .rawQuery(
                "SELECT c.row_id,CASE WHEN length(c.title)>512 THEN substr(c.title,1,512)||'…' ELSE"
                    + " c.title END,c.created,c.updated,(SELECT COUNT(*) FROM messages m WHERE"
                    + " m.conversation=c.row_id AND m.is_message=1) FROM conversations c WHERE"
                    + " c.search_title LIKE ? ESCAPE '\\' ORDER BY "
                    + order
                    + " LIMIT ? OFFSET ?",
                new String[] {
                  pattern,
                  Integer.toString(Math.max(1, Math.min(limit, 100000))),
                  Integer.toString(Math.max(0, offset))
                })) {
      while (q.moveToNext()) {
        Row r = new Row();
        r.id = q.getLong(0);
        r.title = q.getString(1);
        r.created = q.isNull(2) ? null : q.getDouble(2);
        r.updated = q.isNull(3) ? null : q.getDouble(3);
        r.count = q.getInt(4);
        rows.add(r);
      }
    }
    return rows;
  }
}
