package com.example.chatgptnova.archive;

import java.util.*;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.*;
import org.commonmark.ext.gfm.tables.TablesExtension;
import com.example.chatgptnova.archive.ArchiveModel.Conversation;

/** Archive model -> static documents. No DOM, snapshot, script or remote assets. */
public final class ArchiveRenderer {
    private static final List<org.commonmark.Extension> EXT=Collections.singletonList(TablesExtension.create());
    private static final Parser MARKDOWN=Parser.builder().extensions(EXT).build();
    private static final HtmlRenderer HTML=HtmlRenderer.builder().extensions(EXT).escapeHtml(true).sanitizeUrls(true)
        .nodeRendererFactory(context->new org.commonmark.renderer.NodeRenderer() {
            public Set<Class<? extends Node>> getNodeTypes(){return new HashSet<>(Arrays.asList(Image.class,Link.class));}
            public void render(Node node) {
                HtmlWriter w=context.getWriter();
                if(node instanceof Image){w.text("[图片：离线附件占位]");return;}
                Link link=(Link)node;String url=link.getDestination();boolean allowed=url.matches("(?i)^https?://[^\\s]+$");
                if(allowed){Map<String,String> attrs=new LinkedHashMap<>();attrs.put("href",url);attrs.put("rel","noreferrer noopener");w.tag("a",attrs);}else w.tag("span");
                for(Node child=node.getFirstChild();child!=null;child=child.getNext())context.render(child);
                w.tag(allowed?"/a":"/span");
            }
        }).build();
    private ArchiveRenderer() {}
    public static String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    public static String role(ArchiveModel.Node n) {
        switch(n.role){case "user":return "User";case "assistant":return "Assistant";case "system":return "System";case "developer":return "Developer";case "tool":return "Tool";default:return "Unknown";}
    }
    public static String markdown(Conversation c,ArchiveTree.Selection selection)throws ArchiveError {
        StringBuilder out=new StringBuilder("# ").append(c.title.replace('\n',' ')).append("\n\nSource: Nova Archive / user-selected export\nScope: ").append(selection.scope)
            .append("\nCompleteness: depends on imported data; not independently verified.\n\n");
        for(String w:selection.warnings)out.append("> 警告：").append(w).append("\n\n");
        for(ArchiveModel.Node n:selection.messages){out.append("## ").append(role(n));if(!n.channel.isEmpty())out.append(" · ").append(n.channel.replace('\n',' '));out.append("\n\n").append(n.text()).append("\n\n");if(out.length()>2*1024*1024)throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");}
        return out.toString();
    }
    public static String html(Conversation c,ArchiveTree.Selection selection)throws ArchiveError {
        StringBuilder out=new StringBuilder("<!doctype html><html lang=\"zh\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; style-src 'unsafe-inline'; img-src 'none'; font-src 'none'; connect-src 'none'; frame-src 'none'; script-src 'none'; base-uri 'none'; form-action 'none'\"><meta name=\"captureMode\" content=\"nova-archive\"><meta name=\"historyCompleteness\" content=\"depends-on-import-not-verified\"><title>")
            .append(escape(c.title)).append("</title><style>body{font-family:system-ui,sans-serif;line-height:1.6;margin:20px auto;padding:0 16px;max-width:860px;color:#18202b;background:#fff}h1{font-size:24px}h2{font-size:18px}.meta{color:#536173;font-size:13px}article{border-top:1px solid #dce0e5;margin-top:24px;padding-top:12px;overflow-wrap:anywhere}pre{background:#f4f5f7;padding:12px;white-space:pre-wrap;overflow-wrap:anywhere;font-size:12px}code{font-family:monospace}blockquote{border-left:3px solid #aaa;margin-left:0;padding-left:12px;color:#536173}table{border-collapse:collapse;display:block;overflow:auto}th,td{border:1px solid #ccc;padding:6px}a{color:#145ba8}@page{size:A4;margin:16mm}@media print{body{margin:0;max-width:none;padding:0}table{display:table}pre{white-space:pre-wrap}h2{break-after:avoid}article{break-inside:auto}}</style></head><body><h1>")
            .append(escape(c.title)).append("</h1><p class=\"meta\">Nova Archive · 用户选择的导出文件 · ").append(escape(selection.scope)).append("<br>完整性取决于导入文件本身；Nova 未独立验证完整历史。LaTeX 保留源文；附件 metadata 保存在本地，不下载附件。</p>");
        for(String w:selection.warnings)out.append("<p class=\"meta\">警告：").append(escape(w)).append("</p>");
        for(ArchiveModel.Node n:selection.messages) {
            out.append("<article><h2>").append(role(n));if(!n.channel.isEmpty())out.append(" · ").append(escape(n.channel));out.append("</h2>").append(HTML.render(MARKDOWN.parse(n.text()))).append("</article>");
            if(out.length()>4*1024*1024)throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
        }return out.append("</body></html>").toString();
    }
}
