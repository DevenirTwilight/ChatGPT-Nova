package com.example.chatgptnova.archive;

import com.example.chatgptnova.archive.ArchiveModel.Conversation;
import java.util.*;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.*;

/** Archive model -> static documents. No DOM, snapshot, script or remote assets. */
public final class ArchiveRenderer {
  private static final List<org.commonmark.Extension> EXT =
      Collections.singletonList(TablesExtension.create());
  private static final Parser MARKDOWN = Parser.builder().extensions(EXT).build();
  private static final HtmlRenderer HTML =
      HtmlRenderer.builder()
          .extensions(EXT)
          .escapeHtml(true)
          .sanitizeUrls(true)
          .nodeRendererFactory(
              context ->
                  new org.commonmark.renderer.NodeRenderer() {
                    public Set<Class<? extends Node>> getNodeTypes() {
                      return new HashSet<>(Arrays.asList(Image.class, Link.class));
                    }

                    public void render(Node node) {
                      HtmlWriter w = context.getWriter();
                      if (node instanceof Image) {
                        w.text("[图片：离线附件占位]");
                        return;
                      }
                      Link link = (Link) node;
                      String url = link.getDestination();
                      boolean allowed = url.matches("(?i)^https?://[^\\s]+$");
                      if (allowed) {
                        Map<String, String> attrs = new LinkedHashMap<>();
                        attrs.put("href", url);
                        attrs.put("rel", "noreferrer noopener");
                        w.tag("a", attrs);
                      } else w.tag("span");
                      for (Node child = node.getFirstChild();
                          child != null;
                          child = child.getNext()) context.render(child);
                      w.tag(allowed ? "/a" : "/span");
                    }
                  })
          .build();

  private ArchiveRenderer() {}

