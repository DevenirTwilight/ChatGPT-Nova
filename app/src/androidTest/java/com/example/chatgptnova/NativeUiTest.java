package com.example.chatgptnova;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.webkit.CookieManager;
import android.webkit.WebView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Touch the actual signed Activity using only test-APK-owned pages and data. */
public final class NativeUiTest extends FixtureActivity {
    private static final String LOGIN = "https://chatgpt.com/auth/login";
    @Before public void before() { nativeFixture = true; loginFixture = true; start(); }
    @After public void after() { if (scenario != null) scenario.close(); }

    @Test public void nativeControlsAndLifecycleRemainUsable() throws Exception {
        waitFor("Nova title in accessibility window", () -> shown("ChatGPT Nova"));
        assertTrue(shown("ChatGPT Nova"));
        assertFalse(shown("ChatGPT Nova · 非官方"));
        assertTrue(shown("chatgpt.com"));
        capture("portrait");
        click("菜单");
        for (String item : new String[]{"刷新","ChatGPT 首页","用浏览器打开","设置"}) assertTrue(shown(item));
        for (String old : new String[]{"清除第二账号登录数据","在 Nova 内登录","关于 / 登录帮助"}) assertFalse(shown(old));
        capture("menu");
        click("设置");
        waitFor("settings visible", () -> shown("关于 ChatGPT Nova"));
        capture("settings");
        click("关于 ChatGPT Nova");
        waitFor("unofficial About", () -> contains("这是非官方客户端") && contains("chatgpt.com"));
        capture("about");
        click("知道了");

        settings("登录帮助");
        assertTrue(contains("回到 Nova 不会自动带入浏览器会话"));
        assertTrue(shown("浏览器登录"));
        capture("login-help");
        click("应用内登录");
        waitFor("official login URL in Nova with a fixture page", () -> LOGIN.equals(currentUrl()));
        waitFor("fixture login ready", () -> "ready".equals(js("document.getElementById('ready')?.textContent")));
        assertTrue(shown("菜单"));
        AtomicReference<Boolean> focused = new AtomicReference<>(false);
        main(() -> focused.set(activity.hasWindowFocus()));
        assertTrue("Internal login must keep Nova resumed",focused.get());
        capture("login-entry");
        menu("ChatGPT 首页");
        waitFor("home URL", () -> "https://chatgpt.com/".equals(currentUrl()));
        menu("刷新");
        waitFor("refresh remains usable", () -> shown("菜单"));

        main(() -> { CookieManager.getInstance().setCookie(PAGE,"nova_native_clear=present; Path=/; Secure"); CookieManager.getInstance().flush(); });
        js("localStorage.setItem('nova_native_clear','present')");
        settings("清除登录与网站数据");
        click("取消");
        assertTrue(CookieManager.getInstance().getCookie(PAGE).contains("nova_native_clear=present"));
        assertEquals("present",js("localStorage.getItem('nova_native_clear')"));
        settings("清除登录与网站数据");
        WebView previous = web;
        click("清除");
        waitFor("new WebView after clear", () -> {
            AtomicReference<WebView> next = new AtomicReference<>(); main(() -> next.set(web(activity)));
            return next.get() != null && next.get() != previous;
        });
        main(() -> web = web(activity));
        fixture(PAGE);
        String cookies = CookieManager.getInstance().getCookie(PAGE);
        assertTrue(cookies == null || !cookies.contains("nova_native_clear"));
        assertEquals("null",js("localStorage.getItem('nova_native_clear')"));
        assertTrue(shown("菜单"));

        main(() -> activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        waitFor("landscape", () -> activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        waitFor("landscape controls", () -> shown("菜单"));
        capture("landscape");
        main(() -> activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        waitFor("portrait", () -> activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
        waitFor("portrait controls", () -> shown("菜单"));
        main(() -> web.loadUrl(LOGIN));
        waitFor("back history ready", () -> LOGIN.equals(currentUrl()));
        instrument.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        waitFor("Android Back navigates inside Nova", () -> !LOGIN.equals(currentUrl()) && shown("菜单"));

        // The separate second invocation runs after adb force-stop and must
        // observe these synthetic markers, without substituting a real login.
        main(() -> { CookieManager.getInstance().setCookie(PAGE,"nova_native_restart=retained; Path=/; Secure"); CookieManager.getInstance().flush(); });
        js("localStorage.setItem('nova_native_restart','retained')");
    }

    @Test public void processRestartPreservesSyntheticSessionAndControls() throws Exception {
        String cookie = CookieManager.getInstance().getCookie(PAGE);
        assertNotNull(cookie);
        assertTrue(cookie.contains("nova_native_restart=retained"));
        assertEquals("retained",js("localStorage.getItem('nova_native_restart')"));
        waitFor("Nova title in accessibility window", () -> shown("ChatGPT Nova"));
        assertTrue(shown("ChatGPT Nova"));
        click("菜单");
        for (String item : new String[]{"刷新","ChatGPT 首页","用浏览器打开","设置"}) assertTrue(shown(item));
        capture("restart-menu");
    }

    private String currentUrl() {
        AtomicReference<String> value = new AtomicReference<>(); main(() -> value.set(web.getUrl())); return value.get();
    }
    private void menu(String item) { click("菜单"); click(item); }
    private void settings(String item) { menu("设置"); click(item); }
    private boolean shown(String label) { return find(instrument.getUiAutomation().getRootInActiveWindow(),label,false) != null; }
    private boolean contains(String label) { return find(instrument.getUiAutomation().getRootInActiveWindow(),label,true) != null; }
    private static AccessibilityNodeInfo find(AccessibilityNodeInfo node,String label,boolean partial) {
        if (node == null || !node.isVisibleToUser()) return null;
        String value = node.getText() == null ? "" : node.getText().toString();
        String description = node.getContentDescription() == null ? "" : node.getContentDescription().toString();
        if (partial ? value.contains(label) || description.contains(label) : value.equals(label) || description.equals(label)) return node;
        for (int i=0;i<node.getChildCount();i++) { AccessibilityNodeInfo result=find(node.getChild(i),label,partial); if(result!=null)return result; }
        return null;
    }
    private void click(String label) {
        waitFor("click "+label, () -> {
            AccessibilityNodeInfo node=find(instrument.getUiAutomation().getRootInActiveWindow(),label,false);
            while (node != null && !node.isClickable()) node=node.getParent();
            return node != null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        });
        instrument.waitForIdleSync();
        SystemClock.sleep(100);
    }
    private void capture(String name) throws Exception {
        Bitmap screenshot=instrument.getUiAutomation().takeScreenshot(); assertNotNull(screenshot);
        File folder=new File(instrument.getTargetContext().getExternalFilesDir(null),"native-smoke");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))) { assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,out)); }
        screenshot.recycle();
    }
}
