package com.example.chatgptnova;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONTokener;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

abstract class FixtureActivity {
    static final String PAGE = "https://chatgpt.com/nova-fixture";
    static final String CLIPBOARD_REFERENCE = "https://nova-paste-baseline.invalid/nova-fixture";
    static final Uri OUTPUT = Uri.parse("content://com.example.chatgptnova.test.documents/output.bin");
    final Instrumentation instrument = InstrumentationRegistry.getInstrumentation();
    ActivityScenario<MainActivity> scenario;
    MainActivity activity;
    WebView web;
    volatile boolean retryFixture;
    volatile boolean loginFixture;
    volatile boolean nativeFixture;
    volatile boolean clipboardFixture;
    volatile String keyguardDismiss = "not-requested";

    void start() {
        // Connect accessibility before showing an editor/toolbar, not midway through a paste.
        android.accessibilityservice.AccessibilityServiceInfo info = instrument.getUiAutomation().getServiceInfo();
        info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        instrument.getUiAutomation().setServiceInfo(info);
        instrument.getUiAutomation().getRootInActiveWindow();
        // UTP can reinstall the app between suites, and a cold emulator can lock
        // after android-ci.sh's one-time wakeup. Wake before every Activity launch.
        shell("input keyevent KEYCODE_WAKEUP");
        scenario = ActivityScenario.launch(new Intent(instrument.getTargetContext(), MainActivity.class));
        scenario.onActivity(value -> {
            activity = value; web = web(value);
            prepareWindow(value);
        });
        fixture(PAGE);
        dismissKeyguard();
        focus();
    }

    private void prepareWindow(MainActivity value) {
        // Applied to each resumed instance, including a cold-emulator recreation.
        value.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(android.os.Build.VERSION.SDK_INT>=27) {
            value.setShowWhenLocked(true);value.setTurnScreenOn(true);
        }
    }

    private void dismissKeyguard() {
        // RESUMED does not prove a window is visible yet. requestDismissKeyguard
        // can fail before its first frame; the old null callback hid that failure.
        waitFor("visible fixture window", () -> {
            AtomicBoolean shown=new AtomicBoolean();
            main(() -> shown.set(activity.getWindow().getDecorView().isAttachedToWindow()
                    && web.isShown() && web.getWidth()>0 && web.getHeight()>0));
            return shown.get();
        });
        main(() -> {
            android.app.KeyguardManager keyguard=activity.getSystemService(android.app.KeyguardManager.class);
            if(keyguard==null || !keyguard.isKeyguardLocked()) { keyguardDismiss="already-unlocked";return; }
            keyguardDismiss="pending";
            keyguard.requestDismissKeyguard(activity,new android.app.KeyguardManager.KeyguardDismissCallback() {
                @Override public void onDismissSucceeded() { keyguardDismiss="succeeded"; }
                @Override public void onDismissCancelled() { keyguardDismiss="cancelled"; }
                @Override public void onDismissError() { keyguardDismiss="error"; }
            });
        });
        // Dismiss the insecure emulator keyguard after Nova is drawn as well as
        // before launch. This does not enter a credential or weaken focus checks.
        shell("wm dismiss-keyguard");
    }

    static WebView web(MainActivity activity) {
        try { Field field = MainActivity.class.getDeclaredField("webView"); field.setAccessible(true); return (WebView) field.get(activity); }
        catch (Exception error) { throw new AssertionError(error); }
    }

    void main(Runnable action) { instrument.runOnMainSync(action); }

    void fixture(String address) {
        AtomicReference<WebView> fixtureView = new AtomicReference<>();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicBoolean loaded = new AtomicBoolean();
        AtomicBoolean evaluating = new AtomicBoolean();
        waitFor("fixture loaded", () -> {
            // A cold emulator can recreate the Activity during its initial
            // configuration update. Bind the fixture to the resumed instance,
            // rather than evaluating a destroyed WebView until timeout.
            scenario.onActivity(value -> {
                WebView current = web(value);
                if (current != fixtureView.get()) {
                    activity = value;
                    web = current;
                    prepareWindow(value);
                    fixtureView.set(current);
                    loaded.set(false);
                    evaluating.set(false);
                    result.set(null);
                    loadFixture(address, current, loaded);
                }
                if (loaded.get() && evaluating.compareAndSet(false, true)) {
                    current.evaluateJavascript("location.href === " + org.json.JSONObject.quote(address)
                            + " && document.getElementById('ready')?.textContent === 'ready'", answer -> {
                        if (current == fixtureView.get()) {
                            result.set(answer);
                            evaluating.set(false);
                        }
                    });
                }
            });
            return "true".equals(result.get());
        });
    }

