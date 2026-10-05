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
    @Before public void before() {
        start();
        android.accessibilityservice.AccessibilityServiceInfo info=instrument.getUiAutomation().getServiceInfo();
        info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        instrument.getUiAutomation().setServiceInfo(info);
    }
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
        long until=android.os.SystemClock.uptimeMillis()+20000;
        do {
            AccessibilityNodeInfo node=findControl(instrument.getUiAutomation().getRootInActiveWindow(),label);
            if(node!=null && node.isVisibleToUser() && node.isEnabled()) {
                // Labels may be non-clickable TextViews inside an actionable row.
                // Use the native accessibility action on that row; coordinate taps
                // can hit the previous window while a popup is still transitioning.
                AccessibilityNodeInfo target=node;
                while(target!=null && !target.isClickable()) target=target.getParent();
                if(target!=null && target.isEnabled() && target.isVisibleToUser()
                    && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return;
            }
            android.os.SystemClock.sleep(100);
        } while(android.os.SystemClock.uptimeMillis()<until);
        fail("Timed out: export control "+label+"\n"+dumpPrintWindow(instrument.getUiAutomation().getRootInActiveWindow()));
    }
    private static String shellScript(String script) {
        // UiAutomation uses Runtime.exec rather than a shell parser. A whitespace-
        // free sh -c argument preserves the quoted UTF-8 filename as one command.
        String encoded=android.util.Base64.encodeToString(script.getBytes(StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
        return "sh -c eval${IFS}$(echo${IFS}"+encoded+"|base64${IFS}-d)";
    }
    private static AccessibilityNodeInfo findControl(AccessibilityNodeInfo node,String label) {
        if(node==null) return null;
        if(node.getText()!=null && label.equalsIgnoreCase(node.getText().toString())
            || node.getContentDescription()!=null && label.equalsIgnoreCase(node.getContentDescription().toString())) return node;
        for(int i=0;i<node.getChildCount();i++) {
            AccessibilityNodeInfo found=findControl(node.getChild(i),label);if(found!=null) return found;
        }
        return null;
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
            """.replace("%s",org.json.JSONObject.quote(body)));
    }
    private void export(String format) { main(()->exporter().start());click(format); }
    private void external(java.util.function.Function<Intent,Instrumentation.ActivityResult> action) {
        monitor=new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {return action.apply(intent);}
        };instrument.addMonitor(monitor);
    }
    @Test public void pageOwnedReaderLoadsViaAndroidEvaluateJavascript() throws Exception {
        conversation("Unrendered full-tree ancestor 中文");
        String module = """
            export async function fullReader(id, options={}) {
              if (!options.includeFullConversation || !options.forceNetworkFetch) throw Error('full read required');
              const message=(id,role,text)=>({id,author:{role},recipient:'all',status:'finished_successfully',content:{content_type:'text',parts:[text]}});
              const tree={conversation_id:id,title:'Android reader',current_node:'a',mapping:{
                root:{id:'root',parent:null,children:['u'],message:null},
                u:{id:'u',parent:'root',children:['a'],message:message('u','user','Unrendered full-tree ancestor 中文')},
                a:{id:'a',parent:'u',children:[],message:message('a','assistant','reply')}
              }};
              options.onConversationLoadedFromNetwork(tree);
              window.novaReaderCalled=true;
              return {normalized:true};
            }
            export {fullReader as alias};
            window.novaReaderModuleLoaded=true;
            """;
        main(()-> {
            android.webkit.WebViewClient previous=web.getWebViewClient();
            web.setWebViewClient(new android.webkit.WebViewClient() {
                @Override public android.webkit.WebResourceResponse shouldInterceptRequest(android.webkit.WebView view,android.webkit.WebResourceRequest request) {
                    if ("https://chatgpt.com/cdn/assets/conversation-fixture.js".equals(request.getUrl().toString()))
                        return new android.webkit.WebResourceResponse("application/javascript","UTF-8",new java.io.ByteArrayInputStream(module.getBytes(StandardCharsets.UTF_8)));
                    return previous.shouldInterceptRequest(view,request);
                }
            });
        });
        // Load as the website does; then exercise production evaluateJavascript import.
        js("(()=>{const s=document.createElement('script');s.type='module';s.src='/cdn/assets/conversation-fixture.js';document.head.append(s);return true;})()");
        waitFor("website module loaded",()->"true".equals(js("window.novaReaderModuleLoaded===true")));
        String capture;
        try (java.io.InputStream input=activity.getAssets().open("export/capture.js")) { capture=new String(input.readAllBytes(),StandardCharsets.UTF_8); }
        js("window.__novaExportCapture=null;"+capture);
        export("HTML 阅读版（推荐）");
        waitFor("reader export written",()->output()!=null && output().isFile());
        assertEquals("true",js("window.novaReaderCalled===true"));
        assertTrue(new String(java.nio.file.Files.readAllBytes(output().toPath()),StandardCharsets.UTF_8).contains("Unrendered full-tree ancestor 中文"));
        instrument.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
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
        long[] nextBack={0};
        waitFor("PDF cancel releases exporter",()-> {
            if(!busy()) return true;
            AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();
            long now=android.os.SystemClock.uptimeMillis();
            if(root!=null && "com.android.printspooler".contentEquals(root.getPackageName()) && now>=nextBack[0]) {
                instrument.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
                nextBack[0]=now+2000;
            }
            return false;
        });
    }
    @Test public void pdfSavedBySystemOpensWithMultiplePages() throws Exception {
        StringBuilder text=new StringBuilder();for(int i=0;i<80;i++) text.append("段落 ").append(i).append(" 中文 English [link](https://example.org)\n\n");
        text.append("## Code / 表格 / 数学\n\n```java\nString name = \"中文\";\n")
            .append("long-code-".repeat(200)).append("LONG_LINE_END\n```\n\n")
            .append("| 名称 | Value | Link |\n| --- | --- | --- |\n| export-table-row | **中文** | [target](https://example.org/android-pdf-target) |\n\n")
            .append("Inline `code` and $E = mc^2$。\n\n");
        conversation(text.toString());export("PDF");
        waitFor("print service window",()-> {
            AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();
            return root!=null && "com.android.printspooler".contentEquals(root.getPackageName());
        });
        // Android can initially show "Select a printer" rather than choosing PDF.
        waitFor("printer destination selector",()-> {
            AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();
            if(root==null) return false;
            for(AccessibilityNodeInfo selector:root.findAccessibilityNodeInfosByViewId("com.android.printspooler:id/destination_spinner"))
                if(selector.isClickable()) return selector.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return false;
        });
        waitFor("printer destination popup",()-> {
            AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();
            return findControl(root,"All printers…")!=null && findControl(root,"Save as PDF")!=null
                && root.findAccessibilityNodeInfosByViewId("com.android.printspooler:id/print_button").isEmpty();
        });
        click("Save as PDF");
        // The long document's preview is asynchronous; wait separately from extraction.
        long previewDeadline=android.os.SystemClock.uptimeMillis()+60000;
        boolean saved=false;
        do {
            AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();
            if(root!=null) {
                for(AccessibilityNodeInfo button:root.findAccessibilityNodeInfosByViewId("com.android.printspooler:id/print_button"))
                    if(button.isEnabled() && button.isClickable() && "Save to PDF".contentEquals(button.getContentDescription()))
                        saved=true;
            }
            if(!saved) android.os.SystemClock.sleep(200);
        } while(!saved && android.os.SystemClock.uptimeMillis()<previewDeadline);
        String diagnostic="";
        if(!saved) {
            diagnostic=dumpPrintWindow(instrument.getUiAutomation().getRootInActiveWindow());
        }
        assertTrue("System print preview must enable Save as PDF\n"+diagnostic,saved);
        click("Save to PDF");
        click("Save");
        waitFor("system PDF save finished",()->!busy());
        String path="/sdcard/Download/"+output().getName();
        String command="cat '"+path.replace("'","'\\''")+"'";
        byte[] pdf=new byte[0];
        long fileDeadline=android.os.SystemClock.uptimeMillis()+20000;
        do {
            try(android.os.ParcelFileDescriptor result=instrument.getUiAutomation().executeShellCommand(shellScript(command));
                java.io.InputStream input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(result);
                java.io.ByteArrayOutputStream data=new java.io.ByteArrayOutputStream()) {
                byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)data.write(buffer,0,count);pdf=data.toByteArray();
            }
            if(pdf.length>1000 && new String(pdf,0,5,StandardCharsets.US_ASCII).equals("%PDF-")) break;
            android.os.SystemClock.sleep(200);
        } while(android.os.SystemClock.uptimeMillis()<fileDeadline);
        assertTrue("System saved a PDF",pdf.length>1000);assertEquals("%PDF-",new String(pdf,0,5,StandardCharsets.US_ASCII));
        File local=new File(activity.getFilesDir(),"printed-export.pdf");
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(local)){out.write(pdf);}
        try(android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(local,android.os.ParcelFileDescriptor.MODE_READ_ONLY);
            android.graphics.pdf.PdfRenderer renderer=new android.graphics.pdf.PdfRenderer(fd)) {
            assertTrue("Long HTML became multiple PDF pages",renderer.getPageCount()>1);
            try(android.graphics.pdf.PdfRenderer.Page page=renderer.openPage(0)){assertTrue(page.getWidth()>0);assertTrue(page.getHeight()>0);}
        }
    }
    private static String dumpPrintWindow(AccessibilityNodeInfo node) {
        if(node==null) return "No active window";
        StringBuilder dump=new StringBuilder(node.toString()).append('\n');
        for(int i=0;i<node.getChildCount();i++) dump.append(dumpPrintWindow(node.getChild(i)));
        return dump.toString();
    }
    @Test public void unconfirmedBranchRefusesFileAndShowsPersistentError() {
        conversation("user text");
        js("document.querySelector('[data-message-id]').setAttribute('data-message-id','u')");
        main(()->exporter().start());
        click("知道了");
        assertFalse(busy());assertNull(output());
    }
    @Test public void filenamesHandleUnicodeInvalidCharactersAndDuplicates() {
        String a=ConversationExport.filename("中文 / : * ? \" <> | "+"😀".repeat(80),"html");
        String b=ConversationExport.filename("中文 / : * ? \" <> | "+"😀".repeat(80),"html");
        assertTrue(a.startsWith("ChatGPT-中文"));assertTrue(a.endsWith(".html"));assertFalse(a.matches(".*[\\\\/:*?\"<>|].*"));assertNotEquals(a,b);
        assertTrue(ConversationExport.filename("", "md").contains("未命名会话"));
    }
}
