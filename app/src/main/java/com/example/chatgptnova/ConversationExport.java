package com.example.chatgptnova;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.webkit.ScriptHandler;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.json.JSONObject;

/** Explicit, current-origin export; no JavaScript interface or credential access. */
final class ConversationExport {
    private static final String CHANNEL = "NovaConversationExport";
    private static final int SAVE = 0x6201;
    private final Activity activity;
    private final WebView web;
    private final BooleanSupplier active;
    private ScriptHandler capture;
    private boolean installed, busy, destroyed;
    private String nonce, address;
    private final StringBuilder incoming = new StringBuilder();
    private File file;
    private String mime;
    private WebView printWeb;
    private ParcelFileDescriptor printFd;
    private CancellationSignal printCancel;
    private PrintDocumentAdapter printAdapter;
    private Runnable deadline;

    ConversationExport(Activity activity, WebView web, BooleanSupplier active) {
        this.activity=activity; this.web=web; this.active=active;
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return;
        WebViewCompat.addWebMessageListener(web, CHANNEL, Collections.singleton("https://chatgpt.com"),
            (view,message,origin,main,reply)-> {
                if (!busy || nonce == null || !main || view != web || !active.getAsBoolean()
                    || !"https://chatgpt.com".equals(origin.toString()) || !address.equals(web.getUrl())) return;
                try {
                    String raw=message.getData();
                    if (raw == null || raw.length()>100000) throw new Exception();
                    JSONObject part=new JSONObject(raw);
                    if (!nonce.equals(part.optString("nonce"))) return;
                    if (part.has("error")) { fail(part.getString("error")); return; }
                    incoming.append(part.getString("chunk"));
                    if (incoming.length()>16*1024*1024) throw new Exception();
                    if (part.getBoolean("done")) {
                        JSONObject result=new JSONObject(incoming.toString());
                        stopDeadline(); nonce=null; incoming.setLength(0);
                        chooseFormat(result);
                    }
                } catch (Exception error) { fail("导出数据无法读取，请重试。"); }
            });
        installed=true;
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            capture=WebViewCompat.addDocumentStartJavaScript(web,asset("capture.js"),Collections.singleton("https://chatgpt.com"));
        }
    }
    private String asset(String name) {
        try (java.io.InputStream input=activity.getAssets().open("export/"+name)) {
            return new String(input.readAllBytes(),StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("Missing export asset",e); }
    }
    void pageFinished() {
        if (installed && active.getAsBoolean() && trusted()) web.evaluateJavascript(asset("capture.js"),null);
    }
    private boolean trusted() {
        Uri u=Uri.parse(web.getUrl()==null ? "" : web.getUrl());
        return MainActivity.isTrustedOrigin(u) && "chatgpt.com".equals(u.getHost());
    }
    void start() {
        if (busy) { toast("导出正在进行，请稍候。"); return; }
        if (!installed || !trusted() || !active.getAsBoolean()) { toast("当前页面无法导出会话。"); return; }
        busy=true; nonce=UUID.randomUUID().toString(); address=web.getUrl(); incoming.setLength(0);
        deadline=()->fail("无法确认完整会话：读取超时，请等待页面加载完成后重试。");
        web.postDelayed(deadline,30000);
        String script=asset("capture.js")+"\n"+asset("marked.js")+"\n"+asset("core.js")+"\n"+
            asset("run.js").replace("__NOVA_NONCE__",JSONObject.quote(nonce));
        web.evaluateJavascript(script,null);
        toast("正在验证完整会话…");
    }
    private void chooseFormat(JSONObject data) {
        String warnings=data.optJSONArray("warnings")==null ? "" : data.optJSONArray("warnings").join("\n").replace("\"","");
        if (!warnings.isEmpty()) {
            new AlertDialog.Builder(activity).setTitle("导出内容说明").setMessage(warnings)
                .setPositiveButton("继续导出",(d,w)->format(data)).setNegativeButton("取消",(d,w)->busy=false)
                .setOnCancelListener(d->busy=false).show();
        } else format(data);
    }
    private void format(JSONObject data) {
        if (destroyed) return;
        new AlertDialog.Builder(activity).setTitle("导出当前会话")
            .setItems(new String[]{"HTML 阅读版（推荐）","Markdown","PDF"},(dialog,index)-> {
                if (destroyed) return;
                try {
                    String extension=index==0 ? "html" : index==1 ? "md" : "pdf";
                    mime=index==0 ? "text/html" : index==1 ? "text/markdown" : "application/pdf";
                    File dir=new File(activity.getCacheDir(),"exports");
                    if (!dir.exists() && !dir.mkdirs()) throw new Exception();
                    File[] old=dir.listFiles();
                    if (old!=null) for (File f:old) if (f.lastModified()<System.currentTimeMillis()-7L*86400000) f.delete();
                    file=new File(dir,filename(data.optString("title"),extension));
                    if (index==2) pdf(data.getString("html"));
                    else {
                        try (FileOutputStream out=new FileOutputStream(file)) {
                            out.write(data.getString(index==0 ? "html" : "markdown").getBytes(StandardCharsets.UTF_8));
                        }
                        actions();
                    }
                } catch (Exception error) { fail("无法生成导出文件。"); }
            }).setNegativeButton("取消",(d,w)->busy=false).setOnCancelListener(d->busy=false).show();
    }
    static String filename(String title,String extension) {
        String cleaned=title.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]","-").trim().replaceAll("[. ]+$","");
        if (cleaned.isEmpty()) cleaned="未命名会话";
        int length=cleaned.codePointCount(0,cleaned.length());
        if (length>48) cleaned=cleaned.substring(0,cleaned.offsetByCodePoints(0,48));
        return "ChatGPT-"+cleaned+"-"+new SimpleDateFormat("yyyyMMdd-HHmmss-SSS",Locale.ROOT).format(new Date())+
            "-"+UUID.randomUUID().toString().substring(0,6)+"."+extension;
    }
    private void pdf(String html) {
        printWeb=new WebView(activity);
        printWeb.getSettings().setJavaScriptEnabled(false);
        printWeb.getSettings().setAllowFileAccess(false);
        printWeb.getSettings().setAllowContentAccess(false);
        printWeb.getSettings().setBlockNetworkLoads(true);
        printWeb.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view,String url) {
                view.postVisualStateCallback(1,new WebView.VisualStateCallback() {
                    @Override public void onComplete(long requestId) { if (view==printWeb && printAdapter==null) writePdf(); }
                });
            }
        });
        deadline=()->fail("PDF 生成超时，请重试。"); web.postDelayed(deadline,60000);
        printWeb.loadDataWithBaseURL("https://nova-export.invalid/",html,"text/html","UTF-8",null);
    }
    private void writePdf() {
        try {
            printAdapter=printWeb.createPrintDocumentAdapter(file.getName()); printAdapter.onStart();
            printCancel=new CancellationSignal();
            PrintAttributes attributes=new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("export","PDF",300,300))
                .setMinMargins(new PrintAttributes.Margins(630,630,630,630)).setColorMode(PrintAttributes.COLOR_MODE_COLOR).build();
            printAdapter.onLayout(null,attributes,printCancel,new PrintDocumentAdapter.LayoutResultCallback() {
                @Override public void onLayoutFinished(PrintDocumentInfo info,boolean changed) {
                    if (destroyed || printAdapter==null) return;
                    try {
                        printFd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_CREATE|ParcelFileDescriptor.MODE_TRUNCATE|ParcelFileDescriptor.MODE_READ_WRITE);
                        printAdapter.onWrite(new PageRange[]{PageRange.ALL_PAGES},printFd,printCancel,new PrintDocumentAdapter.WriteResultCallback() {
                            @Override public void onWriteFinished(PageRange[] pages) {
                                finishPrint(); if (!destroyed && file!=null && file.length()>0) actions();
                            }
                            @Override public void onWriteFailed(CharSequence error) { fail("PDF 生成失败。"); }
                            @Override public void onWriteCancelled() { fail("PDF 已取消。"); }
                        });
                    } catch (Exception error) { fail("PDF 文件无法写入。"); }
                }
                @Override public void onLayoutFailed(CharSequence error) { fail("PDF 排版失败。"); }
                @Override public void onLayoutCancelled() { fail("PDF 已取消。"); }
            },null);
        } catch (Exception error) { fail("设备无法生成 PDF。"); }
    }
    private void actions() {
        stopDeadline(); busy=false;
        if (destroyed) return;
        final File ready=file; final String type=mime;
        new AlertDialog.Builder(activity).setTitle("导出完成").setItems(new String[]{"保存到本地…","打开文件","分享文件"},(d,index)-> {
            file=ready; mime=type;
            try {
                if (index==0) {
                    busy=true;
                    activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(type).putExtra(Intent.EXTRA_TITLE,ready.getName()),SAVE);
                } else {
                    Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".fileprovider",ready);
                    Intent intent=index==1 ? new Intent(Intent.ACTION_VIEW).setDataAndType(uri,type)
                        : new Intent(Intent.ACTION_SEND).setType(type).putExtra(Intent.EXTRA_STREAM,uri).putExtra(Intent.EXTRA_SUBJECT,ready.getName());
                    intent.setClipData(ClipData.newRawUri("导出会话",uri)); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    activity.startActivity(Intent.createChooser(intent,index==1 ? "打开导出文件" : "分享导出文件"));
                }
            } catch (RuntimeException error) { busy=false; toast("设备没有支持此操作的应用。"); }
        }).setNegativeButton("关闭",null).show();
    }
    boolean activityResult(int code,int result,Intent data) {
        if (code!=SAVE) return false;
        if (result!=Activity.RESULT_OK || data==null || data.getData()==null) { busy=false; toast("已取消保存。"); return true; }
        final Uri destination=data.getData(); final File source=file;
        if (!"content".equals(destination.getScheme()) || source==null) { fail("无效的保存位置。"); return true; }
        new Thread(()-> {
            String message="已保存导出文件。";
            try (java.io.InputStream in=new java.io.FileInputStream(source); OutputStream out=activity.getContentResolver().openOutputStream(destination,"wt")) {
                if (out==null) throw new Exception(); in.transferTo(out);
            } catch (Exception error) { message="保存失败，请重新导出并选择位置。"; }
            String outcome=message; activity.runOnUiThread(()-> {busy=false; if (!destroyed) toast(outcome);});
        },"conversation-export-save").start();
        return true;
    }
    void navigationStarted() { if (nonce!=null) fail("页面已切换，导出已取消。"); }
    private void stopDeadline() { if (deadline!=null) web.removeCallbacks(deadline); deadline=null; }
    private void finishPrint() {
        stopDeadline();
        if (printCancel!=null) printCancel.cancel(); printCancel=null;
        if (printFd!=null) try {printFd.close();} catch (Exception ignored) {} printFd=null;
        if (printAdapter!=null) printAdapter.onFinish(); printAdapter=null;
        if (printWeb!=null) printWeb.destroy(); printWeb=null;
    }
    private void fail(String message) { stopDeadline(); nonce=null; incoming.setLength(0); busy=false; finishPrint(); if (!destroyed) toast(message); }
    private void toast(String message) { Toast.makeText(activity,message,Toast.LENGTH_LONG).show(); }
    void destroy() {
        destroyed=true; fail("");
        if (capture!=null) capture.remove(); capture=null;
        if (installed) WebViewCompat.removeWebMessageListener(web,CHANNEL); installed=false;
    }
}
