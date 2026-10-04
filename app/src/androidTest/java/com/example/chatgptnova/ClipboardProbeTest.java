package com.example.chatgptnova;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;
import android.widget.PopupMenu;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Baseline: real WebView paste commands, not JS setting a draft or synthesizing paste. */
public final class ClipboardProbeTest extends FixtureActivity {
    private final JSONArray results = new JSONArray();
    private String route;
    @Before public void before() { clipboardFixture=true; start(); }
    @After public void after() throws Exception {
        PackageInfo provider=WebView.getCurrentWebViewPackage();
        JSONObject report=new JSONObject().put("api",Build.VERSION.SDK_INT).put("route",route)
                .put("webview",provider==null ? "unknown" : provider.packageName+" "+provider.versionName)
                .put("page","test-APK-owned textarea and contenteditable; not a real ChatGPT session").put("cases",results);
        File dir=new File(instrument.getTargetContext().getExternalFilesDir(null),"clipboard-probe");
        assertTrue(dir.isDirectory() || dir.mkdirs());
        try(FileOutputStream out=new FileOutputStream(new File(dir,route+".json"))) { out.write(report.toString(2).getBytes(StandardCharsets.UTF_8)); }
        if(scenario!=null)scenario.close();
    }

    @Test public void longPressSystemPaste() throws Exception { route="long-press"; matrix(); }
    @Test public void imePasteCommand() throws Exception { route="ime-context-paste"; matrix(); }
    @Test public void nativeMenuPaste() throws Exception { route="native-menu"; matrix(); }
    @Test public void imeCommitText() throws Exception { route="ime-commit-text"; matrix(); }
    @Test public void imeCommitTextWithAttributes() throws Exception {
        route="ime-commit-text-attributed";
        org.junit.Assume.assumeTrue(Build.VERSION.SDK_INT >= 33);
        matrix();
    }

