package com.example.chatgptnova;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.WebView;
import android.widget.Toast;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Browser-style save: a single synchronous capture, then no live DOM reads. */
final class PageSnapshotExport {
    static final int SAVE=0x6202;
    private final Activity activity;
    private final WebView live;
    private final BooleanSupplier active;
    private volatile long generation;
    private long started;
    private boolean busy,capturing;
    private volatile boolean destroyed;
    private FrozenPageSnapshot snapshot;
    private SnapshotWebView renderer;
    private File file;
    private String format,mime;
    private Runnable deadline;
    private AlertDialog dialog;
    private JSONObject diagnostic=new JSONObject();
    PageSnapshotExport(Activity a,WebView w,BooleanSupplier active){activity=a;live=w;this.active=active;}
    static boolean trustedUrl(String address) {
        try{Uri u=Uri.parse(address);return "https".equals(u.getScheme())&&"chatgpt.com".equals(u.getHost())
            &&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null;}catch(Exception e){return false;}
    }
    void start() {
        if(busy){toast("保存正在进行，请稍候。");return;}
        if(!active.getAsBoolean()||!trustedUrl(live.getUrl())){fail("S01_INVALID_PAGE");return;}
        busy=true;started=android.os.SystemClock.elapsedRealtime();diagnostic=new JSONObject();
        put("scheme","FROZEN-READING-PAGE-2");put("buildRevision",BuildConfig.EXPORT_REVISION);
        put("app",BuildConfig.VERSION_NAME);put("android",android.os.Build.VERSION.SDK_INT);put("sourceOrigin","https://chatgpt.com");
        android.content.pm.PackageInfo info=WebView.getCurrentWebViewPackage();if(info!=null)put("webView",info.versionName);
        dialog=new AlertDialog.Builder(activity).setTitle("保存当前网页")
            .setMessage("三种格式来自同一次冻结，只包含保存时已加载内容。HTML：普通静态网页，可用常见浏览器阅读；Markdown：适合编辑、迁移和AI工具；PDF：使用Android系统打印。正在生成的回复只保存此刻状态，不等待或滚动加载历史。")
            .setPositiveButton("冻结此刻网页",(d,w)->capture())
            .setNegativeButton("取消",(d,w)->cancel()).setOnCancelListener(d->cancel()).show();
    }
    private String script(String id,String address)throws Exception {
        try(InputStream in=activity.getAssets().open("snapshot/freeze.js")) {
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);
            return new String(bytes.toByteArray(),StandardCharsets.UTF_8)
                .replace("__NOVA_SNAPSHOT_ID__",JSONObject.quote(id)).replace("__NOVA_EXPECTED_URL__",JSONObject.quote(address));
        }
    }
    private void capture() {
        if(!active.getAsBoolean()||!trustedUrl(live.getUrl())){fail("S01_INVALID_PAGE");return;}
        final String address=live.getUrl(),id=UUID.randomUUID().toString();final long ticket=++generation;
        capturing=true;put("snapshotId",id);stage("S00_CAPTURE");
        deadline=()->{if(current(ticket))fail("S02_SNAPSHOT_FAILED");};live.postDelayed(deadline,15000);
        try {
            // Exactly one evaluateJavascript. Navigation guards read URL only,
            // never a second DOM/title/body to fill or verify snapshot content.
            live.evaluateJavascript(script(id,address),result->{
                if(!current(ticket))return;
                stopDeadline();capturing=false;
                if(!active.getAsBoolean()||!address.equals(live.getUrl())){fail("S08_PAGE_CHANGED_BEFORE_SNAPSHOT");return;}
                try {
                    if(result==null||result.length()>24*1024*1024){fail("S03_SNAPSHOT_TOO_LARGE");return;}
                    Object decoded=new JSONTokener(result).nextValue();
                    if(!(decoded instanceof String))throw new IllegalArgumentException();
                    if(((String)decoded).length()>12*1024*1024){fail("S03_SNAPSHOT_TOO_LARGE");return;}
                    JSONObject payload=new JSONObject((String)decoded);
                    if(payload.has("error")){fail(payload.getString("error"));return;}
                    if(!id.equals(payload.getString("snapshotId"))||!address.equals(payload.getString("sourceUrl")))throw new IllegalArgumentException();
                    snapshot=new FrozenPageSnapshot(payload);
                    put("snapshotHtmlChars",snapshot.frozenHtml.length());put("markdownChars",snapshot.markdown.length());
                    put("scope","currently-loaded-page");put("historyCompleteness","not-proven");stage("S00_FROZEN");
                    chooseFormat();
                }catch(Exception e){fail("S02_SNAPSHOT_FAILED");}
            });
        }catch(Exception e){fail("S02_SNAPSHOT_FAILED");}
    }
    private boolean current(long ticket){return !destroyed&&busy&&generation==ticket;}
    private void chooseFormat() {
        dialog=new AlertDialog.Builder(activity).setTitle("保存已冻结网页")
            .setItems(new String[]{"HTML", "Markdown", "打印 / 保存为 PDF"},(d,index)->saveFormat(index))
            .setNegativeButton("取消",(d,w)->cancel()).setOnCancelListener(d->cancel()).show();
    }
    private void saveFormat(int index) {
        if(destroyed||!busy||snapshot==null)return;
        format=index==0?"html":index==1?"markdown":"pdf";put("format",format);
        final long ticket=generation;
        if(index<2){mime=index==0?"text/html":"text/markdown";writeText(ticket,index==0);return;}
        stage("S00_RENDER");
        dialog=new AlertDialog.Builder(activity).setTitle("正在载入冻结网页")
            .setMessage("只载入这份静态 HTML；外部图片可能不可用。")
            .setNegativeButton("取消",(d,w)->cancel()).setOnCancelListener(d->cancel()).show();
        try {
            renderer=new SnapshotWebView(activity,live,snapshot,new SnapshotWebView.Callback(){
                public void ready(){if(!current(ticket))return;if(dialog!=null)dialog.dismiss();print(ticket);}
                public void failed(String code){if(current(ticket))fail(code);}
            });
        }catch(Exception e){fail("S06_STATIC_WEBVIEW_FAILED");}
    }
    private File target(String extension)throws Exception {
        File dir=new File(activity.getCacheDir(),"exports");if(!dir.isDirectory()&&!dir.mkdirs())throw new java.io.IOException();
        return new File(dir,"Nova-snapshot-"+snapshot.snapshotId+"."+extension);
    }
    private void writeText(long ticket,boolean html) {
        stage(html?"S00_HTML":"S00_MARKDOWN");final String content=html?snapshot.frozenHtml:snapshot.markdown;
        final String error=html?"S04_HTML_WRITE_FAILED":"S05_MARKDOWN_WRITE_FAILED";
        try{file=target(html?"html":"md");}catch(Exception e){fail(error);return;}
        final File destination=file;
        new Thread(()->{
            try(OutputStream out=new java.io.FileOutputStream(destination)){
                if(destroyed||ticket!=generation)throw new java.io.IOException();
                out.write(content.getBytes(StandardCharsets.UTF_8));
                if(destroyed||ticket!=generation)throw new java.io.IOException();
            }catch(Exception e){activity.runOnUiThread(()->{destination.delete();if(current(ticket))fail(error);});return;}
            activity.runOnUiThread(()->{if(current(ticket))selectDestination(ticket);else destination.delete();});
        },"snapshot-text").start();
    }
    static String documentName(String title) {
        String name=title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_").trim();
        name=name.replaceAll("[. ]+$", "");
        if(name.isEmpty())name="Nova-snapshot";
        int points=name.codePointCount(0,name.length());if(points>48)name=name.substring(0,name.offsetByCodePoints(0,48));
        name=name.replaceAll("[. ]+$", "");
        return name.isEmpty()?"Nova-snapshot":name;
    }
    private String saveName(){return documentName(snapshot.title)+("html".equals(format)?".html":".md");}
    private void print(long ticket) {
        stage("S00_PRINT_UI");
        try {
            PrintManager manager=activity.getSystemService(PrintManager.class);
            if(manager==null)throw new IllegalStateException();
            android.print.PrintJob job=manager.print("Nova-snapshot-"+snapshot.snapshotId,renderer.printAdapter(()->{
                if(current(ticket)){renderer=null;stage("S00_PRINT_UI_FINISHED");finish();}
            }),new PrintAttributes.Builder().build());
            if(job==null)throw new IllegalStateException("Print service did not start");
            // No success toast: onFinish also occurs when the user cancels.
        }catch(Exception e){fail("S07_PRINT_FAILED");}
    }
    private void selectDestination(long ticket) {
        if(!current(ticket))return;
        if(file==null||!file.isFile()||file.length()==0){fail("html".equals(format)?"S04_HTML_WRITE_FAILED":"S05_MARKDOWN_WRITE_FAILED");return;}
        stage("S00_SAVE_PICKER");
        try{activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType(mime).putExtra(Intent.EXTRA_TITLE,saveName()),SAVE);}
        catch(Exception e){fail("S09_SAVE_PICKER_FAILED");}
    }
    boolean activityResult(int request,int result,Intent data) {
        if(request!=SAVE)return false;
        if(!busy||destroyed)return true;
        if(result!=Activity.RESULT_OK||data==null||data.getData()==null){stage("S00_CANCELLED");finish();return true;}
        Uri uri=data.getData();if(!"content".equals(uri.getScheme())){fail("S10_SAVE_FAILED");return true;}
        final long ticket=generation;final File source=file;stage("S00_COPY");
        new Thread(()->{
            boolean ok=false;
            try(InputStream in=new java.io.FileInputStream(source);OutputStream out=activity.getContentResolver().openOutputStream(uri,"w")) {
                if(out==null)throw new java.io.IOException();byte[] b=new byte[65536];int n;long count=0;
                while((n=in.read(b))!=-1){if(destroyed||ticket!=generation)throw new java.io.IOException();out.write(b,0,n);count+=n;}
                out.flush();ok=count>0&&count==source.length();
            }catch(Exception ignored){ok=false;}
            final boolean saved=ok;activity.runOnUiThread(()->{if(!current(ticket))return;
                if(saved){stage("S00_SAVED");toast("已保存当前网页快照，请打开核对内容。");finish();}else fail("S10_SAVE_FAILED");});
        },"snapshot-save").start();return true;
    }
    void navigationStarted(){if(capturing)fail("S08_PAGE_CHANGED_BEFORE_SNAPSHOT");}
    void showDiagnostic() {
        dialog=new AlertDialog.Builder(activity).setTitle("网页保存诊断").setMessage(diagnostic.toString())
            .setPositiveButton("复制诊断",(d,w)->{android.content.ClipboardManager c=activity.getSystemService(android.content.ClipboardManager.class);
                if(c!=null)c.setPrimaryClip(ClipData.newPlainText("Nova 网页保存诊断",diagnostic.toString()));}).setNegativeButton("关闭",null).show();
    }
    private void fail(String code) {
        stage(code);finish();if(destroyed||activity.isDestroyed())return;
        dialog=new AlertDialog.Builder(activity).setTitle("网页保存未完成").setMessage("错误码："+code+"\n未声明保存成功。可复制诊断排查；不包含正文或私密地址。")
            .setPositiveButton("关闭",null).setNeutralButton("诊断",(d,w)->showDiagnostic()).show();
    }
    private void stage(String code){put("stage",code);put("elapsedMs",android.os.SystemClock.elapsedRealtime()-started);}
    private void put(String key,Object value){try{diagnostic.put(key,value);}catch(Exception ignored){}}
    private void stopDeadline(){if(deadline!=null){live.removeCallbacks(deadline);deadline=null;}}
    private void closeRenderer(){if(renderer!=null){renderer.close();renderer=null;}}
    private void finish(){++generation;capturing=false;busy=false;stopDeadline();closeRenderer();if(dialog!=null){dialog.dismiss();dialog=null;}if(file!=null){file.delete();file=null;}snapshot=null;}
    void cancel(){stage("S00_CANCELLED");finish();}
    void destroy(){destroyed=true;finish();}
    private void toast(String text){if(!activity.isDestroyed())Toast.makeText(activity,text,Toast.LENGTH_LONG).show();}
}
