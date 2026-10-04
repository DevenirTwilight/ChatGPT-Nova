package com.example.chatgptnova;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.CancellationSignal;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.print.PrintJob;
import android.os.Bundle;
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
    private PrintJob printJob;
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
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            copy(input,out);
            return out.toString(StandardCharsets.UTF_8.name());
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
        String script=asset("capture.js")+"\n(() => { const module={exports:{}}; const exports=module.exports;\n"
            +asset("marked.js")+"\nconst marked=module.exports.marked;\n"+asset("core.js")
            +"\nconst NovaExportCore=module.exports;\n"
            +asset("run.js").replace("__NOVA_NONCE__",JSONObject.quote(nonce))+"\n})();";
        web.evaluateJavascript(script,null);
        toast("正在验证完整会话…");
    }
    private void chooseFormat(JSONObject data) {
        StringBuilder details=new StringBuilder();
        org.json.JSONArray list=data.optJSONArray("warnings");
        if (list!=null) for (int i=0;i<list.length();i++) details.append(list.optString(i)).append("\n");
        String warnings=details.toString().trim();
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
                        File target=file;
                        String content=data.getString(index==0 ? "html" : "markdown");
                        new Thread(()-> {
                            try (FileOutputStream out=new FileOutputStream(target)) {
                                out.write(content.getBytes(StandardCharsets.UTF_8));
                                activity.runOnUiThread(()-> { if (!destroyed) actions(); });
                            } catch (Exception error) { activity.runOnUiThread(()->fail("无法生成导出文件。")); }
                        },"conversation-export-render").start();
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
                // Printing an unattached WebView follows the public onPageFinished flow.
                // Visual-state callbacks require an attached/rasterized view on some providers.
                view.post(()-> { if (view==printWeb && printJob==null) printPdf(); });
            }
        });
        deadline=()->fail("PDF 生成超时，请重试。"); web.postDelayed(deadline,60000);
        printWeb.loadDataWithBaseURL("https://nova-export.invalid/",html,"text/html","UTF-8",null);
    }
    private void printPdf() {
        try {
            PrintManager manager=(PrintManager)activity.getSystemService(Activity.PRINT_SERVICE);
            if (manager==null) throw new IllegalStateException("Print service unavailable");
            PrintDocumentAdapter document=printWeb.createPrintDocumentAdapter(file.getName());
            PrintDocumentAdapter lifecycle=new PrintDocumentAdapter() {
                @Override public void onStart() { document.onStart(); }
                @Override public void onLayout(PrintAttributes oldAttributes,PrintAttributes newAttributes,
                    CancellationSignal cancellation,LayoutResultCallback callback,Bundle extras) {
                    document.onLayout(oldAttributes,newAttributes,cancellation,callback,extras);
                }
                @Override public void onWrite(PageRange[] pages,android.os.ParcelFileDescriptor destination,
                    CancellationSignal cancellation,WriteResultCallback callback) {
                    document.onWrite(pages,destination,cancellation,callback);
                }
                @Override public void onFinish() {
                    document.onFinish();
                    activity.runOnUiThread(()-> { finishPrint(); busy=false; });
                }
            };
            PrintAttributes attributes=new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("export","PDF",300,300))
                .setMinMargins(new PrintAttributes.Margins(630,630,630,630)).setColorMode(PrintAttributes.COLOR_MODE_COLOR).build();
            stopDeadline();
            printJob=manager.print(file.getName(),lifecycle,attributes);
            toast("请选择“保存为 PDF”和保存位置。保存后可从文件管理器打开或分享。 ");
        } catch (Exception error) { fail("设备无法启动 PDF 保存界面。"); }
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
                if (out==null) throw new Exception(); copy(in,out);
            } catch (Exception error) { message="保存失败，请重新导出并选择位置。"; }
            String outcome=message; activity.runOnUiThread(()-> {busy=false; if (!destroyed) toast(outcome);});
        },"conversation-export-save").start();
        return true;
    }
    void navigationStarted() { if (nonce!=null) fail("页面已切换，导出已取消。"); }
    private void stopDeadline() { if (deadline!=null) web.removeCallbacks(deadline); deadline=null; }
    private void finishPrint() {
        stopDeadline();
        printJob=null;
        if (printWeb!=null) printWeb.destroy(); printWeb=null;
    }
    private void fail(String message) {
        stopDeadline(); nonce=null; incoming.setLength(0); busy=false; finishPrint();
        if (!destroyed && !activity.isFinishing() && !activity.isDestroyed()) {
            new AlertDialog.Builder(activity).setTitle("未能导出会话").setMessage(message)
                .setPositiveButton("知道了",null).show();
        }
    }
    private static void copy(java.io.InputStream in, OutputStream out) throws java.io.IOException {
        byte[] buffer=new byte[16384]; int count;
        while ((count=in.read(buffer))!=-1) out.write(buffer,0,count);
    }
    private void toast(String message) { Toast.makeText(activity,message,Toast.LENGTH_LONG).show(); }
    void destroy() {
        destroyed=true; fail("");
        if (capture!=null) capture.remove(); capture=null;
        if (installed) WebViewCompat.removeWebMessageListener(web,CHANNEL); installed=false;
    }
}