    private void matrix() throws Exception {
        int failures=0;
        String[][] cases={{"short-chinese","你好 😀"},{"short-multiline","第一行\n第二行"},
                {"CRLF","第一行\r\n第二行\r第三行"},
                {"1KB",repeat("a",1024)},{"10KB",repeat("b",10240)},{"50KB",repeat("c",51200)},
                {"multiline-1KB",repeat("中文段落 English\n\n第二段 😀\n",1024)+"结束 END"},
                {"multiline-10KB",repeat("one\n\ntwo\nthree\n",10240)+"结束 END"},
                {"multiline-50KB",repeat("中文段落 English\n\n第二段 😀\n",51200)+"结束 END"},
                {"markdown",repeat("# Heading\n\n- **bold**\n> quote\n```java\nString s = \"<script> & \\ path\";\n```\n",10240)+"结束 END"},
                {"trailing-newlines",repeat("中文\n\n",1024)},
                {"unicode-emoji",repeat("中文 English 👩🏽‍💻 🇨🇳 😀 e\u0301 \u2028 \u2029\n\n",51200)+"结束 END"}};
        for(String id:new String[]{"mobile-composer-prompt","prompt-textarea"})for(String[] item:cases) {
            String source=item[1];
            String expected=source.replace("\r\n","\n").replace('\r','\n');
            js("(()=>{let e=document.getElementById('"+id+"');if(e.tagName==='TEXTAREA')e.value='';else e.textContent='';pasteEvents=[];inputEvents=[];e.scrollTop=0;})()");
            clickWeb(id);
            waitFor("focused editor",()->id.equals(js("document.activeElement.id")));
            main(()->((ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("synthetic paste fixture",source)));
            // Chromium receives clipboard-change notifications asynchronously.
            // Wait for the OS value and then allow its renderer clipboard cache to update.
            waitFor("clipboard published", () -> {
                AtomicReference<String> value = new AtomicReference<>();
                main(() -> { ClipData clip = ((ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip();
                    value.set(clip == null ? null : String.valueOf(clip.getItemAt(0).getText())); });
                return source.equals(value.get());
            });
            SystemClock.sleep(400);
            if("long-press".equals(route))longPress(id);
            else if("native-menu".equals(route)) menuPaste();
            else input(source,"ime-context-paste".equals(route));
            // Observe the asynchronous paste result without repeating the action.
            long until = SystemClock.uptimeMillis() + 5000;
            String actual;
            do {
                SystemClock.sleep(100);
                actual=js("(()=>{let e=document.getElementById('"+id+"');return e.tagName==='TEXTAREA'?e.value:e.innerText})()");
                if (expected.equals(actual)) break;
            } while (SystemClock.uptimeMillis() < until);
            SystemClock.sleep(200);
            actual=js("(()=>{let e=document.getElementById('"+id+"');return e.tagName==='TEXTAREA'?e.value:e.innerText})()");
            JSONObject result=new JSONObject().put("case",item[0]).put("editor",id).put("utf8Bytes",expected.getBytes(StandardCharsets.UTF_8).length)
                    .put("utf16Units",expected.length()).put("actualUnits",actual.length()).put("exactMatch",expected.equals(actual))
                    .put("pasteEvents",new JSONArray(js("JSON.stringify(pasteEvents)"))).put("inputEvents",new JSONArray(js("JSON.stringify(inputEvents)"))).put("htmlTail", js("document.getElementById('"+id+"').innerHTML.slice(-200)"));
            results.put(result);
            System.out.println("NOVA_CLIPBOARD_CASE " + result.toString());
            if (!expected.equals(actual)) System.out.println("NOVA_CLIPBOARD_TAIL " + new JSONObject().put("expected", expected.substring(Math.max(0, expected.length()-80))).put("actual", actual.substring(Math.max(0, actual.length()-80))));
            if(!expected.equals(actual))failures++;
        }
        assertEquals(route+": full-text paste failures (see clipboard-probe JSON)",0,failures);
    }

    private void menuPaste() throws Exception {
        Method show = MainActivity.class.getDeclaredMethod("showMenu");
        show.setAccessible(true);
        Field field = MainActivity.class.getDeclaredField("overflowMenu");
        field.setAccessible(true);
        PopupMenu previous = (PopupMenu) field.get(activity);
        main(() -> { try { show.invoke(activity); } catch(Exception e) { throw new AssertionError(e); } });
        AtomicReference<PopupMenu> menu = new AtomicReference<>();
        waitFor("native paste menu", () -> {
            main(() -> { try { menu.set((PopupMenu)field.get(activity)); } catch(Exception e) { throw new AssertionError(e); } });
            return menu.get() != null && menu.get() != previous && menu.get().getMenu().findItem(6) != null;
        });
        main(() -> { assertTrue(menu.get().getMenu().performIdentifierAction(6,0)); menu.get().dismiss(); });
    }

    private void input(String text,boolean paste) throws Exception {
        input(connection -> {
            if (paste) assertTrue(connection.performContextMenuAction(android.R.id.paste));
            else if ("ime-commit-text-attributed".equals(route))
                assertTrue(connection.commitText(text,1,new android.view.inputmethod.TextAttribute.Builder().build()));
            else assertTrue(connection.commitText(text,1));
        });
    }

    private void input(java.util.function.Consumer<InputConnection> action) throws Exception {
        AtomicReference<InputConnection> ref=new AtomicReference<>();
        main(()->ref.set(web.onCreateInputConnection(new EditorInfo())));
        InputConnection connection=ref.get();assertNotNull(connection);
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();
        Runnable operation=()->{try{action.accept(connection);}catch(Throwable failure){error.set(failure);}finally{done.countDown();}};
        Handler handler=connection.getHandler();
        if(handler!=null)handler.post(operation);else operation.run();
        assertTrue("IME command completes",done.await(15,TimeUnit.SECONDS));
        if(error.get()!=null)throw new AssertionError("IME command failed",error.get());
    }

    @Test public void selectionAndFollowingTypingStayInOrder() throws Exception {
        route="ime-ordering";
        for(String id:new String[]{"mobile-composer-prompt","prompt-textarea"}) {
            clickWeb(id);
            input(connection -> assertTrue(connection.commitText("prefix old suffix",1)));
            waitFor("initial draft",()->"prefix old suffix".equals(editorText(id)));
            input(connection -> {
                assertTrue(connection.beginBatchEdit());
                assertTrue(connection.setSelection(7,10));
                assertTrue(connection.commitText("中文\n多行 😀",1));
                assertTrue(connection.commitText("!",1));
                assertTrue(connection.endBatchEdit());
            });
            expectText(id,"prefix 中文\n多行 😀! suffix","selection replacement followed by typing");
        }
    }

    @Test public void chineseCompositionKeepsNativeReplacement() throws Exception {
        route="ime-composition";
        for(String id:new String[]{"mobile-composer-prompt","prompt-textarea"}) {
            clickWeb(id);
            input(connection -> {
                assertTrue(connection.setComposingText("ni",1));
                assertTrue(connection.setComposingText("你",1));
                assertTrue(connection.commitText("你好",1));
            });
            expectText(id,"你好","Chinese composition replaced");
        }
    }

    @Test public void ordinaryFieldsUseNativeFallback() throws Exception {
        route="ime-native-fallback";
        for(String type:new String[]{"textarea","password"}) {
            js("(()=>{let e=document.createElement('"+("textarea".equals(type)?"textarea":"input")+"');e.id='ordinary-field';e.type='"+type+"';document.body.prepend(e)})()");
            clickWeb("ordinary-field");
            waitFor("focused ordinary field",()->"ordinary-field".equals(js("document.activeElement.id")));
            input(connection -> assertTrue(connection.commitText("fallback\n中文",1)));
            String expected="textarea".equals(type)?"fallback\n中文":"fallback中文";
            expectText("ordinary-field",expected,"native fallback in "+type);
            js("document.getElementById('ordinary-field').remove()");
        }
    }

    @Test public void navigationCancelsPendingCommit() throws Exception {
        route="ime-cancelled";
        for(boolean recreate:new boolean[]{false,true}) {
            clickWeb("mobile-composer-prompt");
            AtomicReference<InputConnection> ref=new AtomicReference<>();
            main(()->{
                InputConnection connection=web.onCreateInputConnection(new EditorInfo());
                ref.set(connection);
                assertTrue(connection.commitText("old document\nclipboard",1));
                if(recreate) assertNotSame(connection,web.onCreateInputConnection(new EditorInfo()));
                else ((NovaWebView)web).invalidateInputConnection();
            });
            instrument.waitForIdleSync();
            assertEquals("",editorText("mobile-composer-prompt"));
            assertFalse(ref.get().commitText("late stale command",1));
        }
    }

    private String editorText(String id) {
        return js("(()=>{let e=document.getElementById('"+id+"');return e.tagName==='TEXTAREA'||e.tagName==='INPUT'?e.value:e.innerText})()");
    }

    private void expectText(String id,String expected,String label) {
        try { waitFor(label,()->expected.equals(editorText(id))); }
        catch(AssertionError failure) {
            assertEquals(label+" ("+id+")",expected,editorText(id));
            throw failure;
        }
    }

    private void longPress(String id) {
        String[] coords=js("(()=>{let r=document.getElementById('"+id+"').getBoundingClientRect();return [r.x+16,r.y+20].join(',')})()").split(",");
        int[] at=new int[2];main(()->web.getLocationOnScreen(at));float density=activity.getResources().getDisplayMetrics().density;
        float x=at[0]+Float.parseFloat(coords[0])*density,y=at[1]+Float.parseFloat(coords[1])*density;
        long downAt=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(downAt,downAt,MotionEvent.ACTION_DOWN,x,y,0);
        instrument.sendPointerSync(down);down.recycle();SystemClock.sleep(900);
        MotionEvent up=MotionEvent.obtain(downAt,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,y,0);instrument.sendPointerSync(up);up.recycle();
        waitFor("system Paste action",()->{
            AccessibilityNodeInfo node=pasteNode(instrument.getUiAutomation().getRootInActiveWindow());
            if (node == null) for (android.view.accessibility.AccessibilityWindowInfo window : instrument.getUiAutomation().getWindows()) {
                node = pasteNode(window.getRoot()); if (node != null) break;
            }
            while(node!=null&&!node.isClickable())node=node.getParent();
            return node!=null&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        });
    }

    private static AccessibilityNodeInfo pasteNode(AccessibilityNodeInfo node) {
        if(node==null||!node.isVisibleToUser())return null;
        String text=node.getText()==null?"":node.getText().toString();
        if("Paste".equalsIgnoreCase(text)||"粘贴".equals(text))return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=pasteNode(node.getChild(i));if(found!=null)return found;}
        return null;
    }

    private static String repeat(String unit,int bytes) {
        StringBuilder out=new StringBuilder();int size=unit.getBytes(StandardCharsets.UTF_8).length;
        for(int written=0;written<bytes;written+=size)out.append(unit);
        return out.toString();
    }
}
