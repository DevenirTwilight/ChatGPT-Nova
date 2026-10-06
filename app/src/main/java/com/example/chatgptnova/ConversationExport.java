package com.example.chatgptnova;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.print.PrintJob;
import android.webkit.WebView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/** DOM trial: no hooks, private readers, network capture or completeness claim. */
final class ConversationExport {
    private static final int SAVE = 0x6201;
    private final Activity activity;
    private final WebView web;
    private final BooleanSupplier active;
    private boolean busy, collecting;
    private volatile boolean destroyed;
    private volatile long generation;
    private long started;
    private File file, pendingSave;
    private String mime, address;
    private WebView printWeb;
    private PrintJob printJob;
    private Runnable deadline;
    private JSONObject diagnostic = new JSONObject();

    ConversationExport(Activity activity, WebView web, BooleanSupplier active) {
        this.activity=activity; this.web=web; this.active=active;
        resetDiagnostic();
    }
    // Existing Activity callbacks retained. Nothing is injected on page load.
    void pageFinished() { }
    void activityPaused() { }
    void activityResumed() {
        if (printJob!=null) {
            if (printJob.isFailed()) fail("P03_JOB_FAILED", "系统打印任务失败，请重新尝试。",null);
            else if (printJob.isCancelled()) stage("P04_CANCELLED", "print-cancelled");
            else if (printJob.isCompleted()) stage("P05_JOB_COMPLETED", "print-job-completed-file-unchecked");
        }
    }
    private String asset() {
        try (java.io.InputStream input=activity.getAssets().open("export/dom-trial.js")) {
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream(); copy(input,out);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) { throw new IllegalStateException("Missing DOM trial asset",e); }
    }
    private boolean trusted() {
        Uri u=Uri.parse(web.getUrl()==null ? "" : web.getUrl());
        return MainActivity.isTrustedOrigin(u) && "chatgpt.com".equals(u.getHost());
    }
    void start() {
        if (busy) { toast("导出正在进行，请稍候。"); return; }
        if (!trusted() || !active.getAsBoolean()) { fail("N01_PAGE", "请打开 ChatGPT 会话后重试。",null); return; }
        busy=true;
        new AlertDialog.Builder(activity).setTitle("导出已加载消息（试用）")
            .setMessage("新方案只读取页面当前已加载的消息，不调用旧内部接口。完整历史尚未确认，长会话可能缺少开头或中间内容。建议先滚到顶部等待加载，再回到底部。图片与附件不会打包。")
            .setPositiveButton("采集并选择格式",(d,w)->capture())
            .setNegativeButton("取消",(d,w)->busy=false).setOnCancelListener(d->busy=false).show();
    }
    private void capture() {
        resetDiagnostic(); started=android.os.SystemClock.elapsedRealtime();
        if (!active.getAsBoolean() || !trusted()) { fail("N01_PAGE","页面已不可用，请重新打开会话。",null); return; }
        collecting=true; address=web.getUrl(); long ticket=++generation;
        put("routeType",Uri.parse(address).getPath()!=null && Uri.parse(address).getPath().startsWith("/g/") ? "project" : "conversation");
        stage("N00_CAPTURE","capture");
        evaluate(false,ticket,result->{
            if (result.has("error")) { fail(result.optString("error"),messageFor(result.optString("error")),null); return; }
            JSONObject stats=result.optJSONObject("diagnostic"); if (stats!=null) put("dom",stats);
            stage("N00_RENDER","render");
            new Thread(()-> {
                try {
                    JSONObject rendered=render(result);
                    activity.runOnUiThread(()-> {
                        if (!current(ticket)) {
                            if(!destroyed && ticket==generation) fail("D10_CHANGED","页面已切换，采集已取消。",null);
                            return;
                        }
                        evaluate(true,ticket,check->{
                            if (check.has("error") || stats==null || check.optJSONObject("diagnostic")==null
                                || !stats.optString("signature").equals(check.optJSONObject("diagnostic").optString("signature"))) {
                                fail("D10_CHANGED","采集期间会话内容发生变化，请等待回复结束后重试。",null); return;
                            }
                            collecting=false; stage("N00_READY","snapshot-ready"); chooseFormat(rendered);
                        });
                    });
                } catch (Exception e) { activity.runOnUiThread(()-> {if(current(ticket)) fail("N04_RENDER","无法转换页面内容。",e);}); }
            },"dom-trial-render").start();
        });
    }
    private boolean current(long ticket) {
        return !destroyed && ticket==generation && active.getAsBoolean()
            && java.util.Objects.equals(address,web.getUrl()) && !activity.isFinishing() && !activity.isDestroyed();
    }
    private void evaluate(boolean verify,long ticket,java.util.function.Consumer<JSONObject> callback) {
        stage(verify ? "N00_VERIFY" : "N00_CAPTURE",verify ? "verify" : "capture");
        stopDeadline(); deadline=()-> {if(!destroyed && ticket==generation) fail("N02_TIMEOUT","页面采集超时，请等待加载完成后重试。",null);};
        web.postDelayed(deadline,15000);
        try {
            web.evaluateJavascript(asset().replace("__NOVA_VERIFY_ONLY__",verify ? "true" : "false"),value->{
                if (!current(ticket)) {
                    if(!destroyed && ticket==generation) fail("D10_CHANGED","页面已切换，采集已取消。",null);
                    return;
                }
                stopDeadline();
                // JSON decoding and conversion stay out of the input/UI transaction queue.
                new Thread(()-> {
                    try {
                        Object decoded=new JSONTokener(value==null ? "null" : value).nextValue();
                        if (!(decoded instanceof String) || ((String)decoded).length()>8*1024*1024) throw new IllegalStateException();
                        JSONObject data=new JSONObject((String)decoded);
                        activity.runOnUiThread(()-> {if(!current(ticket)) {
                            if(!destroyed && ticket==generation) fail("D10_CHANGED","页面已切换，采集已取消。",null);
                        } else {
                            JSONObject stats=data.optJSONObject("diagnostic"); if(stats!=null) put(verify ? "verification" : "dom",stats);
                            callback.accept(data);
                        }});
                    } catch (Exception e) { activity.runOnUiThread(()-> {if(current(ticket)) fail("N03_DECODE","页面未返回有效的导出数据。",e);}); }
                },"dom-trial-decode").start();
            });
        } catch (RuntimeException e) { fail("N03_EVALUATE","无法读取当前页面。",e); }
    }
    static JSONObject render(JSONObject data) throws Exception {
        String title=data.optString("title","未命名会话");
        JSONArray messages=data.getJSONArray("messages"), warnings=data.getJSONArray("warnings");
        StringBuilder body=new StringBuilder(), md=new StringBuilder("# "+title.replaceAll("[\\r\\n]"," ")+"\n\n> 试用版：仅当前已加载消息，完整历史未确认。\n\n");
        for(int i=0;i<messages.length();i++) {
            JSONObject m=messages.getJSONObject(i); String role="user".equals(m.getString("role")) ? "用户" : "助手";
            body.append("<article><h2>").append(role).append("</h2>").append(m.getString("html")).append("</article>");
            md.append("## ").append(role).append("\n\n").append(m.getString("markdown")).append("\n\n---\n\n");
        }
        body.append("<footer><h2>导出说明</h2><ul>"); md.append("## 导出说明\n\n");
        for(int i=0;i<warnings.length();i++) {body.append("<li>").append(escape(warnings.getString(i))).append("</li>");md.append("- ").append(warnings.getString(i)).append('\n');}
        String html="<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
            +"<meta http-equiv='Content-Security-Policy' content=\"default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'\">"
            +"<title>"+escape(title)+"</title><style>body{font:16px/1.7 sans-serif;margin:24px;overflow-wrap:anywhere;color:#20242a;background:white}main{max-width:860px;margin:auto}article{border-bottom:1px solid #ddd;padding:12px 0}pre{background:#f4f5f6;padding:12px;white-space:pre-wrap;overflow-wrap:anywhere}table{border-collapse:collapse;max-width:100%}td,th{border:1px solid #aaa;padding:6px}a{color:#1467a3}@media print{body{margin:0}pre,table{font-size:11px}h2{break-after:avoid}tr{break-inside:avoid}}</style></head><body><main><h1>"
            +escape(title)+"</h1><p>试用版：仅当前已加载消息，完整历史未确认。</p>"+body+"</ul></footer></main></body></html>";
        return new JSONObject().put("title",title).put("html",html).put("markdown",md.toString()).put("warnings",warnings).put("count",messages.length());
    }
    private static String escape(String s) {return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    private void chooseFormat(JSONObject data) {
        if (destroyed) return;
        new AlertDialog.Builder(activity).setTitle("已采集 "+data.optInt("count")+" 条已加载消息")
            .setItems(new String[]{"HTML 阅读版（推荐）","Markdown","PDF"},(dialog,index)-> {
                if (destroyed) return;
                stage("N00_FILE",index==2 ? "pdf-load" : "cache-write");
                try {
                    String extension=index==0 ? "html" : index==1 ? "md" : "pdf";
                    mime=index==0 ? "text/html" : index==1 ? "text/markdown" : "application/pdf";
                    File dir=new File(activity.getCacheDir(),"exports");
                    if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException();
                    File[] old=dir.listFiles();
                    if(old!=null) for(File f:old) if(f.lastModified()<System.currentTimeMillis()-7L*86400000) f.delete();
                    file=new File(dir,filename(data.optString("title"),extension));
                    if(index==2) pdf(data.getString("html"));
                    else {
                        File target=file; String content=data.getString(index==0 ? "html" : "markdown");
                        new Thread(()-> {
                            try (FileOutputStream out=new FileOutputStream(target)) {
                                out.write(content.getBytes(StandardCharsets.UTF_8));
                                activity.runOnUiThread(()-> {if(!destroyed) {put("bytes",target.length());stage("N00_FILE_READY","cache-ready");actions();}});
                            } catch(Exception e) {activity.runOnUiThread(()->fail("S01_CACHE","无法生成导出文件。",e));}
                        },"dom-trial-file").start();
                    }
                } catch(Exception e) {fail("S01_CACHE","无法生成导出文件。",e);}
            }).setNegativeButton("取消",(d,w)->busy=false).setOnCancelListener(d->busy=false).show();
    }
    static String filename(String title,String extension) {
        String cleaned=title.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]","-").trim().replaceAll("[. ]+$","");
        if(cleaned.isEmpty()) cleaned="未命名会话";
        if(cleaned.codePointCount(0,cleaned.length())>48) cleaned=cleaned.substring(0,cleaned.offsetByCodePoints(0,48));
        return "ChatGPT-"+cleaned+"-"+new SimpleDateFormat("yyyyMMdd-HHmmss-SSS",Locale.ROOT).format(new Date())+"-"+UUID.randomUUID().toString().substring(0,6)+"."+extension;
    }
    private void pdf(String html) {
        printWeb=new WebView(activity); printWeb.setVisibility(View.VISIBLE); printWeb.setFocusable(false);
        printWeb.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        ViewGroup parent=activity.findViewById(android.R.id.content);
        parent.addView(printWeb,0,new ViewGroup.LayoutParams(-1,-1));
        printWeb.getSettings().setJavaScriptEnabled(false); printWeb.getSettings().setAllowFileAccess(false);
        printWeb.getSettings().setAllowContentAccess(false); printWeb.getSettings().setBlockNetworkLoads(true);
        printWeb.setWebViewClient(new WebViewClient() {
            private boolean requested;
            @Override public void onPageFinished(WebView view,String url) {
                if(requested || view!=printWeb) return; requested=true;
                view.postVisualStateCallback(1,new WebView.VisualStateCallback() {
                    @Override public void onComplete(long id) {if(!destroyed && view==printWeb) printPdf();}
                });
            }
        });
        deadline=()->fail("P01_LOAD_TIMEOUT","PDF 页面渲染超时。",null); web.postDelayed(deadline,30000);
        printWeb.loadDataWithBaseURL("https://nova-export.invalid/",html,"text/html","UTF-8",null);
    }
    private void printPdf() {
        try {
            PrintManager manager=(PrintManager)activity.getSystemService(Activity.PRINT_SERVICE);
            if(manager==null) throw new IllegalStateException();
            final WebView printing=printWeb;
            PrintDocumentAdapter delegate=printing.createPrintDocumentAdapter(file.getName());
            PrintDocumentAdapter adapter=new PrintDocumentAdapter() {
                @Override public void onStart() {delegate.onStart();}
                @Override public void onLayout(PrintAttributes oldA,PrintAttributes newA,android.os.CancellationSignal signal,LayoutResultCallback callback,android.os.Bundle extras) {
                    stage("N00_PRINT_LAYOUT","print-layout-requested");
                    delegate.onLayout(oldA,newA,signal,callback,extras);
                }
                @Override public void onWrite(android.print.PageRange[] pages,android.os.ParcelFileDescriptor output,android.os.CancellationSignal signal,WriteResultCallback callback) {
                    stage("N00_PRINT_WRITE","print-write-requested");put("requestedPageRanges",pages.length);put("renderAllPages",true);
                    // Chromium subset writes can break the spooler's final PDF transform.
                    // Render the whole document; the system applies the user's page selection.
                    delegate.onWrite(new android.print.PageRange[]{android.print.PageRange.ALL_PAGES},output,signal,callback);
                }
                @Override public void onFinish() {
                    try {delegate.onFinish();} finally {web.post(()-> {if(!destroyed && printing==printWeb) {put("printAdapterFinished",true);if(!diagnostic.optString("code").startsWith("P0")) stage("P06_FINISHED","print-finished-save-unverified");finishPrint(false);busy=false;}});}
                }
            };
            stopDeadline(); printJob=manager.print(file.getName(),adapter,new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
            stage("N00_PRINT_DIALOG","print-dialog"); toast("请选择“保存为 PDF”。保存结果请实际打开文件检查。");
        } catch(Exception e) {fail("P02_START","设备无法启动 PDF 保存界面。",e);}
    }
    private void actions() {
        stopDeadline(); busy=false; if(destroyed) return;
        final File ready=file; final String type=mime;
        new AlertDialog.Builder(activity).setTitle("已生成试用导出文件")
            .setItems(new String[]{"保存到本地…","打开文件","分享文件","复制诊断"},(d,index)-> {
                try {
                    if(index==3) {copyDiagnostic();return;}
                    if(index==0) {
                        pendingSave=ready; busy=true;stage("N00_SAVE_PICKER","save-picker");
                        activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(type).putExtra(Intent.EXTRA_TITLE,ready.getName()),SAVE);
                    } else {
                        Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".fileprovider",ready);
                        Intent intent=index==1 ? new Intent(Intent.ACTION_VIEW).setDataAndType(uri,type) : new Intent(Intent.ACTION_SEND).setType(type).putExtra(Intent.EXTRA_STREAM,uri);
                        intent.setClipData(ClipData.newRawUri("导出会话",uri));intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        activity.startActivity(Intent.createChooser(intent,index==1 ? "打开导出文件" : "分享导出文件"));
                    }
                } catch(RuntimeException e) {pendingSave=null;fail("S02_APP","设备没有支持此操作的应用。",e);}
            }).setNegativeButton("关闭",null).show();
    }
    boolean activityResult(int code,int result,Intent data) {
        if(code!=SAVE) return false;
        final File source=pendingSave;pendingSave=null;
        if(result!=Activity.RESULT_OK || data==null || data.getData()==null) {busy=false;stage("S03_CANCELLED","save-cancelled");toast("已取消保存。");return true;}
        final Uri destination=data.getData();
        if(!"content".equals(destination.getScheme()) || source==null) {fail("S04_URI","无效的保存位置。",null);return true;}
        stage("N00_SAVE_WRITE","save-write");
        new Thread(()-> {
            try (java.io.InputStream in=new java.io.FileInputStream(source);OutputStream out=activity.getContentResolver().openOutputStream(destination,"wt")) {
                if(out==null) throw new java.io.IOException();copy(in,out);
            } catch(Exception e) {activity.runOnUiThread(()->fail("S05_WRITE","保存失败，目标位置可能留有不完整文件，请重新导出。",e));return;}
            activity.runOnUiThread(()-> {busy=false;if(!destroyed) {stage("S00_SAVED","save-stream-closed");toast("已写入保存位置，请打开文件核对内容。");}});
        },"dom-trial-save").start();return true;
    }
    void navigationStarted() {if(collecting) fail("D10_CHANGED","页面已切换，采集已取消。",null);generation++;}
    private void stopDeadline() {if(deadline!=null) web.removeCallbacks(deadline);deadline=null;}
    private void finishPrint(boolean cancel) {
        stopDeadline();if(cancel && printJob!=null) printJob.cancel();printJob=null;
        if(printWeb!=null) {if(printWeb.getParent() instanceof ViewGroup) ((ViewGroup)printWeb.getParent()).removeView(printWeb);printWeb.destroy();printWeb=null;}
    }
    private void resetDiagnostic() {
        diagnostic=new JSONObject();started=android.os.SystemClock.elapsedRealtime();
        put("scheme","DOM-TRIAL-2");put("historyCompleteness","not-proven");put("oldCaptureInstalled",false);
        put("android",android.os.Build.VERSION.SDK_INT);
        android.content.pm.PackageInfo w=WebView.getCurrentWebViewPackage();put("webView",w==null ? "unknown" : w.versionName);
        try {android.content.pm.PackageInfo p=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);put("app",p.versionName);} catch(Exception ignored) {put("app","unknown");}
        stage("N00_IDLE","idle");
    }
    private void put(String key,Object value) {try {diagnostic.put(key,value);} catch(Exception ignored) {}}
    private void stage(String code,String phase) {put("code",code);put("phase",phase);put("elapsedMs",android.os.SystemClock.elapsedRealtime()-started);android.util.Log.i("NovaDomTrial",diagnostic.toString());}
    void showDiagnostic() {
        if(destroyed) return;
        new AlertDialog.Builder(activity).setTitle("新方案导出诊断").setMessage(diagnostic.toString())
            .setPositiveButton("复制诊断",(d,w)->copyDiagnostic()).setNegativeButton("关闭",null).show();
    }
    private void copyDiagnostic() {
        android.content.ClipboardManager clipboard=activity.getSystemService(android.content.ClipboardManager.class);
        if(clipboard!=null) {clipboard.setPrimaryClip(ClipData.newPlainText("Nova DOM 试用诊断",diagnostic.toString()));toast("已复制诊断（不含聊天正文和登录凭据）。");}
    }
    private void fail(String code,String message,Exception error) {
        stopDeadline();generation++;collecting=false;busy=false;finishPrint(true);
        stage(code,"failed");if(error!=null) put("exceptionType",error.getClass().getSimpleName());
        if(!destroyed && !activity.isFinishing() && !activity.isDestroyed())
            new AlertDialog.Builder(activity).setTitle("试用导出失败").setMessage(message+"\n\n错误码："+code+"\n可复制诊断发回排查。")
                .setPositiveButton("知道了",null).setNeutralButton("复制诊断",(d,w)->copyDiagnostic()).show();
    }
    private String messageFor(String code) {
        if ("D09_EMPTY_BODY".equals(code)) {
            JSONObject dom=diagnostic.optJSONObject("dom");
            JSONObject failed=dom==null ? null : dom.optJSONObject("failedMessage");
            if (failed!=null) return "第 "+failed.optInt("index")+" 条"+("user".equals(failed.optString("role")) ? "用户" : "助手")
                +"消息未提取到可读正文，已停止，未跳过该消息。请复制诊断反馈。";
        }
        return message(code);
    }
    private static String message(String code) {
        switch(code) {
            case "D04_STREAMING":return "回复仍在生成，请结束后重试。";
            case "D05_NO_MESSAGES":return "没有找到已加载消息，可能未登录、页面尚未就绪或官网结构改变。";
            case "D06_LIMIT":return "已加载内容超过试用版限制（1000条、200万字符或8MiB结果），已停止，未生成截断文件。";
            case "D02_ROUTE":return "当前不是支持的会话页面。";
            case "D03_LOADING":return "页面尚未加载完成，请稍后重试。";
            case "D07_NESTED":case "D08_DUPLICATE_ID":return "页面消息结构存在歧义，已停止采集。";
            case "D09_EMPTY_BODY":return "有消息未找到可读正文，请展开内容后重试。";
            default:return "无法读取页面消息，请复制诊断排查。";
        }
    }
    private static void copy(java.io.InputStream in,OutputStream out) throws java.io.IOException {byte[] buffer=new byte[16384];int n;while((n=in.read(buffer))!=-1) out.write(buffer,0,n);}
    private void toast(String message) {if(!destroyed) Toast.makeText(activity,message,Toast.LENGTH_LONG).show();}
    void destroy() {destroyed=true;generation++;collecting=false;busy=false;pendingSave=null;finishPrint(true);}
}
