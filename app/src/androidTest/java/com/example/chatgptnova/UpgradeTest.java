package com.example.chatgptnova;

import android.webkit.CookieManager;
import android.os.SystemClock;
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
        // Instrumentation completion kills the target process. Allow Chromium
        // to commit DOM storage, then verify persistence in a separate process.
        SystemClock.sleep(6000); // Exceed Chromium's five-second default commit timer.
    }
    @Test public void testSeedDataPersistedBeforeUpgrade() {
        String cookies = CookieManager.getInstance().getCookie(PAGE);
        assertNotNull(cookies);
        assertTrue(cookies.contains("nova_upgrade_fixture=retained"));
        assertEquals("retained", js("localStorage.getItem('nova_upgrade_fixture')"));
    }
    @Test public void testUpgradeDataPreserved() {
        String cookies=CookieManager.getInstance().getCookie(PAGE);
        assertNotNull(cookies); assertTrue(cookies.contains("nova_upgrade_fixture=retained"));
        assertEquals("retained",js("localStorage.getItem('nova_upgrade_fixture')"));
        assertEquals("com.example.chatgptnova",activity.getPackageName());
        try {
            android.content.pm.PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);
            assertEquals(BuildConfig.VERSION_CODE,info.versionCode);
            assertEquals(BuildConfig.VERSION_NAME,info.versionName);
        }
        catch (Exception error) { throw new AssertionError(error); }
    }
}
