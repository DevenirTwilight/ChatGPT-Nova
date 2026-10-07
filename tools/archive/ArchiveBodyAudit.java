package com.example.chatgptnova.archive;

import java.io.File;
import java.util.*;

/** Explicit local-only audit: emits counts and fixed codes, never source identifiers or text. */
public final class ArchiveBodyAudit {
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("auditFailure=Arguments");
      System.exit(1);
    }
    final long[] counts = new long[9];
    try {
      new ArchiveImporter(new ArchiveImporter.Control()).read(new File(args[0]), true, c -> {
        counts[0]++;
        Set<String> main = new HashSet<>();
        ArchiveTree.Selection selected = ArchiveTree.select(c, false);
        for (ArchiveModel.Node n : selected.messages) main.add(n.key);
        for (ArchiveModel.Node n : c.nodes.values()) {
          counts[1]++;
          if (!n.hasMessage) continue;
          counts[2]++;
          if (!n.displayable()) { counts[3]++; continue; }
          if (!n.role.equals("assistant") || !n.contentType.equals("text")) continue;
          Map<String,Object> content = ArchiveModel.object(
              ArchiveModel.object(n.data.get("message")).get("content"));
          Object parts = content.get("parts");
          if (!(parts instanceof List)) continue;
          StringBuilder original = new StringBuilder();
          boolean stringsOnly = true;
          for (Object part : (List<?>)parts) {
            if (!(part instanceof String)) { stringsOnly = false; break; }
            if (original.length() > 0) original.append('\n');
            original.append((String)part);
          }
          if (!stringsOnly || original.length() <= 4000) continue;
          counts[4]++;
          if (main.contains(n.key)) counts[5]++; else counts[6]++;
          if (!original.toString().equals(n.text())) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
          List<String> display = new ArrayList<>();
          for (ArchiveDisplay.Block b : ArchiveDisplay.visible(n))
            if (b.asset == null) display.add(b.text);
          if (!String.join("\n",display).equals(original.toString()))
            throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
          counts[7]++;
        }
        ArchiveTree.Selection all = ArchiveTree.select(c, true);
        String md = ArchiveRenderer.markdown(c, all, Collections.emptyMap());
        ArchiveRenderer.html(c, all, Collections.emptyMap(), ArchiveRenderer.AssetMode.READER);
        for (ArchiveModel.Node n : all.messages) {
          if (n.displayable() && n.role.equals("assistant") && n.contentType.equals("text")
              && n.text().length() > 4000) {
            if (!md.contains(n.text())) throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
            counts[8]++;
          }
        }
      });
      String[] names = {"conversations","nodes","messages","hiddenNodes","longPlainAssistant",
          "longOnCurrentBranch","longOffCurrentBranch","exactModelDisplayMatches","exactAllBranchMarkdownMatches"};
      for (int i=0;i<names.length;i++) System.out.println(names[i]+"="+counts[i]);
      System.out.println("allBranchHtmlRendering=completed");
      System.out.println("specificResearchReportIdentified=false");
      System.out.println("androidDatabaseOrDeviceValidated=false");
    } catch (ArchiveError e) {
      System.out.println("fixedError="+e.code);
      System.exit(1);
    } catch (Exception e) {
      System.out.println("auditFailure=Unexpected");
      System.exit(1);
    }
  }
}
