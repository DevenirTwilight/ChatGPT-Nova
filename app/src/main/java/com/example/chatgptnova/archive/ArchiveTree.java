package com.example.chatgptnova.archive;

import com.example.chatgptnova.archive.ArchiveModel.*;
import java.util.*;

/** No body guessing or JSON object order. Cycles are detected iteratively. */
public final class ArchiveTree {
  public static final class Selection {
    public final List<Node> messages;
    public final List<String> warnings;
    public final String scope;

    Selection(List<Node> m, List<String> w, String s) {
      messages = m;
      warnings = w;
      scope = s;
    }
  }

  public static Selection select(Conversation c, boolean all) {
    List<String> w = new ArrayList<>();
    boolean cycle = false, orphan = false;
    Map<String, Integer> colors = new HashMap<>();
    for (Node n : c.nodes.values()) {
      List<String> path = new ArrayList<>();
      String k = n.key;
      while (!k.isEmpty() && c.nodes.containsKey(k) && !colors.containsKey(k)) {
        colors.put(k, 1);
        path.add(k);
        k = c.nodes.get(k).parent;
      }
      if (!k.isEmpty() && !c.nodes.containsKey(k)) orphan = true;
      else if (!k.isEmpty() && colors.get(k) != null && colors.get(k) == 1) cycle = true;
      for (String p : path) colors.put(p, 2);
    }
    if (cycle) w.add("发现 parent cycle；使用明确标注的安全顺序。");
    if (orphan) w.add("存在缺失 parent 的节点；分支可能不连续。");
    String head = c.currentNode;
    if (!c.nodes.containsKey(head)) {
      if (!head.isEmpty()) w.add("current_node 不存在。");
      Set<String> parents = new HashSet<>();
      for (Node n : c.nodes.values()) parents.add(n.parent);
      List<String> leaves = new ArrayList<>();
      for (String k : c.nodes.keySet()) if (!parents.contains(k)) leaves.add(k);
      head = leaves.size() == 1 ? leaves.get(0) : "";
      if (head.isEmpty() && !all) w.add("无法确定唯一主链；显示全部节点的安全顺序（不是单一分支）。");
    }
    List<Node> result = new ArrayList<>();
    if (!all && !cycle && !head.isEmpty()) {
      Set<String> seen = new HashSet<>();
      String k = head;
      while (c.nodes.containsKey(k) && seen.add(k)) {
        Node n = c.nodes.get(k);
        if (n.hasMessage) result.add(n);
        k = n.parent;
      }
      Collections.reverse(result);
      return new Selection(result, w, "current-branch");
    }
    for (Node n : c.nodes.values()) if (n.hasMessage) result.add(n);
    result.sort(
        Comparator.comparing((Node n) -> n.created, Comparator.nullsLast(Double::compareTo))
            .thenComparing(n -> n.key));
    return new Selection(result, w, "all-nodes-safe-order");
  }
}
