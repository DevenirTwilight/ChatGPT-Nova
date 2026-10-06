package com.example.chatgptnova;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Temporary feasibility probes, not a conversation exporter. No JS bridge or fetch hook. */
final class ExportFeasibilityProbe {
    private static final String TAG = "NovaFeasibility";
    private static final int SAVE_PROBE = 0x6301;

    private static void requireUiThread() {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("Call probe on UI thread");
    }

    static void snapshot(Activity activity, WebView web, BooleanSupplier current) {
        requireUiThread();
        if (web == null || activity.isFinishing() || activity.isDestroyed()
                || !current.getAsBoolean()) return;
        final String address = web.getUrl();
        Uri uri = Uri.parse(address == null ? "" : address);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"chatgpt.com".equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)) return;

        final String script;
        try (InputStream in = activity.getAssets().open("feasibility/dom-probe.js")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) bytes.write(buffer, 0, read);
            script = bytes.toString(StandardCharsets.UTF_8.name());
        } catch (Exception error) {
            Log.e(TAG, "DOM probe asset unavailable", error);
            return;
        }

        Handler main = new Handler(Looper.getMainLooper());
        long start = android.os.SystemClock.elapsedRealtime();
        boolean[] finished = {false};
        Runnable timeout = () -> {
            if (finished[0]) return;
            finished[0] = true;
            Log.w(TAG, "DOM callback timeout; provider may be stalled");
        };
        main.postDelayed(timeout, 8000);
        try {
            web.evaluateJavascript(script, value -> {
                if (finished[0]) return;
                finished[0] = true;
                main.removeCallbacks(timeout);
                if (activity.isFinishing() || activity.isDestroyed()
                        || !current.getAsBoolean() || !Objects.equals(address, web.getUrl())) {
                    Log.i(TAG, "DOM result discarded: page replaced or navigated");
                    return;
                }
                try {
                    Object decoded = new JSONTokener(value).nextValue();
                    if (!(decoded instanceof String)) throw new IllegalStateException();
                    JSONObject report = new JSONObject((String) decoded);
                    report.put("callbackMain", Looper.myLooper() == Looper.getMainLooper());
                    report.put("elapsedMs", android.os.SystemClock.elapsedRealtime() - start);
                    Log.i(TAG, report.toString());
                } catch (Exception error) { Log.e(TAG, "DOM result decoding failed", error); }
            });
        } catch (RuntimeException error) {
            finished[0] = true;
            main.removeCallbacks(timeout);
            Log.e(TAG, "DOM evaluation failed", error);
        }
    }

    static PdfSession startPdf(Activity activity) {
        requireUiThread();
        PdfSession session = new PdfSession(activity);
        session.start();
        return session;
    }

    static final class PdfSession implements AutoCloseable {
        private final Activity activity;
        private final Handler main = new Handler(Looper.getMainLooper());
        private WebView web;
        private PrintJob job;
        private boolean closed, requested;
        private final Runnable deadline = () -> {
            Log.e(TAG, "PDF render timeout");
            close();
        };

        PdfSession(Activity activity) { this.activity = activity; }
        boolean isClosed() { return closed; }

        private void start() {
            if (activity.isFinishing() || activity.isDestroyed()) { close(); return; }
            try {
                web = new WebView(activity);
                web.setFocusable(false);
                web.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                ViewGroup root = activity.findViewById(android.R.id.content);
                root.addView(web, 0, new ViewGroup.LayoutParams(-1, -1));
                web.getSettings().setJavaScriptEnabled(false);
                web.getSettings().setAllowFileAccess(false);
                web.getSettings().setAllowContentAccess(false);
                web.getSettings().setBlockNetworkLoads(true);
                web.setWebViewClient(new WebViewClient() {
                    @Override public void onPageFinished(WebView view, String url) {
                        if (closed || requested || view != web) return;
                        requested = true;
                        view.postVisualStateCallback(1, new WebView.VisualStateCallback() {
                            @Override public void onComplete(long requestId) {
                                if (!closed && view == web) print();
                            }
                        });
                    }
                });
                StringBuilder html = new StringBuilder("<!doctype html><meta charset='utf-8'>"
                    + "<style>body{font:14px sans-serif}pre{white-space:pre-wrap;overflow-wrap:anywhere}"
                    + "table{border-collapse:collapse}td{border:1px solid;padding:4px}</style>"
                    + "<h1>PDF-FIRST 中文测试</h1>");
                for (int i = 1; i <= 100; i++)
                    html.append("<p>段落 ").append(i).append("：检查顺序和分页。</p>");
                html.append("<pre>code-line-1\ncode-line-2\n");
                for (int i = 0; i < 80; i++) html.append("LONG-CODE-");
                html.append("-CODE-LAST</pre>")
                    .append("<table><tr><td>中文</td><td>table-last</td></tr></table>")
                    .append("<a href='https://example.org/pdf-probe'>PDF link</a>")
                    .append("<p>PDF-LAST</p>");
                main.postDelayed(deadline, 15000);
                web.loadDataWithBaseURL("https://nova-probe.invalid/", html.toString(),
                        "text/html", "UTF-8", null);
            } catch (RuntimeException error) {
                Log.e(TAG, "PDF setup failed", error);
                close();
            }
        }

        private void print() {
            if (activity.isFinishing() || activity.isDestroyed()) { close(); return; }
            try {
                PrintManager manager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
                if (manager == null) throw new IllegalStateException("No print service");
                final PrintDocumentAdapter delegate = web.createPrintDocumentAdapter("Nova-PDF-Probe");
                PrintDocumentAdapter adapter = new PrintDocumentAdapter() {
                    @Override public void onStart() { delegate.onStart(); }
                    @Override public void onLayout(PrintAttributes oldA, PrintAttributes newA,
                            CancellationSignal signal, LayoutResultCallback callback,
                            android.os.Bundle extras) {
                        delegate.onLayout(oldA, newA, signal, callback, extras);
                    }
                    @Override public void onWrite(PageRange[] pages, ParcelFileDescriptor output,
                            CancellationSignal signal, WriteResultCallback callback) {
                        delegate.onWrite(pages, output, signal, callback);
                    }
                    @Override public void onFinish() {
                        try { delegate.onFinish(); }
                        finally { main.post(() -> { job = null; close(); }); }
                    }
                };
                job = manager.print("Nova-PDF-Probe", adapter,
                        new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
                main.removeCallbacks(deadline);
                Log.i(TAG, "PDF dialog opened; actual saved file still needs inspection");
            } catch (RuntimeException error) {
                Log.e(TAG, "PDF print failed", error);
                close();
            }
        }

        @Override public void close() {
            requireUiThread();
            if (closed) return;
            closed = true;
            main.removeCallbacks(deadline);
            if (job != null) { job.cancel(); job = null; }
            if (web != null) {
                if (web.getParent() instanceof ViewGroup) ((ViewGroup) web.getParent()).removeView(web);
                web.stopLoading();
                web.destroy();
                web = null;
            }
        }
    }

    static void startSaf(Activity activity) {
        requireUiThread();
        activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain")
            .putExtra(Intent.EXTRA_TITLE, "Nova-SAF-Probe.txt"), SAVE_PROBE);
    }

    static boolean safResult(Activity activity, int code, int result, Intent data) {
        requireUiThread();
        if (code != SAVE_PROBE) return false;
        if (result != Activity.RESULT_OK || data == null || data.getData() == null) {
            Log.i(TAG, "SAF cancelled; existing download state untouched");
            return true;
        }
        Uri destination = data.getData();
        if (!"content".equalsIgnoreCase(destination.getScheme())) {
            Log.e(TAG, "SAF invalid scheme"); return true;
        }
        new Thread(() -> {
            byte[] expected = "Nova-SAF-Probe 中文\nUTF-8 round trip\n".getBytes(StandardCharsets.UTF_8);
            try {
                try (OutputStream out = activity.getContentResolver().openOutputStream(destination, "wt")) {
                    if (out == null) throw new java.io.IOException("No output stream");
                    out.write(expected);
                }
                try (InputStream in = activity.getContentResolver().openInputStream(destination)) {
                    if (in == null) throw new java.io.IOException("No input stream");
                    ByteArrayOutputStream got = new ByteArrayOutputStream();
                    int b;
                    while ((b = in.read()) != -1 && got.size() < 1024) got.write(b);
                    Log.i(TAG, "SAF roundTrip=" + Arrays.equals(expected, got.toByteArray()));
                }
            } catch (Exception error) { Log.e(TAG, "SAF round trip failed", error); }
        }, "nova-feasibility-io").start();
        return true;
    }
}
