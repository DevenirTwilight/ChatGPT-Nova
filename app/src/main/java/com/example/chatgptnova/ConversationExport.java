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
    private AlertDialog scanDialog;
    private String scrollToken;
    private Runnable scanPoll;
    private String scanAsset;

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
    private String asset() { return asset("dom-trial.js"); }
    private String asset(String name) {
        try (java.io.InputStream input=activity.getAssets().open("export/"+name)) {
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
        new AlertDialog.Builder(activity).setTitle("导出聊天历史（试用）")
            .setMessage("推荐滚动收集：自动上下滚动并缓存消息，再检查顺序与正文一致性。请等待回复结束，采集时不要操作页面。到达顶部仍不证明历史完整。图片与附件原文件不会打包。也可只采集当前页面。")
            .setPositiveButton("滚动收集历史",(d,w)->startScroll())
            .setNeutralButton("采集并选择格式",(d,w)->capture())
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
    private String scrollExpression(String command,String token) {
        if(scanAsset==null) scanAsset=asset("scroll-trial.js").replace("__NOVA_SNAPSHOT__",asset().replace("__NOVA_VERIFY_ONLY__","false"));
        return scanAsset.replace("__NOVA_SCROLL_COMMAND__",JSONObject.quote(command)).replace("__NOVA_SCROLL_TOKEN__",JSONObject.quote(token));
    }
    private void startScroll() {
        resetDiagnostic();file=null;
        if(!active.getAsBoolean() || !trusted()) {fail("N01_PAGE","请重新打开会话。",null);return;}
        collecting=true;address=web.getUrl();long ticket=++generation;
        scrollToken=UUID.randomUUID().toString();put("mode","scroll-cache");
        put("routeType",Uri.parse(address).getPath()!=null && Uri.parse(address).getPath().startsWith("/g/") ? "project" : "conversation");
        scanDialog=new AlertDialog.Builder(activity).setTitle("正在滚动收集历史")
            .setMessage("正在识别聊天滚动区域…\n可随时取消；不会自动保存部分内容。")
            .setNegativeButton("取消采集",(d,w)->cancelScroll()).setOnCancelListener(d->cancelScroll()).create();
        scanDialog.setCanceledOnTouchOutside(false);scanDialog.show();
        pollScroll("start",ticket,scrollToken);
    }
    private void pollScroll(String command,long ticket,String token) {
        if(!current(ticket)) {if(!destroyed && ticket==generation) fail("H04_CHANGED","页面已切换，采集已取消。",null);return;}
        stopDeadline();deadline=()->{if(!destroyed && ticket==generation) fail("N02_TIMEOUT","历史采集回调超时。",null);};web.postDelayed(deadline,15000);
        try {
            web.evaluateJavascript(scrollExpression(command,token),value->{
                if(!current(ticket)) {if(!destroyed && ticket==generation) fail("H04_CHANGED","页面已切换，采集已取消。",null);return;}
                stopDeadline();
                new Thread(()->{
                    try {
                        Object decoded=new JSONTokener(value==null ? "null" : value).nextValue();
                        if(!(decoded instanceof String) || ((String)decoded).length()>8*1024*1024) throw new IllegalStateException();
                        JSONObject result=new JSONObject((String)decoded);
                        JSONObject rendered=result.optBoolean("done") ? render(result) : null;
                        activity.runOnUiThread(()->{
                            if(!current(ticket)) {if(!destroyed && ticket==generation) fail("H04_CHANGED","页面已切换，采集已取消。",null);return;}
                            JSONObject coverage=result.optJSONObject("coverage");if(coverage!=null) put("coverage",coverage);
                            JSONObject dom=result.optJSONObject("diagnostic");if(dom!=null) put("dom",dom);
                            if(result.has("error")) {String code=result.optString("error");fail(code,messageFor(code),null);return;}
                            if(rendered!=null) {
                                stopScan(false);collecting=false;stage("H00_READY","scroll-collected-unproven");
                                showCoverage(result,rendered);return;
                            }
                            if(coverage!=null && scanDialog!=null) {
                                String[] legs={"向上收集","向下收集","第二次向上核对","第二次向下核对"};
                                JSONObject settling=coverage.optJSONObject("settling");
                                String reason=settling==null ? "" : settling.optString("reason");
                                String waiting=("history-loading".equals(reason) || "page-not-ready".equals(reason))
                                    ? "\n网页正在加载历史，正在等待…" : "message-list-changing".equals(reason) ? "\n历史消息正在换入，正在核对…" : "";
                                scanDialog.setMessage(legs[Math.min(3,coverage.optInt("leg"))]+"\n已缓存 "+coverage.optInt("count")+" 条（用户 "+coverage.optInt("users")+" / 助手 "+coverage.optInt("assistants")+"）\n滚动 "+coverage.optInt("steps")+" 次；完整历史未确认。"+waiting+"\n采集时请不要操作页面，可随时取消。");
                            }
                            stage("H00_SCAN","scroll-sampling");scanPoll=()->pollScroll("poll",ticket,token);web.postDelayed(scanPoll,250);
                        });
                    } catch(Exception e) {activity.runOnUiThread(()->{if(current(ticket)) fail("N03_DECODE","无法处理历史采集结果。",e);});}
                },"scroll-trial-decode").start();
            });
        } catch(RuntimeException e) {fail("N03_EVALUATE","无法执行历史采集。",e);}
    }
    private void showCoverage(JSONObject result,JSONObject rendered) {
        JSONArray messages=result.optJSONArray("messages");
        JSONObject coverage=result.optJSONObject("coverage");
        String first=messages==null || messages.length()==0 ? "" : preview(messages.optJSONObject(0));
        String last=messages==null || messages.length()==0 ? "" : preview(messages.optJSONObject(messages.length()-1));
        new AlertDialog.Builder(activity).setTitle("历史覆盖结果（未确认完整）")
            .setMessage(coverageText(coverage)+"\n\n最早："+first+"\n最新："+last)
            .setPositiveButton("选择导出格式",(d,w)->chooseFormat(rendered))
            .setNeutralButton("复制诊断",(d,w)->{copyDiagnostic();busy=false;})
            .setNegativeButton("关闭",(d,w)->busy=false).setOnCancelListener(d->busy=false).show();
    }
    private static String preview(JSONObject message) {
        String text=message==null ? "" : message.optString("markdown").replaceAll("\\s+"," ");
        return text.length()>90 ? text.substring(0,90)+"…" : text;
    }
    private static String coverageText(JSONObject c) {
        if(c==null) return "完整历史未确认。";
        return "已缓存 "+c.optInt("count")+" 条（用户 "+c.optInt("users")+" / 助手 "+c.optInt("assistants")+"）"
            +"\n顶部："+(c.optBoolean("topObserved") ? "已观察到滚动顶部" : "未确认")
            +"\n底部："+(c.optBoolean("bottomObserved") ? "已观察到滚动底部" : "未确认")
            +"\n扫描：两轮上下遍历，顺序与正文核对一致"
            +"\n文本历史：完整性未确认（无独立基准）"
            +"\n附件元数据：未核实覆盖；附件原文件：未包含；图片原文件：未包含。";
    }
    private void stopScan(boolean cancel) {
        if(scanPoll!=null) web.removeCallbacks(scanPoll);scanPoll=null;
        String token=scrollToken;scrollToken=null;
        if(scanDialog!=null) {scanDialog.dismiss();scanDialog=null;}
        if(cancel && token!=null) {
            try {web.evaluateJavascript(scrollExpression("cancel",token),null);}catch(RuntimeException ignored) { }
        }
    }
    private void cancelScroll() {
        stopDeadline();stopScan(true);generation++;collecting=false;busy=false;
        stage("H07_CANCELLED","scroll-cancelled");toast("已取消采集，未生成部分文件。");
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
        JSONObject coverage=data.optJSONObject("coverage");
        String notice=coverage==null ? "试用版：仅当前已加载消息，完整历史未确认。" : "试用版：滚动收集并缓存可见历史，完整历史未确认。";
        String summary=coverage==null ? "" : coverageText(coverage);
        StringBuilder body=new StringBuilder(), md=new StringBuilder("# "+title.replaceAll("[\\r\\n]"," ")+"\n\n> "+notice+"\n\n"+summary+"\n\n");
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
            +escape(title)+"</h1><p>"+escape(notice)+"</p><p>"+escape(summary).replace("\n","<br>")+"</p>"+body+"</ul></footer></main></body></html>";
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
        put("scheme","DOM-SCROLL-TRIAL-2");put("buildRevision",BuildConfig.EXPORT_REVISION);put("historyCompleteness","not-proven");put("oldCaptureInstalled",false);
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
        stopDeadline();stopScan(true);generation++;collecting=false;busy=false;finishPrint(true);
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
            case "H01_SCROLL_CONTAINER":return "未能可靠识别或移动聊天滚动区域，已停止。可尝试当前页面采集，并复制诊断反馈。";
            case "H02_MISSING_ID":return "消息缺少稳定ID，无法安全合并历史，未生成文件。";
            case "H03_ORDER":return "跨窗口消息顺序冲突或无法确定，未生成文件。";
            case "H04_CHANGED":case "H04_SECOND_PASS":case "H04_SESSION":return "采集期间页面、分支或正文发生变化，或第二轮无法核对全部缓存消息。请等待稳定后重试。";
            case "H05_LIMIT":return "历史采集达到时间、滚动次数、消息或大小限制，已停止，未生成截断文件。";
            case "H06_UNSETTLED":return "消息正文、消息列表或边界布局未稳定，采集已停止。请复制诊断（含变化分类）反馈。";
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
    void destroy() {stopScan(true);destroyed=true;generation++;collecting=false;busy=false;pendingSave=null;finishPrint(true);}
}
