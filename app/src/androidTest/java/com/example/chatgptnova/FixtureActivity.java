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
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

abstract class FixtureActivity {
    static final String PAGE = "https://chatgpt.com/nova-fixture";
    static final Uri OUTPUT = Uri.parse("content://com.example.chatgptnova.test.documents/output.bin");
    final Instrumentation instrument = InstrumentationRegistry.getInstrumentation();
    ActivityScenario<MainActivity> scenario;
    MainActivity activity;
    WebView web;
    volatile boolean retryFixture;

    void start() {
        scenario = ActivityScenario.launch(new Intent(instrument.getTargetContext(), MainActivity.class));
        scenario.onActivity(value -> { activity = value; web = web(value); });
        fixture(PAGE);
    }

    static WebView web(MainActivity activity) {
        try { Field field = MainActivity.class.getDeclaredField("webView"); field.setAccessible(true); return (WebView) field.get(activity); }
        catch (Exception error) { throw new AssertionError(error); }
    }

    void main(Runnable action) { instrument.runOnMainSync(action); }

    void fixture(String address) {
        main(() -> {
            web.stopLoading();
            WebViewClient original = web.getWebViewClient();
            web.setWebViewClient(new WebViewClient() {
                @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    // Served locally inside instrumentation, without requesting chatgpt.com or reading credentials.
                    String path=request.getUrl().getPath();
                    if ((path!=null && path.startsWith("/nova-fixture"))
                            || (retryFixture && "127.0.0.1".equals(request.getUrl().getHost()) && "/network-error".equals(path))) {
                        return new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream(HTML.getBytes(StandardCharsets.UTF_8)));
                    }
                    return original.shouldInterceptRequest(view, request);
                }
                @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return original.shouldOverrideUrlLoading(view, request); }
                @Override public void onPageStarted(WebView view, String url, Bitmap icon) { original.onPageStarted(view,url,icon); }
                @Override public void onPageFinished(WebView view, String url) { original.onPageFinished(view,url); }
                @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) { original.onReceivedError(view,request,error); }
            });
            web.loadUrl(address);
        });
        waitFor("fixture loaded", () -> "ready".equals(js("document.getElementById('ready')?.textContent")));
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
}