  public static String escape(String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  public static String role(ArchiveModel.Node n) {
    if (n.contentType.equals("reasoning_recap")) return "推理摘要";
    switch (n.role) {
      case "user":
        return "User";
      case "assistant":
        return "Assistant";
      case "system":
        return "System";
      case "developer":
        return "Developer";
      case "tool":
        return "Tool";
      default:
        return "Unknown";
    }
  }

  public static String markdown(Conversation c, ArchiveTree.Selection selection)
      throws ArchiveError {
    StringBuilder out =
        new StringBuilder("# ")
            .append(c.title.replace('\n', ' '))
            .append("\n\nSource: Nova Archive / user-selected export\nScope: ")
            .append(selection.scope)
            .append(
                "\n"
                    + "Completeness: depends on imported data; not independently verified.\n"
                    + "Display: thoughts nodes hidden; raw metadata retained.\n\n");
    for (String w : selection.warnings) out.append("> 警告：").append(w).append("\n\n");
    for (ArchiveModel.Node n : selection.messages) {
      if (!n.displayable()) continue;
      out.append("## ").append(role(n));
      if (!n.channel.isEmpty()) out.append(" · ").append(n.channel.replace('\n', ' '));
      out.append("\n\n").append(n.text()).append("\n\n");
      if (out.length() > 2 * 1024 * 1024) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    }
    return out.toString();
  }

  private static String renderMarkdown(String text) throws ArchiveError {
    // CommonMark parses its tree iteratively; reject extreme nesting before recursive HTML
    // rendering.
    Node document = MARKDOWN.parse(text);
    Node cursor = document;
    int depth = 0, count = 0;
    while (cursor != null) {
      if (++count > 100000 || depth > 64) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
      if (cursor.getFirstChild() != null) {
        cursor = cursor.getFirstChild();
        depth++;
        continue;
      }
      while (cursor != null && cursor.getNext() == null) {
        cursor = cursor.getParent();
        depth--;
      }
      if (cursor != null) cursor = cursor.getNext();
    }
    return HTML.render(document);
  }

  public static String html(Conversation c, ArchiveTree.Selection selection) throws ArchiveError {
    StringBuilder out =
        new StringBuilder(
                "<!doctype html><html lang=\"zh\"><head><meta charset=\"utf-8\"><meta"
                    + " name=\"viewport\" content=\"width=device-width,initial-scale=1\"><meta"
                    + " http-equiv=\"Content-Security-Policy\" content=\"default-src 'none';"
                    + " style-src 'unsafe-inline'; img-src 'none'; font-src 'none'; connect-src"
                    + " 'none'; frame-src 'none'; script-src 'none'; base-uri 'none'; form-action"
                    + " 'none'\"><meta name=\"captureMode\" content=\"nova-archive\"><meta"
                    + " name=\"historyCompleteness\""
                    + " content=\"depends-on-import-not-verified\"><title>")
            .append(escape(c.title))
            .append(
                "</title><style>body{font-family:system-ui,sans-serif;line-height:1.6;margin:20px"
                    + " auto;padding:0"
                    + " 16px;max-width:860px;color:#18202b;background:#fff}h1{font-size:24px}h2{font-size:18px}.meta{color:#536173;font-size:13px}article{border-top:1px"
                    + " solid"
                    + " #dce0e5;margin-top:24px;padding-top:12px;overflow-wrap:anywhere}pre{background:#f4f5f7;padding:12px;white-space:pre-wrap;overflow-wrap:anywhere;font-size:12px}code{font-family:monospace}blockquote{border-left:3px"
                    + " solid"
                    + " #aaa;margin-left:0;padding-left:12px;color:#536173}table{border-collapse:collapse;display:block;overflow:auto}th,td{border:1px"
                    + " solid #ccc;padding:6px}a{color:#145ba8}@page{size:A4;margin:16mm}@media"
                    + " print{body{margin:0;max-width:none;padding:0}table{display:table}pre{white-space:pre-wrap}h2{break-after:avoid}article{break-inside:auto}}</style></head><body><h1>")
            .append(escape(c.title))
            .append("</h1><p class=\"meta\">Nova Archive · 用户选择的导出文件 · ")
            .append(escape(selection.scope))
            .append(
                "<br>完整性取决于导入文件本身；Nova 未独立验证完整历史。thoughts 节点默认隐藏，raw metadata 保留。LaTeX 保留源文；附件"
                    + " metadata 保存在本地，不下载附件。</p>");
    for (String w : selection.warnings)
      out.append("<p class=\"meta\">警告：").append(escape(w)).append("</p>");
    for (ArchiveModel.Node n : selection.messages) {
      if (!n.displayable()) continue;
      out.append("<article><h2>").append(role(n));
      if (!n.channel.isEmpty()) out.append(" · ").append(escape(n.channel));
      out.append("</h2>").append(renderMarkdown(n.text())).append("</article>");
      if (out.length() > 4 * 1024 * 1024) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    }
    return out.append("</body></html>").toString();
  }

  public enum AssetMode {
    READER,
    PORTABLE,
    PRINT
  }

  public static final String LOCAL_ORIGIN = "https://nova-archive.invalid";
  private static final long EMBED_IMAGE_LIMIT = 2L * 1024 * 1024,
      EMBED_TOTAL_LIMIT = 6L * 1024 * 1024;

  /** Same ordered interpretation for reader, portable HTML and print; documents are descriptors. */
  public static String html(
      Conversation c,
      ArchiveTree.Selection selection,
      Map<String, ArchiveAsset> assets,
      AssetMode mode)
      throws ArchiveError {
    String base =
        html(
            c,
            new ArchiveTree.Selection(
                Collections.emptyList(), selection.warnings, selection.scope));
    base =
        base.replace(
            "img-src 'none'",
            mode == AssetMode.PORTABLE ? "img-src data:" : "img-src https://nova-archive.invalid");
    base = base.replace("metadata 保存在本地，不下载附件。", "来自导入文件，离线保存在本地；独立文档内容不合并进会话导出。");
    base =
        base.replace(
            "</style>",
            "figure{margin:12px"
                + " 0}img{max-width:100%;height:auto}figcaption,.asset{font-size:13px;overflow-wrap:anywhere}.asset{border:1px"
                + " solid #ccd3dc;padding:10px;margin:8px 0}@media"
                + " print{img{max-height:240mm;object-fit:contain}figure{break-inside:avoid}a.asset-open{display:none}}</style>");
    StringBuilder out = new StringBuilder(base.substring(0, base.lastIndexOf("</body>")));
    long embedded = 0;
    for (ArchiveModel.Node node : selection.messages) {
      if (!node.displayable()) continue;
      out.append("<article><h2>").append(role(node));
      if (!node.channel.isEmpty()) out.append(" · ").append(escape(node.channel));
      out.append("</h2>");
      for (ArchiveDisplay.Block block : ArchiveDisplay.visible(node)) {
        if (block.asset == null) {
          out.append(renderMarkdown(block.text));
          continue;
        }
        ArchiveDisplay.Ref ref = block.asset;
        ArchiveAsset asset = assets.get(ref.identity);
        String name = asset == null ? ref.displayName : asset.name;
        String label = (ref.ordinal + 1) + ". " + name;
        if (asset == null || !asset.available()) {
          out.append("<div class=\"asset\">")
              .append(escape(label))
              .append(" · 附件未包含在导出文件中或无法读取</div>");
          continue;
        }
        String src = null;
        if (asset.image()) {
          if (mode != AssetMode.PORTABLE) src = LOCAL_ORIGIN + "/images/" + asset.file.getName();
          else if (asset.bytes <= EMBED_IMAGE_LIMIT
              && embedded + asset.bytes <= EMBED_TOTAL_LIMIT) {
            try (java.io.InputStream in = new java.io.FileInputStream(asset.file);
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream()) {
              byte[] b = new byte[32768];
              int n;
              long count = 0;
              while ((n = in.read(b)) != -1) {
                count += n;
                if (count > EMBED_IMAGE_LIMIT) throw new java.io.IOException();
                bytes.write(b, 0, n);
              }
              if (count != asset.bytes) throw new java.io.IOException();
              src =
                  "data:"
                      + asset.mime
                      + ";base64,"
                      + java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());
              embedded += count;
            } catch (java.io.IOException e) {
              src = null;
            }
          }
        }
        if (src != null)
          out.append("<figure><img src=\"")
              .append(escape(src))
              .append("\" alt=\"")
              .append(escape(name))
              .append("\" width=\"")
              .append(asset.width)
              .append("\" height=\"")
              .append(asset.height)
              .append("\"><figcaption>")
              .append(escape(label))
              .append("</figcaption></figure>");
        out.append("<div class=\"asset\">")
            .append(escape(label))
            .append(" · ")
            .append(escape(asset.mime))
            .append(" · ")
            .append(asset.bytes)
            .append(" bytes · Archived locally in Nova");
        if (asset.image() && src == null) out.append(" · 图片超过单文件内嵌预算，原图仍在Nova本地");
        if (mode == AssetMode.READER)
          out.append(" · <a class=\"asset-open\" href=\"")
              .append(LOCAL_ORIGIN)
              .append("/attachments/")
              .append(asset.file.getName())
              .append("\">打开附件</a>");
        out.append("</div>");
      }
      out.append("</article>");
      if (out.length() > 16 * 1024 * 1024) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
    }
    return out.append("</body></html>").toString();
  }

