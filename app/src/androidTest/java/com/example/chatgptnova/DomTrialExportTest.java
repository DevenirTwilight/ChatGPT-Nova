package com.example.chatgptnova;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** DOM trial on Android fixtures; no real account or completeness proof. */
public final class DomTrialExportTest extends FixtureActivity {
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
        long until=android.os.SystemClock.uptimeMillis()+("选择导出格式".equals(label) ? 150000 : 20000);
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
    private void conversation(String ignored) {
        js("""
            (()=>{
              history.replaceState({},'', '/g/g-p-fixture/c/fixture');
              document.title='DOM trial 中文';
              document.body.innerHTML='<article data-message-id="u"><div data-message-author-role="user"><div class="whitespace-pre-wrap">USER-FIRST 中文😀</div></div></article>'
                +'<article data-message-id="a"><div data-message-author-role="assistant"><div class="markdown">'
                +Array.from({length:80},(_,i)=>'<p>段落 '+i+' 中文 English</p>').join('')
                +'<pre><code>CODE-FIRST\\n'+('long-code-'.repeat(200))+'CODE-LAST</code></pre>'
                +'<table><tr><th>名称</th><th>Value</th></tr><tr><td>中文</td><td>TABLE-LAST</td></tr></table><p>ASSISTANT-LAST</p></div></div></article>';
              return true;
            })()
            """);
    }
    private void export(String format) { main(()->exporter().start());click("快速导出已加载消息");click(format); }
    private void external(java.util.function.Function<Intent,Instrumentation.ActivityResult> action) {
        monitor=new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {return action.apply(intent);}
        };instrument.addMonitor(monitor);
    }
    private void virtualHistory() throws Exception {virtualHistory(false);}
    private void virtualHistory(boolean narrow) throws Exception {
        try(java.io.InputStream input=instrument.getContext().getAssets().open("virtual-history.js")) {
            js(new String(input.readAllBytes(),StandardCharsets.UTF_8).replace("__NOVA_FIXTURE_COUNT__",narrow ? "40,3" : "40"));
            js("document.getElementById('history').style.scrollBehavior='smooth';true");
        }
    }
    @Test public void historyScrollCachesUnmountedMessagesThroughSaf() throws Exception {
        virtualHistory();String original=js("document.getElementById('history').scrollTop");
        AtomicReference<Intent> saved=new AtomicReference<>();
        external(intent->{if(!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) return null;saved.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(OUTPUT));});
        main(()->exporter().start());click("单向扫描历史");click("选择导出格式");click("HTML 阅读版（推荐）");click("保存到本地…");
        waitFor("cached virtual history saved",()->saved.get()!=null && !busy());
        String html=new String(read(OUTPUT),StandardCharsets.UTF_8);
        for(int i=0;i<40;i++) if(i!=12 && i!=14) assertTrue("missing ROW-"+i,html.contains("ROW-"+i+"</p>"));
        assertEquals(40,html.split("<article>",-1).length-1);
        assertEquals(2,html.split("REPEATED-TEXT",-1).length-1);
        assertTrue(html.contains("未做折返核对"));assertTrue(html.contains("滚动收集并缓存可见历史"));assertTrue(html.contains("完整历史未确认"));assertTrue(html.contains("无独立基准"));
        assertEquals("7",js("document.querySelectorAll('[data-message-author-role]').length"));
        assertEquals(original,js("document.getElementById('history').scrollTop"));assertEquals("undefined",js("typeof window.__novaHistoryScrollTrial"));
    }
    @Test public void topStartMakesOneDownwardPassAndReportsNoSecondPass() throws Exception {
        virtualHistory();js("document.getElementById('history').scrollTo({top:0,behavior:'instant'});true");
        main(()->exporter().start());click("单向扫描历史");click("复制诊断");
        AtomicReference<String> detail=new AtomicReference<>();
        main(()->{android.content.ClipboardManager c=activity.getSystemService(android.content.ClipboardManager.class);detail.set(c.getPrimaryClip().getItemAt(0).getText().toString());});
        org.json.JSONObject diag=new org.json.JSONObject(detail.get()),coverage=diag.getJSONObject("coverage");
        assertEquals("H00_READY",diag.getString("code"));assertEquals(40,coverage.getInt("count"));assertEquals(1,coverage.getInt("leg"));
        assertEquals("down",coverage.getString("direction"));assertFalse(coverage.getBoolean("secondPass"));assertEquals("not-proven",coverage.getString("history"));
        assertEquals("0",js("document.getElementById('history').scrollTop"));assertEquals("undefined",js("typeof window.__novaHistoryScrollTrial"));
    }
    @Test public void standardHistoryBacktracksToKeepNarrowWindowOverlap() throws Exception {
        virtualHistory(true);
        AtomicReference<Intent> saved=new AtomicReference<>();
        external(intent->{if(!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) return null;saved.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(OUTPUT));});
        main(()->exporter().start());click("单向扫描历史");click("选择导出格式");click("HTML 阅读版（推荐）");click("保存到本地…");
        waitFor("narrow window history saved",()->saved.get()!=null && !busy());
        String html=new String(read(OUTPUT),StandardCharsets.UTF_8);assertEquals(40,html.split("<article>",-1).length-1);
        assertTrue(html.contains("ROW-0</p>"));assertTrue(html.contains("ROW-39</p>"));assertTrue(html.contains("未做折返核对"));assertTrue(html.contains("完整历史未确认"));
    }
    @Test public void layoutOnlyJitterStillSavesAllCachedMessages() throws Exception {
        virtualHistory();js("document.getElementById('history').scrollTo({top:640,behavior:'instant'});let jitterTick=0;window.fixtureJitter=setInterval(()=>{document.getElementById('space').style.height=(2560+(++jitterTick%2)*2)+'px';},70);true");
        AtomicReference<Intent> saved=new AtomicReference<>();
        external(intent->{if(!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) return null;saved.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(OUTPUT));});
        main(()->exporter().start());click("单向扫描历史");click("选择导出格式");click("HTML 阅读版（推荐）");click("保存到本地…");
        waitFor("layout jitter cached history saved",()->saved.get()!=null && !busy());
        String html=new String(read(OUTPUT),StandardCharsets.UTF_8);assertEquals(40,html.split("<article>",-1).length-1);
        assertTrue(html.contains("ROW-0</p>"));assertTrue(html.contains("ROW-39</p>"));assertTrue(html.contains("完整历史未确认"));
        assertEquals("640",js("document.getElementById('history').scrollTop"));
        assertEquals("undefined",js("typeof window.__novaHistoryScrollTrial"));js("clearInterval(window.fixtureJitter);true");
    }
    @Test public void delayedHistorySpinnerWaitsThenSavesAllCachedMessages() throws Exception {
        virtualHistory();String original=js("document.getElementById('history').scrollTop");
        js("const loader=document.createElement('div');loader.id='fixture-loader';loader.setAttribute('role','progressbar');loader.style='position:sticky;bottom:0;height:10px;width:20px';document.getElementById('history').append(loader);setTimeout(()=>loader.remove(),8000);true");
        AtomicReference<Intent> saved=new AtomicReference<>();
        external(intent->{if(!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) return null;saved.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(OUTPUT));});
        main(()->exporter().start());click("单向扫描历史");
        android.os.SystemClock.sleep(5500);assertTrue("must keep waiting for history",busy());assertNull(output());
        assertEquals("0",js("window.__novaHistoryScrollTrial.messages.size"));
        assertEquals("0",js("window.__novaHistoryScrollTrial.steps"));
        click("选择导出格式");click("HTML 阅读版（推荐）");click("保存到本地…");waitFor("delayed history saved",()->saved.get()!=null && !busy());
        String html=new String(read(OUTPUT),StandardCharsets.UTF_8);assertEquals(40,html.split("<article>",-1).length-1);
        assertTrue(html.contains("ROW-0</p>"));assertTrue(html.contains("ROW-39</p>"));assertTrue(html.contains("完整历史未确认"));
        assertEquals(original,js("document.getElementById('history').scrollTop"));assertEquals("undefined",js("typeof window.__novaHistoryScrollTrial"));
    }
    @Test public void changingBodyStillFailsWithTypedRedactedSettlingDiagnostic() throws Exception {
        virtualHistory();js("let bodyTick=0;window.fixtureBodyChange=setInterval(()=>{document.querySelector('.markdown p').textContent='SECRET-JITTER-'+(++bodyTick);},60);true");
        main(()->exporter().start());click("单向扫描历史");click("复制诊断");assertFalse(busy());assertNull(output());
        AtomicReference<String> detail=new AtomicReference<>();
        main(()->{android.content.ClipboardManager c=activity.getSystemService(android.content.ClipboardManager.class);detail.set(c.getPrimaryClip().getItemAt(0).getText().toString());});
        org.json.JSONObject diag=new org.json.JSONObject(detail.get());assertEquals("H06_UNSETTLED",diag.getString("code"));
        org.json.JSONObject settling=diag.getJSONObject("coverage").getJSONObject("settling");
        assertEquals("body-or-structure-changing",settling.getString("reason"));assertTrue(settling.getInt("bodyChanges")>0);
        assertTrue(diag.has("buildRevision"));assertFalse(detail.get().contains("SECRET-JITTER"));assertFalse(detail.get().contains("g-p-fixture"));
        js("clearInterval(window.fixtureBodyChange);true");assertEquals("undefined",js("typeof window.__novaHistoryScrollTrial"));
    }
    @Test public void historyScrollCancellationReleasesCacheAndAllowsRetry() throws Exception {
        virtualHistory();String original=js("document.getElementById('history').scrollTop");
        main(()->exporter().start());click("单向扫描历史");click("取消采集");waitFor("scan cancelled",()->!busy());
        assertNull(output());assertEquals("undefined",js("typeof window.__novaHistoryScrollTrial"));assertEquals(original,js("document.getElementById('history').scrollTop"));
        main(()->exporter().start());click("单向扫描历史");click("取消采集");waitFor("retry cancelled",()->!busy());
    }
    @Test public void historyMissingIdStopsWithRedactedDiagnostic() throws Exception {
        virtualHistory();js("document.querySelector('[data-message-id]').removeAttribute('data-message-id');true");
        main(()->exporter().start());click("单向扫描历史");click("复制诊断");assertFalse(busy());assertNull(output());
        AtomicReference<String> detail=new AtomicReference<>();
        main(()->{android.content.ClipboardManager c=activity.getSystemService(android.content.ClipboardManager.class);detail.set(c.getPrimaryClip().getItemAt(0).getText().toString());});
        assertTrue(detail.get().contains("H02_MISSING_ID"));
        for(String secret:new String[]{"ROW-","REPEATED-TEXT","m33","g-p-fixture"}) assertFalse(detail.get().contains(secret));
        assertEquals("undefined",js("typeof window.__novaHistoryScrollTrial"));
    }
    @Test public void htmlSavedThroughSafHasDomBodyAndScopeNotice() throws Exception {
        conversation("");
        assertEquals("true",js("typeof window.__novaExportCapture==='undefined'"));
        AtomicReference<Intent> captured=new AtomicReference<>();
        external(intent->{
            if(!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) return null;
            captured.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(OUTPUT));
        });
        export("HTML 阅读版（推荐）");click("保存到本地…");
        waitFor("saved",()->captured.get()!=null && !busy());
        String saved=new String(read(OUTPUT),StandardCharsets.UTF_8);
        assertTrue(saved.contains("USER-FIRST"));assertTrue(saved.contains("ASSISTANT-LAST"));
        assertTrue(saved.contains("CODE-LAST"));assertTrue(saved.contains("TABLE-LAST"));
        assertTrue(saved.contains("完整历史未确认"));assertFalse(saved.contains("prompt-textarea"));
        assertEquals("text/html",captured.get().getType());
    }
    @Test public void explicitProgressTurnSavesInOrderWithRedactedDiscovery() throws Exception {
        conversation("");
        js("document.querySelector('[data-message-id=a]').insertAdjacentHTML('beforebegin','<article data-turn=\"assistant\" data-message-channel=\"commentary\" data-message-id=\"PRIVATE-PROGRESS-ID\"><p>PROGRESS-BODY</p><button>PRIVATE-CONTROL</button></article>');true");
        AtomicReference<Intent> captured=new AtomicReference<>();
        external(intent->{
            if(!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) return null;
            captured.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(OUTPUT));
        });
        export("HTML 阅读版（推荐）");click("保存到本地…");
        waitFor("progress saved",()->captured.get()!=null && !busy());
        String html=new String(read(OUTPUT),StandardCharsets.UTF_8);
        assertTrue(html.contains("助手进度"));assertTrue(html.indexOf("USER-FIRST")<html.indexOf("PROGRESS-BODY"));
        assertTrue(html.indexOf("PROGRESS-BODY")<html.indexOf("ASSISTANT-LAST"));assertFalse(html.contains("PRIVATE-CONTROL"));
        main(()->exporter().showDiagnostic());click("复制诊断");
        AtomicReference<String> detail=new AtomicReference<>();main(()->{
            android.content.ClipboardManager c=activity.getSystemService(android.content.ClipboardManager.class);
            detail.set(c.getPrimaryClip().getItemAt(0).getText().toString());
        });
        org.json.JSONObject dom=new org.json.JSONObject(detail.get()).getJSONObject("dom");
        assertEquals(3,dom.getInt("count"));assertEquals(1,dom.getInt("turnFallbacks"));
        assertEquals(2,dom.getJSONObject("progressDiscovery").getJSONObject("authors").getInt("visibleSupported"));
        for(String secret:new String[]{"PROGRESS-BODY","PRIVATE-PROGRESS-ID","PRIVATE-CONTROL"}) assertFalse(detail.get().contains(secret));
    }
    @Test public void authorlessAssistantSectionWithoutIdSavesInOrder() throws Exception {
        conversation("");
        js("document.querySelector('[data-message-id=a]').insertAdjacentHTML('beforebegin','<section data-testid=\"conversation-turn-3\" data-turn=\"assistant\"><div class=\"markdown\"><p><strong>PROGRESS-BODY</strong></p></div><button>PRIVATE-CONTROL</button></section>');true");
        AtomicReference<Intent> captured=new AtomicReference<>();
        external(intent->{
            if(!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) return null;
            captured.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(OUTPUT));
        });
        export("HTML 阅读版（推荐）");click("保存到本地…");
        waitFor("progress saved",()->captured.get()!=null && !busy());
        String html=new String(read(OUTPUT),StandardCharsets.UTF_8);
        assertTrue(html.contains("公开 ID"));assertTrue(html.indexOf("USER-FIRST")<html.indexOf("PROGRESS-BODY"));
        assertTrue(html.indexOf("PROGRESS-BODY")<html.indexOf("ASSISTANT-LAST"));assertFalse(html.contains("PRIVATE-CONTROL"));
        main(()->exporter().showDiagnostic());click("复制诊断");
        AtomicReference<String> detail=new AtomicReference<>();main(()->{
            android.content.ClipboardManager c=activity.getSystemService(android.content.ClipboardManager.class);
            detail.set(c.getPrimaryClip().getItemAt(0).getText().toString());
        });
        org.json.JSONObject dom=new org.json.JSONObject(detail.get()).getJSONObject("dom");
        assertEquals(1,dom.getInt("missingIds"));assertEquals(3,dom.getInt("count"));assertEquals(1,dom.getInt("turnFallbacks"));
        assertEquals(2,dom.getJSONObject("progressDiscovery").getJSONObject("authors").getInt("visibleSupported"));
        for(String secret:new String[]{"PROGRESS-BODY","PRIVATE-PROGRESS-ID","PRIVATE-CONTROL"}) assertFalse(detail.get().contains(secret));
    }
    @Test public void locatorCopiesOnlyStructureForVisibleAuthorSibling() throws Exception {
        conversation("");
        js("document.querySelector('[data-message-id=a]').insertAdjacentHTML('beforebegin','<section><p>LOCATOR-PRIVATE-TARGET</p></section>');true");
        main(()->exporter().locateText("LOCATOR-PRIVATE-TARGET"));click("复制诊断");
        AtomicReference<String> detail=new AtomicReference<>();main(()->{
            android.content.ClipboardManager c=activity.getSystemService(android.content.ClipboardManager.class);
            detail.set(c.getPrimaryClip().getItemAt(0).getText().toString());
        });
        org.json.JSONObject result=new org.json.JSONObject(detail.get()).getJSONObject("locator");
        assertEquals("L00_MATCH",result.getString("code"));assertFalse(result.getBoolean("bodyIncluded"));
        org.json.JSONObject match=result.getJSONArray("matches").getJSONObject(0);
        assertEquals(0,match.getInt("authorIndex"));assertFalse(match.getJSONObject("visibility").getBoolean("cssHidden"));
        assertFalse(detail.get().contains("LOCATOR-PRIVATE-TARGET"));assertFalse(busy());assertNull(output());
    }
    @Test public void boxlessBodyWrappersStillSaveTextAndCode() throws Exception {
        conversation("");
        js("document.querySelector('[data-message-author-role=assistant]').style.display='contents';document.querySelector('.markdown').style.display='contents';true");
        AtomicReference<Intent> captured=new AtomicReference<>();
        external(intent->{
            if(!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) return null;
            captured.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(OUTPUT));
        });
        export("HTML 阅读版（推荐）");click("保存到本地…");
        waitFor("boxless body saved",()->captured.get()!=null && !busy());
        String saved=new String(read(OUTPUT),StandardCharsets.UTF_8);
        assertTrue(saved.contains("USER-FIRST"));assertTrue(saved.contains("ASSISTANT-LAST"));assertTrue(saved.contains("CODE-LAST"));
    }
    @Test public void genuineEmptyBodyFailsWithRedactedMessagePosition() throws Exception {
        conversation("");
        js("document.body.insertAdjacentHTML('beforeend','<article data-message-id=\"private-node-id\"><div data-message-author-role=\"assistant\"><div class=\"markdown\"><button>PRIVATE-BUTTON</button><p hidden>PRIVATE-HIDDEN</p></div></div></article>');true");
        main(()->exporter().start());click("快速导出已加载消息");
        waitFor("empty-body failure",()->findControl(instrument.getUiAutomation().getRootInActiveWindow(),"试用导出失败")!=null);
        assertFalse(busy());assertNull(output());click("复制诊断");
        AtomicReference<String> detail=new AtomicReference<>();
        main(()->{android.content.ClipboardManager c=activity.getSystemService(android.content.ClipboardManager.class);detail.set(c.getPrimaryClip().getItemAt(0).getText().toString());});
        org.json.JSONObject copied=new org.json.JSONObject(detail.get());
        assertEquals("D09_EMPTY_BODY",copied.getString("code"));
        org.json.JSONObject position=copied.getJSONObject("dom").getJSONObject("failedMessage");
        assertEquals(3,position.getInt("index"));assertEquals("assistant",position.getString("role"));
        for(String secret:new String[]{"private-node-id","PRIVATE-BUTTON","PRIVATE-HIDDEN","USER-FIRST","g-p-fixture"}) assertFalse(detail.get().contains(secret));
    }
    @Test public void markdownCanCancelThenShareReadableFile() throws Exception {
        conversation("");AtomicReference<Intent> captured=new AtomicReference<>();
        external(intent->{
            if(Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);
            if(Intent.ACTION_CHOOSER.equals(intent.getAction())) {captured.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);}
            return null;
        });
        export("Markdown");File first=output();click("保存到本地…");waitFor("cancelled",()->!busy());
        export("Markdown");File second=output();assertNotEquals(first.getName(),second.getName());click("分享文件");
        waitFor("share",()->captured.get()!=null);Intent send=captured.get().getParcelableExtra(Intent.EXTRA_INTENT);
        assertEquals("text/markdown",send.getType());
        assertTrue((send.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
        Uri uri=send.getParcelableExtra(Intent.EXTRA_STREAM);String text=new String(read(uri),StandardCharsets.UTF_8);
        assertTrue(text.contains("USER-FIRST"));assertTrue(text.contains("CODE-LAST"));assertTrue(text.contains("TABLE-LAST"));assertTrue(text.contains("完整历史未确认"));
    }
    @Test public void streamingErrorHasCopyableRedactedDiagnostic() throws Exception {
        conversation("");js("document.body.insertAdjacentHTML('beforeend','<button data-testid=\"stop-button\">stop</button>');true");
        main(()->exporter().start());click("快速导出已加载消息");
        waitFor("failure dialog",()->findControl(instrument.getUiAutomation().getRootInActiveWindow(),"试用导出失败")!=null);
        assertFalse(busy());assertNull(output());click("复制诊断");
        AtomicReference<String> detail=new AtomicReference<>();
        main(()->{android.content.ClipboardManager c=activity.getSystemService(android.content.ClipboardManager.class);detail.set(c.getPrimaryClip().getItemAt(0).getText().toString());});
        assertTrue(detail.get().contains("D04_STREAMING"));assertTrue(detail.get().contains("not-proven"));
        assertFalse(detail.get().contains("USER-FIRST"));assertFalse(detail.get().contains("g-p-fixture"));
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
        File local=new File(activity.getExternalFilesDir(null),"printed-export.pdf");
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
}
