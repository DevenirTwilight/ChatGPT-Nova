package com.example.chatgptnova.archive;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class ArchiveBinaryAudit {
  public static void main(String[] args) {
    long start = System.nanoTime(), bytes = 0;
    int complete = 0, failed = 0;
    Map<String, Integer> types = new TreeMap<>();
    File root;
    try {
      root = java.nio.file.Files.createTempDirectory("nova-binary-audit-").toFile();
    } catch (IOException e) {
      System.out.println("auditFailure=Storage");
      System.exit(1);
      return;
    }
    try {
      File input = new File(args[0]);
      ArchiveImporter.Control control = new ArchiveImporter.Control();
      Set<String> needed = new HashSet<>();
      new ArchiveImporter(control)
          .read(
              input,
              true,
              c -> {
                for (ArchiveModel.Node n : c.nodes.values())
                  for (ArchiveDisplay.Ref r : ArchiveDisplay.references(n))
                    if (r.recognized()) needed.add(r.entry);
              });
      Set<String> entries = new HashSet<>();
      try (ZipFile z = new ZipFile(input)) {
        Enumeration<? extends ZipEntry> all = z.entries();
        while (all.hasMoreElements()) {
          ZipEntry e = all.nextElement();
          String n = e.getName();
          String leaf = n.substring(n.lastIndexOf('/') + 1);
          if (!e.isDirectory() && needed.contains(leaf)) entries.add(n);
        }
      }
      try (ArchiveAssetFiles.Batch batch =
          new ArchiveAssetFiles.Batch(input, root, entries, control)) {
        for (String entry : entries) {
          ArchiveAssetFiles.Result r = batch.copy(entry);
          if (r.state.equals("complete")) {
            complete++;
            bytes += r.bytes;
            types.merge(r.mime, 1, Integer::sum);
            if (!ArchiveAssetFiles.resolve(new File(root, "nova-archive-assets"), r.relative)
                .isFile()) throw new IOException();
          } else failed++;
        }
      }
      File[] remains = new File(root, "nova-archive-assets").listFiles();
      File[] pending = new File(root, "nova-archive-pending").listFiles();
      System.out.println(
          "candidates="
              + needed.size()
              + " matched="
              + entries.size()
              + " copied="
              + complete
              + " unavailable="
              + failed
              + " bytes="
              + bytes
              + " durationMs="
              + (System.nanoTime() - start) / 1000000
              + " rollbackRemaining="
              + (remains == null ? 0 : remains.length)
              + " pendingRemaining="
              + (pending == null ? 0 : pending.length)
              + " mimeCounts="
              + types);
      new File(root, "nova-archive-assets").delete();
      new File(root, "nova-archive-pending").delete();
      root.delete();
    } catch (ArchiveError e) {
      System.out.println("fixedError=" + e.code);
      System.exit(1);
    } catch (Exception e) {
      System.out.println("auditFailure=" + e.getClass().getSimpleName());
      System.exit(1);
    }
  }
}