  public static String markdown(
      Conversation c, ArchiveTree.Selection selection, Map<String, ArchiveAsset> assets)
      throws ArchiveError {
    StringBuilder out =
        new StringBuilder(
            markdown(
                c,
                new ArchiveTree.Selection(
                    Collections.emptyList(), selection.warnings, selection.scope)));
    for (ArchiveModel.Node node : selection.messages) {
      if (!node.displayable()) continue;
      out.append("## ").append(role(node)).append("\n\n");
      for (ArchiveDisplay.Block block : ArchiveDisplay.visible(node)) {
        if (block.asset == null) out.append(block.text).append("\n\n");
        else {
          ArchiveAsset asset = assets.get(block.asset.identity);
          String name =
              (asset == null ? block.asset.displayName : asset.name)
                  .replace("\\", "\\\\")
                  .replace("[", "\\[")
                  .replace("]", "\\]");
          out.append("附件 ").append(block.asset.ordinal + 1).append(": ").append(name);
          if (asset == null || !asset.available()) out.append(" · 附件未包含在导出文件中或无法读取");
          else
            out.append(" · ")
                .append(asset.mime)
                .append(" · ")
                .append(asset.bytes)
                .append(" bytes · Archived locally in Nova")
                .append(asset.image() ? " · 图片原件保存在Nova；单Markdown不携带binary" : "");
          out.append("\n\n");
        }
        if (out.length() > 2 * 1024 * 1024) throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
      }
    }
    return out.toString();
  }
}
