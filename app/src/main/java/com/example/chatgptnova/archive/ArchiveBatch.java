package com.example.chatgptnova.archive;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Sequential local exports. No account access; each conversation owns its own directory. */
public final class ArchiveBatch implements AutoCloseable {
  public static final int MAX_CONVERSATIONS = 10000;
  private static final long MAX_BYTES = 512L * 1024 * 1024;
  private final ZipOutputStream zip;
  private final ArchiveImporter.Control control;
  private final List<Map<String, Object>> results = new ArrayList<>();
  private final Map<String, String> hashes = new LinkedHashMap<>();
  private long bytes;
  private boolean finished;
  public int succeeded, failed;

  public ArchiveBatch(OutputStream out, ArchiveImporter.Control control) {
    zip = new ZipOutputStream(out, StandardCharsets.UTF_8);
    this.control = control;
  }

  public static String directory(int ordinal, String title) {
    StringBuilder safe = new StringBuilder();
    title
        .codePoints()
        .limit(60)
        .forEach(
            cp ->
                safe.appendCodePoint(
                    Character.isISOControl(cp) || "/\\:*?\"<>|".indexOf(cp) >= 0 ? '_' : cp));
    String name = safe.toString().trim().replaceAll("^[. ]+|[. ]+$", "");
    return String.format(Locale.ROOT, "%05d-%s", ordinal, name.isEmpty() ? "会话" : name);
  }

  public void failure(String title, String code) throws ArchiveError {
    checkCount();
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("title", title);
    r.put("status", "failed");
    r.put("error", code.matches("A\\d{2}_[A-Z_]+") ? code : "A11_EXPORT_FAILED");
    results.add(r);
    failed++;
  }

