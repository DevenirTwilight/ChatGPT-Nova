package com.example.chatgptnova.archive;

import java.util.*;

/** Display-only report insertion. Never creates or rewrites chat parents/branch membership. */
public final class ArchiveTimeline {
  private ArchiveTimeline() {}

  public static final class Entry {
    public final ArchiveModel.Node chat;
    public final ArchiveResearch.Report report;
    public final String placement;

    private Entry(ArchiveModel.Node chat, ArchiveResearch.Report report, String placement) {
      this.chat = chat;
      this.report = report;
      this.placement = placement;
    }

    public String note() {
      if (placement.equals("time")) return "按时间恢复位置 · 依据报告消息时间插入，原气泡位置未提供。";
      if (placement.equals("report-only")) return "按报告时间排序 · 原气泡位置未提供。";
      return "位置未确定 · 时间缺失或无法与当前聊天顺序对应，正文保留在末尾。";
    }
  }

  public static List<Entry> select(ArchiveModel.Conversation c, ArchiveTree.Selection selection)
      throws ArchiveError {
    if (c.reports.size() > ArchiveResearch.PER_CONVERSATION_LIMIT)
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    List<ArchiveModel.Node> chat = new ArrayList<>();
    for (ArchiveModel.Node node : selection.messages) if (node.displayable()) chat.add(node);
    int n = chat.size();
    double[] before = new double[n + 1], after = new double[n + 1];
    before[0] = Double.NEGATIVE_INFINITY;
    after[n] = Double.POSITIVE_INFINITY;
    boolean comparable = true;
    for (int i = 0; i < n; i++) {
      Double time = chat.get(i).created;
      if (time == null) comparable = false;
      before[i + 1] = Math.max(before[i], time == null ? 0 : time);
    }
    for (int i = n - 1; i >= 0; i--) {
      Double time = chat.get(i).created;
      after[i] = Math.min(after[i + 1], time == null ? 0 : time);
    }
    List<ArchiveResearch.Report> reports = new ArrayList<>(c.reports);
    reports.sort(
        Comparator.comparing(
                (ArchiveResearch.Report r) -> ArchiveModel.number(r.created),
                Comparator.nullsLast(Double::compareTo))
            .thenComparing(r -> r.identity));
    Map<Integer, List<Entry>> slots = new HashMap<>();
    List<Entry> unknown = new ArrayList<>();
    for (ArchiveResearch.Report report : reports) {
      Double time = ArchiveModel.number(report.created);
      int slot = -1;
      if (comparable && time != null)
        for (int i = 0; i <= n; i++)
          if (before[i] <= time && time < after[i]) {
            slot = i;
            break;
          }
      boolean only = selection.scope.equals("research-reports");
      Entry entry =
          new Entry(
              null, report, only && time != null ? "report-only" : slot < 0 ? "unknown" : "time");
      if (slot < 0) unknown.add(entry);
      else slots.computeIfAbsent(slot, k -> new ArrayList<>()).add(entry);
    }
    List<Entry> result = new ArrayList<>(n + reports.size());
    for (int i = 0; i <= n; i++) {
      result.addAll(slots.getOrDefault(i, Collections.emptyList()));
      if (i < n) result.add(new Entry(chat.get(i), null, "chat"));
    }
    result.addAll(unknown);
    return result;
  }
}
