package com.example.chatgptnova.archive;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Explicit private local run. All documents stay outside Git; stdout contains aggregate counts. */
public final class ArchiveBatchLocal {
  public static void main(String[] args) throws Exception {
    if (args.length != 3)
      throw new IllegalArgumentException("input ZIP, output ZIP, private staging directory");
    File input = new File(args[0]), output = new File(args[1]), staging = new File(args[2]);
    ArchiveImporter.Control control = new ArchiveImporter.Control();
    Set<String> ids = new HashSet<>(), needed = new HashSet<>();
    new ArchiveImporter(control)
        .read(
            input,
            true,
            c -> {
              ids.add(c.id);
              for (ArchiveModel.Node n : c.nodes.values())
                for (ArchiveDisplay.Ref r : ArchiveDisplay.references(n))
                  if (r.recognized()) needed.add(r.entry);
            });
    Map<String, List<ArchiveResearch.Report>> reports = new HashMap<>();
    ArchiveResearch.readZip(
        input,
        ids,
        control,
        r -> reports.computeIfAbsent(r.conversation, k -> new ArrayList<>()).add(r));
    Map<String, String> names = ArchiveAssetMap.readZip(input, control), entries = new HashMap<>();
    try (ZipFile zip = new ZipFile(input)) {
      for (ZipEntry e : Collections.list(zip.entries())) {
        String leaf = e.getName().substring(e.getName().lastIndexOf('/') + 1);
        if (needed.contains(leaf) && !e.isDirectory()) {
          if (entries.put(leaf, e.getName()) != null) throw new ArchiveError("A02_INVALID_ZIP");
        }
      }
    }
    if (!staging.isDirectory() && !staging.mkdirs()) throw new IOException();
    Map<String, ArchiveAssetFiles.Result> copied = new HashMap<>();
    try (ArchiveAssetFiles.Batch files =
            new ArchiveAssetFiles.Batch(input, staging, entries.values(), control);
        ArchiveBatch batch = new ArchiveBatch(new FileOutputStream(output), control)) {
      for (Map.Entry<String, String> e : entries.entrySet())
        copied.put(e.getKey(), files.copy(e.getValue()));
      new ArchiveImporter(control)
          .read(
              input,
              true,
              c -> {
                c.reports.addAll(reports.getOrDefault(c.id, Collections.emptyList()));
                Map<String, ArchiveAsset> assets = new HashMap<>();
                for (ArchiveModel.Node n : c.nodes.values())
                  for (ArchiveDisplay.Ref ref : ArchiveDisplay.references(n)) {
                    ArchiveAssetFiles.Result r = copied.get(ref.entry);
                    if (r != null)
                      try {
                        assets.put(
                            ref.identity,
                            new ArchiveAsset(
                                names.getOrDefault(ref.identity, ref.displayName),
                                r.mime,
                                r.state,
                                r.bytes,
                                r.width,
                                r.height,
                                r.state.equals("complete")
                                    ? ArchiveAssetFiles.resolve(
                                        new File(staging, "nova-archive-assets"), r.relative)
                                    : null));
                      } catch (IOException e) {
                        throw new ArchiveError("A09_STORAGE_FAILED");
                      }
                  }
                try {
                  batch.add(c, false, assets);
                } catch (IOException e) {
                  throw new ArchiveError("A11_EXPORT_FAILED");
                }
              });
      batch.finish();
      System.out.println(
          "conversations="
              + ids.size()
              + " succeeded="
              + batch.succeeded
              + " failed="
              + batch.failed);
      System.out.println("reports=" + reports.values().stream().mapToInt(List::size).sum());
    }
    ArchiveBatch.verify(output, control);
    System.out.println("zipReadbackSha256AndCrc=passed");
  }
}
