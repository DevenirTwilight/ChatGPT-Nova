package com.example.chatgptnova;

import android.app.Activity;
import android.net.Uri;
import android.print.PrintDocumentAdapter;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Separate non-executable renderer. Never navigates or accesses a chat bridge. */
final class SnapshotWebView {
    interface Callback {void ready();void failed(String code);default void openLink(String url){}}
    final WebView web;
    final FrozenPageSnapshot snapshot;
    private final String documentTitle;
    private volatile boolean closed;
    private boolean ready;
    private final java.util.concurrent.atomic.AtomicInteger resourceRequests=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicLong resourceBytes=new java.util.concurrent.atomic.AtomicLong();
    private final Runnable timeout;
    SnapshotWebView(Activity activity,WebView live,FrozenPageSnapshot snapshot,Callback callback) {
        this(activity,attachedParent(live),snapshot,snapshot.title,snapshot.baseUrl,snapshot.frozenHtml,false,false,callback);
    }
    /** Local Archive documents: no cookies, script, bridges or remote resource fetches. */
    SnapshotWebView(Activity activity,ViewGroup host,String html,String title,boolean readable,Callback callback) {
        this(activity,host,null,title,"https://nova-archive.invalid/",html,true,readable,callback);
    }
    private static ViewGroup attachedParent(WebView live) {
        if(!(live.getParent() instanceof ViewGroup))throw new IllegalStateException("Renderer requires attached host");
        return (ViewGroup)live.getParent();
    }
    private SnapshotWebView(Activity activity,ViewGroup parent,FrozenPageSnapshot snapshot,String title,
            String base,String html,boolean offline,boolean readable,Callback callback) {
        this.snapshot=snapshot;this.documentTitle=title;
        web=new WebView(activity);
        web.getSettings().setJavaScriptEnabled(false);
        web.getSettings().setDomStorageEnabled(false);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        web.getSettings().setSupportMultipleWindows(false);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setImportantForAccessibility(readable?View.IMPORTANT_FOR_ACCESSIBILITY_YES:View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        web.setFocusable(readable);
        if(offline)web.getSettings().setBlockNetworkLoads(true);
        else android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        timeout=()->{if(!closed&&!ready)callback.failed("S06_STATIC_WEBVIEW_FAILED");};
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if(offline&&readable&&r.isForMainFrame())callback.openLink(r.getUrl().toString());return true;}
            @Override public boolean shouldOverrideUrlLoading(WebView v,String url){if(offline&&readable)callback.openLink(url);return true;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r) {
                // Static resources are fetched without the global WebView cookie jar,
                // auth headers, JS, redirects to non-HTTPS, or backend API requests.
                if(offline||r.isForMainFrame())return denied();
                return resource(r.getUrl().toString());
            }
            @Override public void onPageFinished(WebView v,String url) {
                if(closed||ready)return;
                web.postVisualStateCallback(0,new WebView.VisualStateCallback(){@Override public void onComplete(long id){if(closed||ready)return;ready=true;web.removeCallbacks(timeout);callback.ready();}});
            }
            @Override public void onReceivedError(WebView v,WebResourceRequest r,android.webkit.WebResourceError e) {
                if(r.isForMainFrame()&&!closed)callback.failed("S06_STATIC_WEBVIEW_FAILED");
            }
            @Override public boolean onRenderProcessGone(WebView v,android.webkit.RenderProcessGoneDetail d) {
                if(!closed)callback.failed("S06_STATIC_WEBVIEW_FAILED");return true;
            }
        });
        parent.addView(web,0,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        web.postDelayed(timeout,30000);
        try{web.loadDataWithBaseURL(base,html,"text/html","UTF-8",null);}
        catch(RuntimeException e){close();throw e;}
    }
    static boolean staticUrl(String address) {
        try {Uri u=Uri.parse(address);return "https".equals(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null
            && (u.getPort()==-1||u.getPort()==443)&&!u.getPath().contains("/backend-api")
            &&!u.getPath().contains("/api");}catch(Exception e){return false;}
    }
    private static WebResourceResponse denied(){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
    private WebResourceResponse resource(String address) {
        if(closed||!staticUrl(address)||resourceRequests.incrementAndGet()>64)return denied();
        HttpURLConnection connection=null;
        try {
            for(int redirects=0;redirects<4;redirects++) {
                if(closed||!staticUrl(address))return denied();
                connection=(HttpURLConnection)new URL(address).openConnection();
                connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(5000);connection.setReadTimeout(5000);
                connection.setRequestProperty("Accept-Encoding","identity");
                int status=connection.getResponseCode();
                if(status>=300&&status<400){String next=connection.getHeaderField("Location");if(next==null)return denied();address=new URL(new URL(address),next).toString();connection.disconnect();connection=null;continue;}
                if(status!=200)return denied();
                String mime=connection.getContentType();if(mime==null)return denied();mime=mime.split(";",2)[0].trim().toLowerCase(java.util.Locale.ROOT);
                if(!mime.equals("text/css")&&!mime.startsWith("image/")&&!mime.startsWith("font/")&&!mime.contains("font")&&!mime.equals("application/octet-stream"))return denied();
                // octet-stream is accepted only for font extensions, never arbitrary application content.
                if(mime.equals("application/octet-stream")&&!address.matches("(?i).*\\.(?:woff2?|ttf|otf)(?:\\?.*)?$"))return denied();
                try(java.io.InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                    byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){if(closed||out.size()+n>8*1024*1024||resourceBytes.addAndGet(n)>32L*1024*1024)return denied();out.write(buffer,0,n);}
                    return new WebResourceResponse(mime,"UTF-8",new ByteArrayInputStream(out.toByteArray()));
                }
            }
        }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}
        return denied();
    }
    PrintDocumentAdapter printAdapter(Runnable finished) {
        if(closed||!ready)throw new IllegalStateException("Renderer not ready");
        PrintDocumentAdapter delegate=web.createPrintDocumentAdapter(PageSnapshotExport.documentName(documentTitle));
        return new PrintDocumentAdapter() {
            @Override public void onStart(){if(!closed)delegate.onStart();}
            @Override public void onLayout(android.print.PrintAttributes old,android.print.PrintAttributes next,
                    android.os.CancellationSignal signal,LayoutResultCallback callback,android.os.Bundle extras){if(closed){callback.onLayoutCancelled();return;}android.util.Log.i("NovaSnapshotPrint","layout");delegate.onLayout(old,next,signal,callback,extras);}
            @Override public void onWrite(android.print.PageRange[] pages,android.os.ParcelFileDescriptor fd,
                    android.os.CancellationSignal signal,WriteResultCallback callback){if(closed){callback.onWriteCancelled();return;}android.util.Log.i("NovaSnapshotPrint","write page ranges="+pages.length);delegate.onWrite(pages,fd,signal,callback);}
            @Override public void onFinish(){try{if(!closed)delegate.onFinish();}finally{close();finished.run();}}
        };
    }
    void close() {
        if(closed)return;closed=true;web.removeCallbacks(timeout);web.stopLoading();
        web.setWebViewClient(new WebViewClient());
        if(web.getParent() instanceof ViewGroup)((ViewGroup)web.getParent()).removeView(web);
        web.destroy();
    }
}
