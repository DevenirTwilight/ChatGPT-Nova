package com.example.chatgptnova.archive;

import com.example.chatgptnova.archive.ArchiveModel.*;
import java.util.*;

/** Identity-based upsert shared by SQLite and JVM fixtures; never merges by message text. */
public final class ArchiveMerge {
  public final Conversation merged;
  public final List<Node> writes = new ArrayList<>();
  public int added, changed, skipped;
  public final boolean headerChanged;

  public ArchiveMerge(Conversation old, Conversation next) throws ArchiveError {
    boolean older =
        old != null && old.updated != null && next.updated != null && next.updated < old.updated;
    Map<String, Object> header =
        new LinkedHashMap<>(
            ArchiveModel.object(
                ArchiveModel.JSON.fromJson(older ? old.header : next.header, Map.class)));
    Map<String, Object> mapping = new LinkedHashMap<>();
    if (old != null) for (Node n : old.nodes.values()) mapping.put(n.key, n.data);
    for (Node n : next.nodes.values()) {
      Node prev = old == null ? null : old.nodes.get(n.key);
      if (prev == null) {
        added++;
        writes.add(n);
        mapping.put(n.key, n.data);
      } else if (older || prev.raw.equals(n.raw)) skipped++;
      else {
        changed++;
        writes.add(n);
        mapping.put(n.key, n.data);
      }
    }
    header.put("mapping", mapping);
    merged = new Conversation(header);
    headerChanged = old == null || !old.header.equals(merged.header);
  }
}
