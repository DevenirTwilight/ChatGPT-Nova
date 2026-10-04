package com.example.chatgptnova;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic current-origin data through the production exporter; no account access. */
public final class ConversationExportTest extends FixtureActivity {
    private Instrumentation.ActivityMonitor monitor;
    @Before public void before() { start(); }
    @After public void after() {
        if (monitor!=null) instrument.removeMonitor(monitor);
        if (scenario!=null) scenario.close();
    }
    private ConversationExport exporter() {
        try { Field f=MainActivity.class.getDeclaredField("conversationExport");f.setAccessible(true);return (ConversationExport)f.get(activity); }
        catch (Exception e) {throw new AssertionError(e);}
    }
    private File output() {
        try { Field f=ConversationExport.class.getDeclaredField("file");f.setAccessible(true);return (File)f.get(exporter()); }
        catch (Exception e) {throw new AssertionError(e);}
    }
    private boolean busy() {
        AtomicReference<Boolean> result=new AtomicReference<>(true);
        main(()-> {try {Field f=ConversationExport.class.getDeclaredField("busy");f.setAccessible(true);result.set(f.getBoolean(exporter()));}catch(Exception e){throw new AssertionError(e);}});
        return result.get();
    }
    private void click(String label) {
        waitFor("export control "+label,()-> {
            AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();
            if (root==null) return false;
            for (AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(label)) {
                if (!label.contentEquals(n.getText())) continue;
                for (AccessibilityNodeInfo p=n;p!=null;p=p.getParent()) if (p.isClickable()) return p.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            return false;
        });
    }
    private void conversation(String body) {
        js("""
            (()=>{
              history.replaceState({},'', '/c/fixture');
              document.body.innerHTML='<div data-message-id="a" data-message-author-role="assistant">reply</div>';
              const message=(id,role,text)=>({id,author:{role},recipient:'all',status:'finished_successfully',content:{content_type:'text',parts:[text]}});
              const tree={conversation_id:'fixture',title:'中文 / : * ? 标题',current_node:'a',mapping:{
                root:{id:'root',parent:null,children:['u'],message:null},
                u:{id:'u',parent:'root',children:['a'],message:message('u','user',%s)},
                a:{id:'a',parent:'u',children:[],message:message('a','assistant','reply')}
              }};
              window.__novaReadConversation=async()=>tree;
              return true;
            })()
            """.formatted(org.json.JSONObject.quote(body)));
    }
    private void export(String format) { main(()->exporter().start());click(format); }
    private void external(java.util.function.Function<Intent,Instrumentation.ActivityResult> action) {
        monitor=new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {return action.apply(intent);}
        };instrument.addMonitor(monitor);
    }
    @Test public void htmlSavesUnrenderedMessagesAndRealLink() throws Exception {
        conversation("# 未渲染标题\n\n**中文 English** [link](https://example.com/path?q=1#target)\n\n```java\ncode\n```\n");
        AtomicReference<Intent> captured=new AtomicReference<>();
        external(intent-> {
            if (!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) return null;
            captured.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(OUTPUT));
        });
        export("HTML 阅读版（推荐）");click("保存到本地…");
        waitFor("save completion",()->captured.get()!=null && !busy());
        String saved=new String(read(OUTPUT),StandardCharsets.UTF_8);
        assertTrue(saved.contains("未渲染标题"));assertTrue(saved.contains("https://example.com/path?q=1#target"));
        assertFalse(saved.contains("prompt-textarea"));assertEquals("text/html",captured.get().getType());
        assertTrue(captured.get().hasCategory(Intent.CATEGORY_OPENABLE));
    }
    @Test public void cancelledSaveCanRepeatAndShareReadableMarkdown() throws Exception {
        conversation("## 编辑用正文\n\n- item\n\n[link](https://example.com)");
        AtomicReference<Intent> captured=new AtomicReference<>();
        external(intent-> {
            if (Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);
            if (Intent.ACTION_CHOOSER.equals(intent.getAction())) {captured.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);}
            return null;
        });
        export("Markdown");File first=output();click("保存到本地…");waitFor("cancelled save",()->!busy());
        export("Markdown");File second=output();assertNotEquals(first.getName(),second.getName());click("分享文件");
        waitFor("markdown chooser",()->captured.get()!=null);
        Intent send=captured.get().getParcelableExtra(Intent.EXTRA_INTENT);
        assertEquals("text/markdown",send.getType());assertTrue((send.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
        Uri uri=send.getParcelableExtra(Intent.EXTRA_STREAM);assertNotNull(uri);
        assertTrue(new String(read(uri),StandardCharsets.UTF_8).contains("## 编辑用正文"));
    }
    @Test public void pdfUsesSystemPrintSaveAndCanCancel() throws Exception {
        StringBuilder text=new StringBuilder();for(int i=0;i<80;i++) text.append("段落 ").append(i).append(" 中文 English [link](https://example.org)\n\n");
        conversation(text.toString());
        export("PDF");
        waitFor("system PDF print job",()-> {
            AtomicReference<Boolean> found=new AtomicReference<>(false);
            main(()-> { try {Field f=ConversationExport.class.getDeclaredField("printJob");f.setAccessible(true);found.set(f.get(exporter())!=null);}catch(Exception e){throw new AssertionError(e);} });
            return found.get();
        });
        waitFor("print service window",()-> {
            AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();
            return root!=null && "com.android.printspooler".contentEquals(root.getPackageName());
        });
        instrument.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        waitFor("PDF cancel releases exporter",()->!busy());
    }
    @Test public void filenamesHandleUnicodeInvalidCharactersAndDuplicates() {
        String a=ConversationExport.filename("中文 / : * ? \" <> | "+"😀".repeat(80),"html");
        String b=ConversationExport.filename("中文 / : * ? \" <> | "+"😀".repeat(80),"html");
        assertTrue(a.startsWith("ChatGPT-中文"));assertTrue(a.endsWith(".html"));assertFalse(a.matches(".*[\\\\/:*?\"<>|].*"));assertNotEquals(a,b);
        assertTrue(ConversationExport.filename("", "md").contains("未命名会话"));
    }
}
