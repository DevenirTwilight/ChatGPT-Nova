package com.example.chatgptnova;

import android.webkit.CookieManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Run the seed method against v1, then install -r and run the check against the new APK. */
public final class UpgradeTest extends FixtureActivity {
    @Before public void before() { start(); }
    @After public void after() { if (scenario != null) scenario.close(); }
    @Test public void testSeedUpgradeData() {
        main(() -> { CookieManager.getInstance().setCookie(PAGE,"nova_upgrade_fixture=retained; Max-Age=3600; Path=/; Secure"); CookieManager.getInstance().flush(); });
        assertEquals("seeded",js("localStorage.setItem('nova_upgrade_fixture','retained');'seeded'"));
        assertTrue(CookieManager.getInstance().getCookie(PAGE).contains("nova_upgrade_fixture=retained"));
    }
    @Test public void testUpgradeDataPreserved() {
        String cookies=CookieManager.getInstance().getCookie(PAGE);
        assertNotNull(cookies); assertTrue(cookies.contains("nova_upgrade_fixture=retained"));
        assertEquals("retained",js("localStorage.getItem('nova_upgrade_fixture')"));
        assertEquals("com.example.chatgptnova",activity.getPackageName());
        try { assertEquals(8,activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionCode); }
        catch (Exception error) { throw new AssertionError(error); }
    }
}
