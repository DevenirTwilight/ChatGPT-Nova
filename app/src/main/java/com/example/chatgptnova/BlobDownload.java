package com.example.chatgptnova;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.webkit.WebView;
import android.widget.Toast;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Saves only a user-requested blob through the system document picker.
 * No JavaScript interface, cookie transfer, credential inspection, or server proxy.
 * FileReader reads bounded slices instead of encoding an entire file in memory.
 */
final class BlobDownload {
    private static final int CHUNK = 64 * 1024;
    private static final long MAX_SIZE = 256L * 1024 * 1024;
    private final Activity activity;
    private final WebView web;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final String key = JSONObject.quote("novaDownload_" + UUID.randomUUID().toString().replace("-", ""));
    private final String host;
    private OutputStream output;
    private Runnable completion;
    private long size;
    private long offset;
    private boolean finished;

    BlobDownload(Activity activity, WebView web, String url) {
        this.activity = activity;
        this.web = web;
        host = Uri.parse(web.getUrl()).getHost();
        // Fetch is restricted to the exact blob URL clicked by the user on chatgpt.com.
        web.evaluateJavascript("(()=>{let s=window[" + key + "]={state:'loading'};"
                + "fetch(" + JSONObject.quote(url) + ").then(r=>{if(!r.ok)throw Error();return r.blob()})"
                + ".then(b=>{if(b.size>" + MAX_SIZE + ")throw Error();s.blob=b;s.state='ready'})"
                + ".catch(()=>s.state='error')})()", null);
    }

    void saveTo(Uri destination, Runnable completion) {
        this.completion = completion;
        if (!"content".equalsIgnoreCase(destination.getScheme())) { finish(false); return; }
        io.execute(() -> {
            try {
                output = activity.getContentResolver().openOutputStream(destination, "w");
                if (output == null) throw new java.io.IOException();
                main.post(() -> pollReady(0));
            } catch (Exception e) { main.post(() -> finish(false)); }
        });
    }

    private boolean samePage() {
        if (finished || activity.isDestroyed()) return false;
        String current = web.getUrl();
        Uri uri = current == null ? null : Uri.parse(current);
        return MainActivity.isTrustedOrigin(uri) && host.equalsIgnoreCase(uri.getHost());
    }

    private JSONObject decode(String value) throws Exception {
        Object result = new JSONTokener(value).nextValue();
        if (!(result instanceof String)) throw new java.io.IOException();
        return new JSONObject((String) result);
    }

    private void pollReady(int attempts) {
        if (!samePage() || attempts > 120) { finish(false); return; }
        web.evaluateJavascript("(()=>{let s=window[" + key + "];return s?JSON.stringify({state:s.state,size:s.blob?s.blob.size:0}):null})()", result -> {
            if (finished) return;
            try {
                JSONObject state = decode(result);
                if ("loading".equals(state.getString("state"))) {
                    main.postDelayed(() -> pollReady(attempts + 1), 250);
                } else if ("ready".equals(state.getString("state"))) {
                    size = state.getLong("size");
                    if (size < 0 || size > MAX_SIZE) { finish(false); return; }
                    nextChunk();
                } else finish(false);
            } catch (Exception e) { finish(false); }
        });
    }

    private void nextChunk() {
        if (!samePage()) { finish(false); return; }
        if (offset >= size) { finish(true); return; }
        web.evaluateJavascript("(()=>{let s=window[" + key + "];if(!s)return;delete s.chunk;"
                + "s.state='reading';let r=new FileReader();r.onload=()=>{s.chunk=r.result.split(',')[1];s.state='chunk'};"
                + "r.onerror=()=>s.state='error';r.readAsDataURL(s.blob.slice(" + offset + ","
                + Math.min(offset + CHUNK, size) + "))})()", unused -> pollChunk(0));
    }

    private void pollChunk(int attempts) {
        if (!samePage() || attempts > 120) { finish(false); return; }
        web.evaluateJavascript("(()=>{let s=window[" + key + "];return s?JSON.stringify({state:s.state,chunk:s.chunk||''}):null})()", result -> {
            if (finished) return;
            try {
                JSONObject state = decode(result);
                if ("reading".equals(state.getString("state"))) {
                    main.postDelayed(() -> pollChunk(attempts + 1), 100);
                    return;
                }
                if (!"chunk".equals(state.getString("state"))) { finish(false); return; }
                byte[] bytes = Base64.decode(state.getString("chunk"), Base64.DEFAULT);
                if (bytes.length != Math.min(CHUNK, size - offset)) { finish(false); return; }
                io.execute(() -> {
                    try {
                        output.write(bytes);
                        main.post(() -> { if (!finished) { offset += bytes.length; nextChunk(); } });
                    } catch (Exception e) { main.post(() -> finish(false)); }
                });
            } catch (Exception e) { finish(false); }
        });
    }

    private void finish(boolean success) {
        if (finished) return;
        finished = true;
        main.removeCallbacksAndMessages(null);
        try { web.evaluateJavascript("delete window[" + key + "]", null); } catch (RuntimeException ignored) { }
        io.execute(() -> {
            boolean saved = success;
            try { if (output != null) output.close(); } catch (Exception e) { saved = false; }
            final boolean completed = saved;
            if (completion != null) main.post(() -> {
                if (!activity.isDestroyed()) Toast.makeText(activity,
                        completed ? "下载已保存。" : "下载失败，可刷新重试或在系统浏览器中下载。", Toast.LENGTH_LONG).show();
                completion.run();
            });
        });
        io.shutdown();
    }

    void cancel() { finish(false); }
}
