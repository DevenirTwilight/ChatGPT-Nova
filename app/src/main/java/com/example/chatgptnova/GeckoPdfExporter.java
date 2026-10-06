package com.example.chatgptnova;

import android.app.Activity;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.ViewGroup;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

/** Mozilla's public PDF engine, rendering only the local sanitized export document. */
final class GeckoPdfExporter {
    static final String VERSION = "140.0.20250707120347";
    interface Callback {
        void ready(int pages);
        void failed(String code, Exception error);
    }
    private static GeckoRuntime runtime;
    private final Activity activity;
    private final GeckoSession session;
    private final GeckoView view;
    private volatile boolean closed;
    private boolean requested, documentStarted;

    GeckoPdfExporter(Activity activity, File destination, Callback callback) {
        this.activity=activity;
        if (runtime==null) runtime=GeckoRuntime.create(activity.getApplicationContext(),
            new GeckoRuntimeSettings.Builder().javaScriptEnabled(false)
                .consoleOutput(false).remoteDebuggingEnabled(false).build());
        session=new GeckoSession(new GeckoSessionSettings.Builder()
            .usePrivateMode(true).allowJavascript(false).build());
        view=new GeckoView(activity);
        view.setFocusable(false);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession s, LoadRequest request) {
                return GeckoResult.fromValue(request.uri.startsWith("data:text/html")
                    ? AllowOrDeny.ALLOW : AllowOrDeny.DENY);
            }
            @Override public GeckoResult<GeckoSession> onNewSession(GeckoSession s, String uri) {
                return GeckoResult.fromValue(null);
            }
        });
        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override public void onPageStart(GeckoSession s, String url) {
                documentStarted=url.startsWith("data:text/html");
            }
            @Override public void onPageStop(GeckoSession s, boolean success) {
                if (closed || requested || !documentStarted) return;
                requested=true;
                if (!success) {callback.failed("G02_LOAD",null);return;}
                session.saveAsPdf().then(input -> {
                    if (input==null) {if(!closed) callback.failed("G03_EMPTY",null);return null;}
                    new Thread(() -> write(input,destination,callback),"nova-gecko-pdf").start();
                    return null;
                }, error -> {
                    if(!closed) callback.failed("G04_RENDER",new Exception(error));
                    return null;
                });
            }
        });
        session.open(runtime);
        view.setSession(session);
        ViewGroup parent=activity.findViewById(android.R.id.content);
        parent.addView(view,0,new ViewGroup.LayoutParams(-1,-1));
    }
    void load(String html) {
        session.load(new GeckoSession.Loader().data(html.getBytes(StandardCharsets.UTF_8),"text/html"));
    }
    private void write(InputStream input, File destination, Callback callback) {
        try (InputStream in=input; FileOutputStream out=new FileOutputStream(destination)) {
            byte[] buffer=new byte[16384];long bytes=0;int count;
            while((count=in.read(buffer))!=-1) {
                if(closed) throw new java.io.IOException("Cancelled");
                bytes+=count;
                if(bytes>128L*1024*1024) throw new java.io.IOException("PDF size limit");
                out.write(buffer,0,count);
            }
        } catch(Exception error) {
            destination.delete();
            activity.runOnUiThread(() -> {if(!closed) callback.failed("G05_WRITE",error);});
            return;
        }
        if(closed) {destination.delete();return;}
        try (ParcelFileDescriptor fd=ParcelFileDescriptor.open(destination,ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer pdf=new PdfRenderer(fd)) {
            int pages=pdf.getPageCount();
            if(pages<1) throw new java.io.IOException("Empty PDF");
            activity.runOnUiThread(() -> {if(!closed) callback.ready(pages);else destination.delete();});
        } catch(Exception error) {
            destination.delete();
            activity.runOnUiThread(() -> {if(!closed) callback.failed("G06_INVALID",error);});
        }
    }
    void close() {
        closed=true;
        if(view.getParent() instanceof ViewGroup) ((ViewGroup)view.getParent()).removeView(view);
        view.releaseSession();
        if(session.isOpen()) session.close();
    }
}