    private void loadFixture(String address, WebView current, AtomicBoolean loaded) {
        web.stopLoading();
        WebViewClient original = web.getWebViewClient();
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Served locally inside instrumentation, without requesting chatgpt.com or reading credentials.
                if (clipboardFixture && (PAGE.equals(request.getUrl().toString())
                        || CLIPBOARD_REFERENCE.equals(request.getUrl().toString()))) {
                    return new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream(CLIPBOARD_HTML.getBytes(StandardCharsets.UTF_8)));
                }
                if (nativeFixture && MainActivity.isTrustedOrigin(request.getUrl())) {
                    return new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream(NATIVE_HTML.getBytes(StandardCharsets.UTF_8)));
                }
                String path=request.getUrl().getPath();
                if ((path!=null && path.startsWith("/nova-fixture"))
                        || (loginFixture && "chatgpt.com".equals(request.getUrl().getHost()) && "/auth/login".equals(path))
                        || (retryFixture && "127.0.0.1".equals(request.getUrl().getHost()) && "/network-error".equals(path))) {
                    return new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream(HTML.getBytes(StandardCharsets.UTF_8)));
                }
                return original.shouldInterceptRequest(view, request);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return original.shouldOverrideUrlLoading(view, request); }
            @Override public void onPageStarted(WebView view, String url, Bitmap icon) { original.onPageStarted(view,url,icon); }
            @Override public void onPageFinished(WebView view, String url) {
                original.onPageFinished(view,url);
                if (view == current && address.equals(url)) loaded.set(true);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) { original.onReceivedError(view,request,error); }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                return original.onRenderProcessGone(view, detail);
            }
        });
        web.loadUrl(address);
    }

    String js(String expression) {
        AtomicReference<String> result = new AtomicReference<>();
        main(() -> web.evaluateJavascript(expression, result::set));
        waitFor("javascript callback", () -> result.get() != null);
        try {
            Object decoded = new JSONTokener(result.get()).nextValue();
            return decoded == org.json.JSONObject.NULL ? "null" : String.valueOf(decoded);
        } catch (Exception error) { throw new AssertionError(error); }
    }

    interface Condition { boolean ready(); }
    static void waitFor(String name, Condition condition) {
        long until = SystemClock.uptimeMillis() + 20000;
        do { if (condition.ready()) return; SystemClock.sleep(100); } while (SystemClock.uptimeMillis() < until);
        fail("Timed out: " + name);
    }

    void clickWeb(String id) {
        focus();
        boolean editor = "true".equals(js("(()=>{const e=document.getElementById('"+id+"');"
                + "e.scrollIntoView({block:'center',inline:'nearest'});"
                + "return e.isContentEditable || e.tagName==='TEXTAREA' || "
                + "(e.tagName==='INPUT' && ['text','search','url','tel','email','password','number'].includes(e.type))})()"));
        if (editor) settleEditorViewport();
        String[] coords = js("(()=>{let r=document.getElementById('"+id+"').getBoundingClientRect();return [r.x+r.width/2,r.y+r.height/2].join(',')})()").split(",");
        int[] screen = new int[2];
        main(() -> web.getLocationOnScreen(screen));
        float density = activity.getResources().getDisplayMetrics().density;
        float x = screen[0] + Float.parseFloat(coords[0]) * density;
        float y = screen[1] + Float.parseFloat(coords[1]) * density;
        long at = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(at,at,MotionEvent.ACTION_DOWN,x,y,0);
        MotionEvent up = MotionEvent.obtain(at,at+60,MotionEvent.ACTION_UP,x,y,0);
        instrument.sendPointerSync(down); instrument.sendPointerSync(up); down.recycle(); up.recycle();
        if (editor) {
            waitFor("focused web editor " + id, () -> "true".equals(js(
                    "(()=>{const e=document.getElementById('"+id+"');return e===document.activeElement"
                    + " || e.contains(document.activeElement)})()")));
            settleEditorViewport();
        }
    }

    // DOM focus can precede the Android input connection and IME resize.
    // Do not start a clipboard timing sample while those frames are changing.
    private void settleEditorViewport() {
        AtomicReference<String> previous = new AtomicReference<>();
        waitFor("stable editor viewport", () -> {
            AtomicReference<String> nativeSize = new AtomicReference<>();
            main(() -> nativeSize.set(web.getWidth() + ":" + web.getHeight()));
            String current = nativeSize.get() + ":" + js(
                    "[innerWidth,innerHeight,visualViewport?.width,visualViewport?.height].join(':')");
            return current.equals(previous.getAndSet(current));
        });
    }

    private void focus() {
        long until=SystemClock.uptimeMillis()+20000;
        do {
            AtomicBoolean ready=new AtomicBoolean();
            main(() -> ready.set(activity.hasWindowFocus() && web.isShown()));
            if(ready.get()) { instrument.waitForIdleSync();return; }
            SystemClock.sleep(100);
        } while(SystemClock.uptimeMillis()<until);
        String details=windowDiagnostics();
        android.util.Log.e("NovaFixture",details);
        fail("Timed out: focused Nova window\n"+details);
    }

    private String shell(String command) {
        try(android.os.ParcelFileDescriptor descriptor=instrument.getUiAutomation().executeShellCommand(command);
            java.io.InputStream input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
            return new String(input.readAllBytes(),StandardCharsets.UTF_8);
        } catch(Exception error) { throw new AssertionError("Fixture shell command failed: "+command,error); }
    }

    private String windowDiagnostics() {
        AtomicReference<String> nativeState=new AtomicReference<>();
        String lifecycle=String.valueOf(scenario.getState());
        main(() -> {
            android.app.KeyguardManager keyguard=activity.getSystemService(android.app.KeyguardManager.class);
            android.os.PowerManager power=activity.getSystemService(android.os.PowerManager.class);
            nativeState.set("scenario="+lifecycle+", task="+activity.getTaskId()
                    +", activityFocus="+activity.hasWindowFocus()+", webShown="+web.isShown()
                    +", webAttached="+web.isAttachedToWindow()+", size="+web.getWidth()+"x"+web.getHeight()
                    +", interactive="+(power!=null && power.isInteractive())
                    +", keyguardLocked="+(keyguard!=null && keyguard.isKeyguardLocked())
                    +", dismiss="+keyguardDismiss);
        });
        StringBuilder result=new StringBuilder(nativeState.get());
        for(android.view.accessibility.AccessibilityWindowInfo window:instrument.getUiAutomation().getWindows()) {
            android.view.accessibility.AccessibilityNodeInfo root=window.getRoot();
            result.append("\nwindow id=").append(window.getId()).append(" type=").append(window.getType())
                    .append(" focused=").append(window.isFocused()).append(" active=").append(window.isActive())
                    .append(" package=").append(root==null ? "null" : root.getPackageName());
        }
        // Only window ownership/state, not page text, is needed for this failure.
        for(String line:shell("dumpsys window windows").split("\n")) {
            if(line.contains("mCurrentFocus=") || line.contains("mFocusedApp=")
                    || line.contains("mObscuringWindow=") || line.contains("mInputMethodWindow="))
                result.append("\n").append(line.trim());
        }
        return result.toString();
    }

    View text(View view, String label) {
        if (view instanceof TextView && label.contentEquals(((TextView)view).getText())) return view;
        if (view instanceof ViewGroup) for (int i=0;i<((ViewGroup)view).getChildCount();i++) {
            View result = text(((ViewGroup)view).getChildAt(i),label); if (result != null) return result;
        }
        return null;
    }

    byte[] read(Uri uri) throws Exception {
        try (java.io.InputStream input = activity.getContentResolver().openInputStream(uri);
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            assertNotNull(input); byte[] buffer = new byte[8192]; int count;
            while ((count=input.read(buffer))!=-1) out.write(buffer,0,count);
            return out.toByteArray();
        }
    }

    static final String HTML = "<!doctype html><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>body{margin:10px}input,button{display:block;width:95%;height:56px;margin-bottom:14px}</style>"
            + "<div id='ready'>ready</div><input id='upload' type='file' multiple accept='.txt,image/png,image/jpeg'>"
            + "<input id='camera' type='file' accept='image/jpeg' capture='environment'>"
            + "<input id='single' type='file' accept='text/plain'><button id='download'>Save fixture</button>"
            + "<script>for(let id of ['upload','camera','single'])document.getElementById(id).onchange=async e=>{"
            + "window[id+'Count']=e.target.files.length;window[id+'Data']=await Promise.all([...e.target.files].map(f=>f.text()));};"
            + "document.getElementById('download').onclick=()=>{let d=new Uint8Array(131089);for(let i=0;i<d.length;i++)d[i]=i%251;"
            + "let a=document.createElement('a');a.href=URL.createObjectURL(new Blob([d],{type:'application/octet-stream'}));"
            + "a.download='nova-fixture.bin';document.body.append(a);a.click();};</script>";

    static final String NATIVE_HTML = "<!doctype html><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>body{font-family:sans-serif;margin:24px;color:#233047}p{line-height:1.6}</style>"
            + "<h2>Nova 原生界面检查</h2><p>这是测试 APK 提供的受控页面，不是真实登录或聊天。</p>"
            + "<div id='ready'>ready</div>";

    static final String CLIPBOARD_HTML = "<!doctype html><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>body{margin:12px;font-family:sans-serif}textarea,[contenteditable]{display:block;box-sizing:border-box;width:100%;height:120px;overflow:auto;white-space:pre-wrap;font-size:16px;border:1px solid #456;padding:8px}</style>"
            + "<div id='ready'>ready</div><label for='mobile-composer-prompt'>Textarea</label><textarea id='mobile-composer-prompt' rows='1'></textarea>"
            + "<p>Contenteditable</p><div id='prompt-textarea' class='ProseMirror' contenteditable='true' role='textbox' aria-label='Fixture composer'></div>"
            + "<script>window.pasteEvents=[];window.inputEvents=[];document.addEventListener('paste',e=>pasteEvents.push({id:e.target.id,length:e.clipboardData.getData('text/plain').length}));"
            + "document.addEventListener('input',e=>inputEvents.push({id:e.target.id,type:e.inputType}));</script>";
}
