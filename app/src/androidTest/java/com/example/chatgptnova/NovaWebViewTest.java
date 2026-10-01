package com.example.chatgptnova;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public final class NovaWebViewTest extends FixtureActivity {
    private Instrumentation.ActivityMonitor monitor;
    @Before public void before() { start(); }
    @After public void after() { if (monitor != null) instrument.removeMonitor(monitor); if (scenario != null) scenario.close(); }

    interface Launch { Instrumentation.ActivityResult result(Intent intent); }
    void external(Launch launch) {
        monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) { return launch.result(intent); }
        };
        instrument.addMonitor(monitor);
    }

    @Test public void multipleDocumentsReachTheRealWebInput() {
        AtomicReference<Intent> captured = new AtomicReference<>();
        external(intent -> {
            if (!Intent.ACTION_OPEN_DOCUMENT.equals(intent.getAction())) return null;
            captured.set(intent);
            Intent result = new Intent();
            ClipData clip = ClipData.newRawUri("synthetic fixture",Uri.parse("content://com.example.chatgptnova.test.documents/upload1.txt"));
            clip.addItem(new ClipData.Item(Uri.parse("content://com.example.chatgptnova.test.documents/upload2.txt")));
            result.setClipData(clip);
            return new Instrumentation.ActivityResult(Activity.RESULT_OK,result);
        });
        clickWeb("upload");
        waitFor("chooser dialog", () -> chooser() != null);
        main(() -> chooser().getListView().performItemClick(null,0,0));
        waitFor("web files", () -> "2".equals(js("window.uploadCount")) && js("JSON.stringify(window.uploadData)").contains("upload2.txt"));
        assertNotNull(captured.get());
        assertTrue(captured.get().getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));
        assertArrayEquals(new String[]{"text/plain","image/png","image/jpeg"},captured.get().getStringArrayExtra(Intent.EXTRA_MIME_TYPES));
    }

    AlertDialog chooser() {
        try {
            Field field=MainActivity.class.getDeclaredField("uploads"); field.setAccessible(true);
            Object controller=field.get(activity);
            Field dialog=UploadController.class.getDeclaredField("choice"); dialog.setAccessible(true);
            return (AlertDialog)dialog.get(controller);
        } catch (Exception error) { throw new AssertionError(error); }
    }

    @Test public void openingAnotherChooserKeepsTheUploadedCameraPhoto() throws Exception {
        AtomicReference<Uri> captured = new AtomicReference<>();
        external(intent -> {
            if (MediaStore.ACTION_IMAGE_CAPTURE.equals(intent.getAction())) {
                Uri uri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
                captured.set(uri);
                try (java.io.OutputStream out=activity.getContentResolver().openOutputStream(uri)) {
                    out.write(new byte[]{(byte)255,(byte)216,(byte)255,(byte)217});
                } catch (Exception error) { throw new AssertionError(error); }
                return new Instrumentation.ActivityResult(Activity.RESULT_OK,null);
            }
            if (Intent.ACTION_OPEN_DOCUMENT.equals(intent.getAction())) return new Instrumentation.ActivityResult(Activity.RESULT_OK,
                    new Intent().setData(Uri.parse("content://com.example.chatgptnova.test.documents/upload1.txt")));
            return null;
        });
        clickWeb("camera");
        waitFor("camera callback", () -> "1".equals(js("window.cameraCount")));
        assertNotNull(captured.get()); assertEquals(4,read(captured.get()).length);
        clickWeb("single");
        waitFor("second input", () -> "1".equals(js("window.singleCount")));
        assertEquals("Previous camera URI must remain readable",4,read(captured.get()).length);
    }

    @Test public void blobDownloadWritesAllChunksToTheSelectedDocument() throws Exception {
        AtomicReference<Intent> captured=new AtomicReference<>();
        external(intent -> {
            if (!Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) return null;
            captured.set(intent);
            return new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(OUTPUT));
        });
        clickWeb("download");
        waitFor("blob save", () -> { try { return read(OUTPUT).length==131089; } catch(Exception error) { return false; } });
        byte[] data=read(OUTPUT); for(int i=0;i<data.length;i++) assertEquals((byte)(i%251),data[i]);
        assertNotNull(captured.get()); assertEquals("nova-fixture.bin",captured.get().getStringExtra(Intent.EXTRA_TITLE));
    }

    @Test public void redirectsNeverForwardCookiesAcrossOriginsAndRejectHttpDowngrades() throws Exception {
        main(() -> {
            CookieManager.getInstance().setCookie(PAGE,"nova_download_fixture=first; Path=/; Secure");
            CookieManager.getInstance().setCookie("https://files.nova.invalid/","nova_cdn_fixture=second; Path=/; Secure");
        });
        byte[] payload=new byte[131089]; Arrays.fill(payload,(byte)73);
        FakeConnection first=new FakeConnection(new URL(PAGE),302,"https://files.nova.invalid/document",new byte[0]);
        FakeConnection second=new FakeConnection(new URL("https://files.nova.invalid/document"),200,null,payload);
        AtomicReference<Boolean> done=new AtomicReference<>();
        HttpDownload download=new HttpDownload(activity,PAGE,"Nova fixture",url -> url.getHost().equals("chatgpt.com")?first:second);
        main(() -> download.saveTo(OUTPUT,() -> done.set(true)));
        waitFor("HTTPS download", () -> done.get()!=null);
        assertArrayEquals(payload,read(OUTPUT));
        assertTrue(first.getRequestProperty("Cookie").contains("nova_download_fixture=first"));
        assertNull("Cross-origin redirects must carry no Cookie, even if that host has fixture cookies",second.getRequestProperty("Cookie"));
        assertFalse(first.getInstanceFollowRedirects());
        FakeConnection downgrade=new FakeConnection(new URL(PAGE),302,"http://files.nova.invalid/document",new byte[0]);
        done.set(null);
        HttpDownload refused=new HttpDownload(activity,PAGE,"Nova fixture",url -> { assertEquals("https",url.getProtocol()); return downgrade; });
        main(() -> refused.saveTo(OUTPUT,() -> done.set(true)));
        waitFor("downgrade refused", () -> done.get()!=null);
        assertArrayEquals("Refused redirect must not replace the previous file",payload,read(OUTPUT));
        assertEquals("月亮+文件.txt",DownloadNames.guess(PAGE,"attachment; filename*=UTF-8''%E6%9C%88%E4%BA%AE+%E6%96%87%E4%BB%B6.txt","text/plain"));
    }

    @Test public void onlyTrustedTopLevelPagesReceiveKnownMediaPermissions() {
        Request allowed=new Request(PAGE,PermissionRequest.RESOURCE_AUDIO_CAPTURE,"unknown.fixture.resource");
        main(() -> web.getWebChromeClient().onPermissionRequest(allowed));
        waitFor("permission grant", () -> allowed.granted!=null || allowed.denied);
        assertFalse(allowed.denied); assertArrayEquals(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE},allowed.granted);
        Request port=new Request("https://chatgpt.com:4443/",PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        main(() -> web.getWebChromeClient().onPermissionRequest(port)); assertTrue(port.denied);
        fixture("https://nova.invalid/nova-fixture");
        Request frame=new Request(PAGE,PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        main(() -> web.getWebChromeClient().onPermissionRequest(frame)); assertTrue(frame.denied);
        assertFalse(MainActivity.isTrustedOrigin(Uri.parse("https://chatgpt.com.evil.invalid/")));
        assertFalse(MainActivity.isTrustedOrigin(Uri.parse("http://chatgpt.com/")));
    }

    @Test public void networkErrorsHaveAUsableRetryAndMicrosoftIsNotPreemptivelyBlocked() throws Exception {
        WebResourceRequest request=new WebResourceRequest() {
            public Uri getUrl(){return Uri.parse(PAGE);} public boolean isForMainFrame(){return true;}
            public boolean isRedirect(){return false;} public boolean hasGesture(){return true;}
            public String getMethod(){return "GET";} public Map<String,String> getRequestHeaders(){return Collections.emptyMap();}
        };
        WebResourceError error=new WebResourceError() {
            public int getErrorCode(){return -2;} public CharSequence getDescription(){return "synthetic DNS failure";}
        };
        main(() -> web.getWebViewClient().onReceivedError(web,request,error));
        main(() -> { View retry=text(activity.getWindow().getDecorView(),"重试加载"); assertNotNull(retry); assertTrue(retry.isShown()); retry.performClick(); });
        waitFor("retry loaded", () -> "ready".equals(js("document.getElementById('ready')?.textContent")));
        Method handle=MainActivity.class.getDeclaredMethod("handleUri",Uri.class); handle.setAccessible(true);
        main(() -> { try {
            assertFalse((Boolean)handle.invoke(activity,Uri.parse("https://login.microsoftonline.com/common/oauth2/authorize")));
            assertFalse((Boolean)handle.invoke(activity,Uri.parse("https://appleid.apple.com/auth/authorize")));
        } catch(Exception problem){throw new AssertionError(problem);} });
    }

    @Test public void confirmedClearRemovesSyntheticCookiesAndWebsiteStorage() throws Exception {
        main(() -> { CookieManager.getInstance().setCookie(PAGE,"nova_clear_fixture=present; Path=/; Secure"); CookieManager.getInstance().flush(); });
        js("localStorage.setItem('nova_clear_fixture','present')");
        Method confirm=MainActivity.class.getDeclaredMethod("confirmClear"); confirm.setAccessible(true);
        main(() -> { try { confirm.invoke(activity); } catch(Exception error){throw new AssertionError(error);} });
        waitFor("clear confirmation", () -> {
            android.view.accessibility.AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();
            if(root==null) return false;
            for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText("清除")) {
                if("清除".contentEquals(node.getText()==null?"":node.getText()))
                    return node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
            }
            return false;
        });
        WebView old=web;
        waitFor("cleared WebView", () -> { AtomicReference<WebView> next=new AtomicReference<>(); main(() -> next.set(web(activity))); return next.get()!=null && next.get()!=old; });
        main(() -> web=web(activity)); fixture(PAGE);
        String cookies=CookieManager.getInstance().getCookie(PAGE);
        assertTrue(cookies==null || !cookies.contains("nova_clear_fixture"));
        assertEquals("null",js("localStorage.getItem('nova_clear_fixture')"));
    }

    static final class Request extends PermissionRequest {
        final Uri origin; final String[] resources; volatile String[] granted; volatile boolean denied;
        Request(String origin,String...resources){this.origin=Uri.parse(origin);this.resources=resources;}
        public Uri getOrigin(){return origin;} public String[] getResources(){return resources;}
        public void grant(String[] resources){granted=resources;} public void deny(){denied=true;}
    }
    static final class FakeConnection extends HttpURLConnection {
        final int status; final String location; final byte[] data;
        FakeConnection(URL url,int status,String location,byte[] data){super(url);this.status=status;this.location=location;this.data=data;}
        public void connect(){} public void disconnect(){} public boolean usingProxy(){return false;}
        public int getResponseCode(){return status;}
        public String getHeaderField(String name){return "Location".equals(name)?location:null;}
        public long getContentLengthLong(){return data.length;}
        public java.io.InputStream getInputStream(){return new ByteArrayInputStream(data);}
    }
}
