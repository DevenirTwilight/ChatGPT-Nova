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

/** Actual archive/SAF/print-adapter output from immutable public-DOM fixtures. */
public final class FrozenPageSnapshotTest extends FixtureActivity {
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
    private PageSnapshotExport exporter(){return (PageSnapshotExport)field(activity,"pageSnapshotExport");}
    private Object field(Object object,String name){try{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new AssertionError(e);}}
    private void click(String label){waitFor("control "+label,()->{AccessibilityNodeInfo node=find(instrument.getUiAutomation().getRootInActiveWindow(),label);while(node!=null&&!node.isClickable())node=node.getParent();return node!=null&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK);});instrument.waitForIdleSync();}
    private AccessibilityNodeInfo find(AccessibilityNodeInfo n,String label){if(n==null)return null;if(n.getText()!=null&&label.equals(n.getText().toString()))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=find(n.getChild(i),label);if(found!=null)return found;}return null;}
    private FrozenPageSnapshot freeze() {
        main(()->exporter().start());click("冻结此刻网页");
        waitFor("immutable payload",()->field(exporter(),"snapshot")!=null);
        return (FrozenPageSnapshot)field(exporter(),"snapshot");
    }
    private void mutate(){js("document.title='AFTER-TITLE';document.querySelector('h1').textContent='After snapshot marker';document.querySelector('table').innerHTML='<tr><td>AFTER-TABLE</td></tr>';document.getElementById('long').innerHTML='AFTER-BODY';true");}
    private void interceptSave(boolean cancel) {
        monitor=new Instrumentation.ActivityMonitor(){@Override public Instrumentation.ActivityResult onStartActivity(Intent intent){if(!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction()))return null;return new Instrumentation.ActivityResult(cancel?Activity.RESULT_CANCELED:Activity.RESULT_OK,new Intent().setData(OUTPUT));}};
        instrument.addMonitor(monitor);
    }
    private boolean busy(){AtomicBoolean b=new AtomicBoolean();main(()->b.set((Boolean)field(exporter(),"busy")));return b.get();}
    private File evidence(String name,byte[] bytes)throws Exception{File f=new File(activity.getExternalFilesDir(null),name);java.nio.file.Files.write(f.toPath(),bytes);return f;}
    private SnapshotWebView render(FrozenPageSnapshot snapshot) {
        AtomicBoolean ready=new AtomicBoolean();AtomicReference<String> error=new AtomicReference<>();
        main(()->standalone=new SnapshotWebView(activity,web,snapshot,new SnapshotWebView.Callback(){public void ready(){ready.set(true);}public void failed(String code){error.set(code);}}));
        waitFor("static renderer ready",()->ready.get()||error.get()!=null);assertNull(error.get());return standalone;
    }
    @Test public void sameSnapshotMarkdownAndActualMhtmlIgnoreLiveMutation() throws Exception {
        FrozenPageSnapshot snapshot=freeze();mutate();interceptSave(false);click("Markdown");waitFor("Markdown SAF",()->!busy());
        byte[] saved=read(OUTPUT);String md=new String(saved,StandardCharsets.UTF_8);assertEquals(snapshot.markdown,md);
        for(String marker:new String[]{"Before snapshot marker","TAIL-SNAPSHOT-MARKER","中文","café","😀","CODE-LAST","TABLE-LAST","E=mc^2"})assertTrue(marker,md.contains(marker));
        for(String marker:new String[]{"After snapshot marker","AFTER-TABLE","AFTER-BODY","BUTTON-GARBAGE","CODE-CONTROL-GARBAGE","SCRIPT-GARBAGE"})assertFalse(marker,md.contains(marker));
        evidence("frozen-page.md",saved);
        SnapshotWebView v=render(snapshot);AtomicReference<String> result=new AtomicReference<>();File archive=new File(activity.getExternalFilesDir(null),"frozen-page.mhtml");
        main(()->v.web.saveWebArchive(archive.getAbsolutePath(),false,result::set));waitFor("actual same-snapshot archive",()->result.get()!=null);assertTrue(archive.length()>0);
        assertSame(snapshot,v.snapshot);assertFalse(v.web.getSettings().getJavaScriptEnabled());
        assertEquals("After snapshot marker",js("document.querySelector('h1').textContent"));
        // The CI host parses MIME parts and opens THIS actual Android archive in Chromium.
    }
    @Test public void archiveProductionPathUsesSafAndFrozenTitleBodyTable() throws Exception {
        FrozenPageSnapshot snapshot=freeze();mutate();interceptSave(false);click("网页归档（MHTML）");waitFor("MHTML SAF",()->!busy());
        byte[] saved=read(OUTPUT);assertTrue(saved.length>0);evidence("frozen-page-saf.mhtml",saved);
        assertTrue(snapshot.frozenHtml.contains("TABLE-LAST"));assertFalse(snapshot.frozenHtml.contains("AFTER-TABLE"));
        assertNull(field(exporter(),"renderer"));
    }
    @Test public void realSystemPrintUiSavesFrozenMultipagePdf() throws Exception {
        FrozenPageSnapshot snapshot=freeze();mutate();click("打印 / 保存为 PDF");
        waitFor("system print window",()->{AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();return root!=null&&"com.android.printspooler".contentEquals(root.getPackageName());});
        waitFor("destination spinner",()->{AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();if(root==null)return false;for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId("com.android.printspooler:id/destination_spinner"))if(n.isClickable())return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);return false;});
        click("Save as PDF");
        waitFor("PDF preview enabled",()->{AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();if(root==null)return false;for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId("com.android.printspooler:id/print_button"))if(n.isEnabled()&&"Save to PDF".contentEquals(n.getContentDescription()))return true;return false;});
        waitFor("save PDF button",()->{AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();if(root==null)return false;for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId("com.android.printspooler:id/print_button"))if(n.isEnabled())return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);return false;});
        click("Save");waitFor("print job finished",()->!busy());
        String path="/sdcard/Download/Nova-snapshot-"+snapshot.snapshotId+".pdf";
        byte[] bytes;
        try(ParcelFileDescriptor fd=instrument.getUiAutomation().executeShellCommand("cat "+path);java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){bytes=in.readAllBytes();}
        assertTrue(bytes.length>1000);assertEquals("%PDF-",new String(bytes,0,5,StandardCharsets.US_ASCII));
        File pdf=evidence("frozen-page.pdf",bytes);
        try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY);android.graphics.pdf.PdfRenderer r=new android.graphics.pdf.PdfRenderer(fd)){assertTrue(r.getPageCount()>1);}
        assertNull(field(exporter(),"renderer"));assertEquals("After snapshot marker",js("document.querySelector('h1').textContent"));
    }
    @Test public void cancelledSaveCanRepeatWithoutRendererLeak() {
        freeze();interceptSave(true);click("网页归档（MHTML）");waitFor("cancelled picker",()->!busy());assertNull(field(exporter(),"renderer"));
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

}