  public void add(ArchiveModel.Conversation c, boolean all, Map<String, ArchiveAsset> assets)
      throws ArchiveError, IOException {
    checkCount();
    ArchiveTree.Selection selection = ArchiveTree.select(c, all);
    String html, md;
    try {
      html = ArchiveRenderer.html(c, selection, assets, ArchiveRenderer.AssetMode.PORTABLE);
      md = ArchiveRenderer.markdown(c, selection, assets);
    } catch (ArchiveError e) {
      if (e.code.equals("A07_IMPORT_CANCELLED")) throw e;
      failure(c.title, e.code);
      return;
    }
    String dir = directory(results.size() + 1, c.title);
    Map<String, ArchiveAsset> selectedAssets = new LinkedHashMap<>();
    int missing = 0, chats = 0;
    for (ArchiveModel.Node n : selection.messages) {
      if (!n.displayable()) continue;
      chats++;
      for (ArchiveDisplay.Block b : ArchiveDisplay.visible(n))
        if (b.asset != null) {
          ArchiveAsset a = assets.get(b.asset.identity);
          if (a == null || !a.available()) {
            missing++;
            continue;
          }
          selectedAssets.putIfAbsent(b.asset.identity, a);
        }
    }
    StringBuilder h = new StringBuilder("<section><h2>附件原件</h2><ul>");
    StringBuilder m = new StringBuilder("\n## 附件原件\n\n");
    Map<String, ArchiveAsset> files = new LinkedHashMap<>();
    for (ArchiveAsset a : selectedAssets.values()) {
      String name =
          String.format(Locale.ROOT, "attachments/%04d", files.size() + 1) + extension(a.mime);
      files.put(name, a);
      h.append("<li><a href=\"")
          .append(name)
          .append("\">")
          .append(ArchiveRenderer.escape(a.name))
          .append("</a></li>");
      m.append("- [附件 ").append(files.size()).append("](").append(name).append(")\n");
    }
    if (!files.isEmpty()) {
      html = html.replace("</body>", h.append("</ul></section>").toString() + "</body>");
      md += m;
    }
    // Render both documents before writing any entry: a rejected conversation has no partial files.
    write(
        dir + "/conversation.html",
        new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
    write(dir + "/conversation.md", new ByteArrayInputStream(md.getBytes(StandardCharsets.UTF_8)));
    for (Map.Entry<String, ArchiveAsset> e : files.entrySet())
      try (InputStream in = new FileInputStream(e.getValue().file)) {
        write(dir + "/" + e.getKey(), in);
      }
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("title", c.title);
    r.put("status", "exported");
    r.put("directory", dir);
    r.put("scope", selection.scope);
    r.put("visibleMessages", chats);
    r.put("researchReports", c.reports.size());
    r.put("attachments", files.size());
    r.put("missingAttachmentReferences", missing);
    r.put("warnings", selection.warnings);
    results.add(r);
    succeeded++;
  }

  private static String extension(String mime) {
    switch (mime) {
      case "image/png":
        return ".png";
      case "image/jpeg":
        return ".jpg";
      case "application/pdf":
        return ".pdf";
      case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
        return ".docx";
      case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
        return ".xlsx";
      default:
        return ".bin";
    }
  }

  private void checkCount() throws ArchiveError {
    control.check();
    if (finished || results.size() >= MAX_CONVERSATIONS)
      throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
  }

  private void write(String name, InputStream in) throws IOException, ArchiveError {
    control.check();
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
    zip.putNextEntry(new ZipEntry(name));
    byte[] buffer = new byte[32768];
    int n;
    while ((n = in.read(buffer)) != -1) {
      control.check();
      bytes += n;
      if (bytes > MAX_BYTES) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
      zip.write(buffer, 0, n);
      digest.update(buffer, 0, n);
    }
    zip.closeEntry();
    hashes.put(name, hex(digest.digest()));
  }

  static String hex(byte[] value) {
    StringBuilder out = new StringBuilder();
    for (byte b : value) out.append(String.format(Locale.ROOT, "%02x", b & 255));
    return out.toString();
  }

  public void finish() throws IOException, ArchiveError {
    control.check();
    Map<String, Object> manifest = new LinkedHashMap<>();
    manifest.put("format", "nova-separate-conversations-v1");
    manifest.put("completeness", "depends on imported data; not independently verified");
    manifest.put("succeeded", succeeded);
    manifest.put("failed", failed);
    manifest.put("conversations", results);
    manifest.put("sha256", new LinkedHashMap<>(hashes));
    write(
        "manifest.json",
        new ByteArrayInputStream(
            ArchiveModel.JSON.toJson(manifest).getBytes(StandardCharsets.UTF_8)));
    zip.finish();
    finished = true;
  }

  /** Re-read every actual ZIP member, checking CRC and SHA against the generated manifest. */
  public static void verify(File file, ArchiveImporter.Control control) throws Exception {
    try (ZipFile zip = new ZipFile(file)) {
      ZipEntry manifest = zip.getEntry("manifest.json");
      if (manifest == null || manifest.getSize() > 16L * 1024 * 1024) throw new IOException();
      Map<?, ?> data;
      try (Reader r = new InputStreamReader(zip.getInputStream(manifest), StandardCharsets.UTF_8)) {
        data = ArchiveModel.JSON.fromJson(r, Map.class);
      }
      Map<String, Object> expected = ArchiveModel.object(data.get("sha256"));
      if (zip.size() != expected.size() + 1) throw new IOException();
      for (Map.Entry<String, Object> item : expected.entrySet()) {
        control.check();
        ZipEntry entry = zip.getEntry(item.getKey());
        if (entry == null) throw new IOException();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        CRC32 crc = new CRC32();
        try (InputStream in = zip.getInputStream(entry)) {
          byte[] b = new byte[32768];
          int n;
          while ((n = in.read(b)) != -1) {
            control.check();
            digest.update(b, 0, n);
            crc.update(b, 0, n);
          }
        }
        if (!hex(digest.digest()).equals(item.getValue()) || crc.getValue() != entry.getCrc())
          throw new IOException();
      }
    }
  }

  @Override
  public void close() throws IOException {
    zip.close();
  }
}
