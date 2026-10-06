package com.example.chatgptnova;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PageRange;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual HTML/SAF/print-adapter output from immutable public-DOM fixtures. */
public class FrozenPageSnapshotTest extends FixtureActivity {
    private Instrumentation.ActivityMonitor monitor;
    private SnapshotWebView standalone;
    @Before public void before() throws Exception {
        start();
        String html;
        try(java.io.InputStream in=instrument.getContext().getAssets().open("frozen-page-fixture.html")){html=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        String script=html.substring(html.indexOf("<script>")+8,html.indexOf("</script>"));
        js("document.open();document.write("+org.json.JSONObject.quote(html.replaceAll("(?s)<script>.*?</script>",""))+ ");document.close();true");
        js(script+";true");
        android.accessibilityservice.AccessibilityServiceInfo info=instrument.getUiAutomation().getServiceInfo();
        info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;instrument.getUiAutomation().setServiceInfo(info);
    }
    @After public void after(){if(monitor!=null)instrument.removeMonitor(monitor);if(standalone!=null)main(()->standalone.close());if(scenario!=null)scenario.close();}
    static void waitFor(String name,Condition condition){long until=android.os.SystemClock.uptimeMillis()+60000;do{if(condition.ready())return;android.os.SystemClock.sleep(100);}while(android.os.SystemClock.uptimeMillis()<until);fail("Timed out: "+name);}
    protected PageSnapshotExport exporter(){return (PageSnapshotExport)field(activity,"pageSnapshotExport");}
    private Object field(Object object,String name){try{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new AssertionError(e);}}
    protected void click(String label){try{waitFor("control "+label,()->{AccessibilityNodeInfo node=find(instrument.getUiAutomation().getRootInActiveWindow(),label);while(node!=null&&!node.isClickable())node=node.getParent();return node!=null&&node.isVisibleToUser()&&node.isEnabled()&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK);});}catch(AssertionError error){
        try{evidence("snapshot-ui-failure.txt",dump(instrument.getUiAutomation().getRootInActiveWindow()).getBytes(StandardCharsets.UTF_8));java.io.ByteArrayOutputStream image=new java.io.ByteArrayOutputStream();android.graphics.Bitmap bitmap=instrument.getUiAutomation().takeScreenshot();if(bitmap!=null){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,image);evidence("snapshot-ui-failure.png",image.toByteArray());}}catch(Exception ignored){}throw error;}
        instrument.waitForIdleSync();}
    protected void retainUi(){retainUi("snapshot");}
    protected void retainUi(String prefix){try{evidence(prefix+"-ui-failure.txt",dump(instrument.getUiAutomation().getRootInActiveWindow()).getBytes(StandardCharsets.UTF_8));android.graphics.Bitmap bitmap=instrument.getUiAutomation().takeScreenshot();if(bitmap!=null){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);evidence(prefix+"-ui-failure.png",out.toByteArray());}}catch(Exception ignored){}}
    private String dump(AccessibilityNodeInfo n){if(n==null)return "no window";StringBuilder s=new StringBuilder(n.toString()).append('\n');for(int i=0;i<n.getChildCount();i++)s.append(dump(n.getChild(i)));return s.toString();}
    protected AccessibilityNodeInfo find(AccessibilityNodeInfo n,String label){if(n==null)return null;if(n.isVisibleToUser()&&((n.getText()!=null&&label.equalsIgnoreCase(n.getText().toString()))||(n.getContentDescription()!=null&&label.equalsIgnoreCase(n.getContentDescription().toString()))))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=find(n.getChild(i),label);if(found!=null)return found;}return null;}
    protected static String shellQuote(String s){return "'"+s.replace("'","'\\''")+"'";}
    protected byte[] fixtureShellBytes(String command){try(ParcelFileDescriptor fd=instrument.getUiAutomation().executeShellCommand(command);java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){return in.readAllBytes();}catch(Exception e){throw new AssertionError(e);}}
    protected String fixtureShell(String command){return new String(fixtureShellBytes(command),StandardCharsets.UTF_8);}
    protected FrozenPageSnapshot freeze() {
        main(()->exporter().start());click("冻结此刻网页");
        waitFor("immutable payload",()->field(exporter(),"snapshot")!=null);
        return (FrozenPageSnapshot)field(exporter(),"snapshot");
    }
    protected void mutate(){js("document.title='AFTER-TITLE';document.querySelector('main').innerHTML='<h1>After snapshot marker</h1><table><tr><td>AFTER-TABLE</td></tr></table><div id=long>AFTER-BODY</div>';true");}
    private void interceptSave(boolean cancel) {
        monitor=new Instrumentation.ActivityMonitor(){@Override public Instrumentation.ActivityResult onStartActivity(Intent intent){if(!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction()))return null;return new Instrumentation.ActivityResult(cancel?Activity.RESULT_CANCELED:Activity.RESULT_OK,new Intent().setData(OUTPUT));}};
        instrument.addMonitor(monitor);
    }
    private boolean busy(){AtomicBoolean b=new AtomicBoolean();main(()->b.set((Boolean)field(exporter(),"busy")));return b.get();}
    protected File evidence(String name,byte[] bytes)throws Exception{File f=new File(activity.getExternalFilesDir(null),name);java.nio.file.Files.write(f.toPath(),bytes);return f;}
    private SnapshotWebView render(FrozenPageSnapshot snapshot) {
        AtomicBoolean ready=new AtomicBoolean();AtomicReference<String> error=new AtomicReference<>();
        main(()->standalone=new SnapshotWebView(activity,web,snapshot,new SnapshotWebView.Callback(){public void ready(){ready.set(true);}public void failed(String code){error.set(code);}}));
        waitFor("static renderer ready",()->ready.get()||error.get()!=null);assertNull(error.get());return standalone;
    }
    @Test public void sameSnapshotMarkdownAndStaticHtmlIgnoreLiveMutation() throws Exception {
        FrozenPageSnapshot snapshot=freeze();mutate();interceptSave(false);click("Markdown");waitFor("Markdown SAF",()->!busy());
        byte[] saved=read(OUTPUT);String md=new String(saved,StandardCharsets.UTF_8);assertEquals(snapshot.markdown,md);
        for(String marker:new String[]{"Before snapshot marker","TAIL-SNAPSHOT-MARKER","中文","café","😀","CODE-LAST","TABLE-LAST","E=mc^2"})assertTrue(marker,md.contains(marker));
        for(String marker:new String[]{"After snapshot marker","AFTER-TABLE","AFTER-BODY","BUTTON-GARBAGE","CODE-CONTROL-GARBAGE","SCRIPT-GARBAGE"})assertFalse(marker,md.contains(marker));
        evidence("frozen-page.md",saved);
        evidence("frozen-page.html",snapshot.frozenHtml.getBytes(StandardCharsets.UTF_8));
        SnapshotWebView v=render(snapshot);assertSame(snapshot,v.snapshot);main(()->assertFalse(v.web.getSettings().getJavaScriptEnabled()));
        assertEquals("After snapshot marker",js("document.querySelector('h1').textContent"));
    }
    @Test public void htmlProductionPathUsesSafAndFrozenTitleBodyTable() throws Exception {
        FrozenPageSnapshot snapshot=freeze();mutate();interceptSave(false);click("HTML");waitFor("HTML SAF",()->!busy());
        byte[] saved=read(OUTPUT);assertEquals(snapshot.frozenHtml,new String(saved,StandardCharsets.UTF_8));evidence("frozen-page-saf.html",saved);
        assertTrue(snapshot.frozenHtml.contains("TABLE-LAST"));assertFalse(snapshot.frozenHtml.contains("AFTER-TABLE"));
        assertNull(field(exporter(),"renderer"));
    }
    @Test public void realSystemPrintUiSavesFrozenMultipagePdf() throws Exception {
        try {
        FrozenPageSnapshot snapshot=freeze();mutate();click("打印 / 保存为 PDF");
        waitFor("system print window",()->{AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();return root!=null&&"com.android.printspooler".contentEquals(root.getPackageName());});
        AccessibilityNodeInfo selected=instrument.getUiAutomation().getRootInActiveWindow();
        if(find(selected,"Save as PDF")==null) {
            waitFor("destination picker",()->{AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();if(root==null)return false;for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId("com.android.printspooler:id/destination_spinner"))if(n.isClickable())return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);return false;});
            waitFor("destination menu",()->find(instrument.getUiAutomation().getRootInActiveWindow(),"All printers…")!=null);click("Save as PDF");
        }
        waitFor("PDF preview enabled",()->{AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();if(root==null||find(root,"Save as PDF")==null)return false;for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId("com.android.printspooler:id/print_button"))if(n.isEnabled())return true;return false;});
        instrument.waitForIdleSync();android.os.SystemClock.sleep(1000);
        waitFor("save PDF button",()->{AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();if(root==null)return false;for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId("com.android.printspooler:id/print_button"))if(n.isEnabled())return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);return false;});
        final String filename="Nova-fixture-"+snapshot.snapshotId+".pdf";
        waitFor("native PDF filename",()->{
            AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();if(root==null)return false;
            for(String id:new String[]{"android:id/title","com.google.android.documentsui:id/filename","com.android.documentsui:id/filename"})
                for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByViewId(id)) {
                    android.os.Bundle args=new android.os.Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,filename);
                    return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);
                }
            return false;
        });
        click("Save");waitFor("print job finished",()->!busy());
        // The adapter document title, not PrintManager's job label, is the default filename.
        // Find the explicitly named file actually saved through DocumentsUI. Production never reads this path.
        final byte[][] actual={null};
        waitFor("actual saved system PDF",()->{
            String paths=fixtureShell("find /sdcard/Download /sdcard/Documents -type f -name "+shellQuote(filename)+" 2>/dev/null");
            for(String path:paths.split("\\r?\\n"))if(path.startsWith("/sdcard/")) {
                byte[] bytes=fixtureShellBytes("cat "+shellQuote(path));
                if(bytes.length>1000&&new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-")){actual[0]=bytes;return true;}
            }return false;
        });
        byte[] bytes=actual[0];
        assertTrue(bytes.length>1000);assertEquals("%PDF-",new String(bytes,0,5,StandardCharsets.US_ASCII));
        File pdf=evidence("frozen-page.pdf",bytes);
        try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY);android.graphics.pdf.PdfRenderer r=new android.graphics.pdf.PdfRenderer(fd)){assertTrue(r.getPageCount()>1);}
        assertNull(field(exporter(),"renderer"));assertEquals("After snapshot marker",js("document.querySelector('h1').textContent"));
        }catch(Exception|AssertionError e){retainUi();throw e;}
    }
    @Test public void cancelledSaveCanRepeatWithoutRendererLeak() {
        freeze();interceptSave(true);click("HTML");waitFor("cancelled picker",()->!busy());assertNull(field(exporter(),"renderer"));
        freeze();click("Markdown");waitFor("repeat cancel",()->!busy());assertNull(field(exporter(),"snapshot"));
    }
    @Test public void captureDoesNotAlterLiveDomOrInstallHooks() {
        String before=js("document.documentElement.outerHTML"),offset=js("scrollY");FrozenPageSnapshot snapshot=freeze();
        assertEquals(before,js("document.documentElement.outerHTML"));assertEquals(offset,js("scrollY"));
        assertFalse(snapshot.frozenHtml.contains("<script"));assertFalse(snapshot.frozenHtml.contains("onclick="));assertFalse(snapshot.frozenHtml.contains("CREDENTIAL-DO-NOT-SAVE"));
        main(()->exporter().cancel());
    }
    @Test public void snapshotRemainsImmutableAndDiagnosticsExcludePrivateValues() throws Exception {
        FrozenPageSnapshot snapshot=freeze();org.json.JSONObject meta=snapshot.diagnosticMetadata();meta.put("privateMutation","SECRET");assertFalse(snapshot.diagnosticMetadata().has("privateMutation"));
        mutate();main(()->exporter().cancel());String diag=field(exporter(),"diagnostic").toString();
        for(String privateValue:new String[]{"Before snapshot marker","nova-fixture","TABLE-LAST","AFTER-TITLE","CREDENTIAL"})assertFalse(diag.contains(privateValue));
        assertTrue(diag.contains(snapshot.snapshotId));assertEquals("Frozen 中文 café 😀",snapshot.title);
    }
    @Test public void staticRendererRejectsUnsafeResourcesAndDoesNotChangeCookies() {
        assertFalse(SnapshotWebView.staticUrl("https://chatgpt.com/backend-api/conversation"));assertFalse(SnapshotWebView.staticUrl("file:///private"));
        assertFalse(SnapshotWebView.staticUrl("https://user:password@example.org/a.css"));assertTrue(SnapshotWebView.staticUrl("https://example.org/static/a.css"));
        android.webkit.CookieManager cm=android.webkit.CookieManager.getInstance();main(()->cm.setCookie(PAGE,"nova_snapshot_cookie=retained; Path=/; Secure"));
        FrozenPageSnapshot snapshot=freeze();main(()->exporter().cancel());render(snapshot);main(()->standalone.close());assertTrue(cm.getCookie(PAGE).contains("nova_snapshot_cookie=retained"));
    }
    @Test public void cancelledPrintAdapterCleansUpAndLivePageStillAcceptsInput() {
        for(int i=0;i<2;i++){FrozenPageSnapshot snapshot=freeze();main(()->exporter().cancel());SnapshotWebView v=render(snapshot);
            main(()->{PrintDocumentAdapter a=v.printAdapter(()->{});a.onStart();a.onFinish();});assertNull(v.web.getParent());}
        assertEquals("true",js("document.querySelector('textarea').value='typing-after-print';document.querySelector('textarea').value==='typing-after-print'"));
    }
    @Test public void activityRecreationAndRendererRecoveryDestroySnapshotViews() {
        FrozenPageSnapshot snapshot=freeze();main(()->exporter().cancel());render(snapshot);
        main(()->{try{Field f=PageSnapshotExport.class.getDeclaredField("renderer");f.setAccessible(true);f.set(exporter(),standalone);}catch(Exception e){throw new AssertionError(e);}});
        main(()->web.getWebViewClient().onRenderProcessGone(web,new android.webkit.RenderProcessGoneDetail(){
            @Override public boolean didCrash(){return true;}
            @Override public int rendererPriorityAtExit(){return android.webkit.WebView.RENDERER_PRIORITY_IMPORTANT;}
        }));
        assertNull(standalone.web.getParent());
        web=FixtureActivity.web(activity);fixture(PAGE);main(()->web.requestFocus());
        assertNotNull(exporter());
        main(()->exporter().start());click("冻结此刻网页");waitFor("post-recovery snapshot",()->field(exporter(),"snapshot")!=null);
        main(()->exporter().cancel());
        scenario.recreate();scenario.onActivity(a->{activity=a;web=FixtureActivity.web(a);});fixture(PAGE);main(()->web.requestFocus());assertNotNull(exporter());
    }

    @Test public void actualSystemPrintCancellationReturnsToLiveChatAndCanRepeat() {
        for(int i=0;i<2;i++) {
            freeze();click("打印 / 保存为 PDF");
            waitFor("print window for cancel",()->{AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();return root!=null&&"com.android.printspooler".contentEquals(root.getPackageName());});
            long[] nextBack={0};waitFor("print cancel releases snapshot",()->{if(!busy())return true;AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();long now=android.os.SystemClock.uptimeMillis();
                if(root!=null&&"com.android.printspooler".contentEquals(root.getPackageName())&&now>=nextBack[0]){instrument.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);nextBack[0]=now+2000;}return false;});
            assertNull(field(exporter(),"renderer"));assertEquals("Before snapshot marker",js("document.querySelector('h1').textContent"));
        }
    }

}
