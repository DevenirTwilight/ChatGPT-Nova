package com.example.chatgptnova;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentSender;
import android.view.KeyEvent;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic webpage Share calls through the signed Activity, never the Nova menu. */
public final class WebShareTest extends FixtureActivity {
    private static final String PUBLIC_URL = "https://chatgpt.com/share/nova-test-owned-fixture";
    private final AtomicReference<Intent> chooser = new AtomicReference<>();
    private Instrumentation.ActivityMonitor monitor;

    @Before public void before() {
        start();
        ready();
        monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                if (Intent.ACTION_CHOOSER.equals(intent.getAction())) chooser.set(intent);
                return null; // Let Android display the real Sharesheet.
            }
        };
        instrument.addMonitor(monitor);
    }

    @After public void after() {
        if (monitor != null) instrument.removeMonitor(monitor);
        if (scenario != null) scenario.close();
    }

    private void ready() {
        waitFor("Web Share adapter", () -> "function".equals(js("typeof window.NovaWebShare?.share"))
                && "function".equals(js("typeof navigator.share")));
    }

    private void button(String expression) {
        js("(()=>{document.getElementById('web-share')?.remove();let b=document.createElement('button');"
                + "b.id='web-share';b.textContent='Web Share fixture';"
                + "b.style='position:fixed;top:8px;left:8px;width:280px;height:56px;z-index:1000';"
                + "window.shareState='idle';b.onclick=()=>{window.shareState='pending';"
                + expression + ".then(()=>window.shareState='resolved',e=>window.shareState=e.name);};"
                + "document.body.append(b);})()");
    }

    private void shareButton() {
        button("navigator.share({title:'Fixture title',text:'Fixture text',url:" + JSONObject.quote(PUBLIC_URL) + "})");
        clickWeb("web-share");
    }

    private Intent openedSharesheet() {
        waitFor("Android Sharesheet launched by webpage", () -> chooser.get() != null);
        waitFor("Android Sharesheet has focus", () -> {
            AtomicReference<Boolean> unfocused = new AtomicReference<>(false);
            main(() -> unfocused.set(!activity.hasWindowFocus()));
            return unfocused.get();
        });
        return chooser.get();
    }

    @Test public void webpagePayloadReachesSharesheetAndWaitsForTargetSelection() throws Exception {
        shareButton();
        Intent intent = openedSharesheet();
        Intent send = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        assertNotNull(send);
        assertEquals(Intent.ACTION_SEND, send.getAction());
        assertEquals("text/plain", send.getType());
        assertEquals("Fixture title", send.getStringExtra(Intent.EXTRA_TITLE));
        assertEquals("Fixture text\n" + PUBLIC_URL, send.getStringExtra(Intent.EXTRA_TEXT));
        assertFalse(send.getStringExtra(Intent.EXTRA_TEXT).contains("/c/"));
        assertEquals("pending", js("shareState"));
        js("window.parallelShare='pending';navigator.share({text:'second fixture'}).catch(e=>parallelShare=e.name)");
        waitFor("only one share can be pending", () -> "InvalidStateError".equals(js("parallelShare")));
        // Test-owned target-selection signal. This verifies the callback transport,
        // not delivery to a real third-party app or publication of a conversation.
        IntentSender selected = intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT_INTENT_SENDER);
        if (selected == null) selected = intent.getParcelableExtra(Intent.EXTRA_CHOOSER_RESULT_INTENT_SENDER);
        assertNotNull("Chooser must carry its target/result callback", selected);
        selected.sendIntent(activity, 0, null, null, null);
        waitFor("share Promise fulfilled after target selection", () -> "resolved".equals(js("shareState")));
        instrument.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
    }

    @Test public void dismissingSharesheetRejectsAndAllowsAnotherShare() {
        shareButton();
        openedSharesheet();
        assertEquals("pending", js("shareState"));
        instrument.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        waitFor("cancelled share Promise", () -> "AbortError".equals(js("shareState")));
        chooser.set(null);
        shareButton();
        openedSharesheet();
        instrument.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        waitFor("second cancelled share Promise", () -> "AbortError".equals(js("shareState")));
    }

    @Test public void invalidDataAndCallsWithoutAGestureNeverLaunchSharesheet() {
        for (String data : new String[]{"{}", "{url:'javascript:alert(1)'}", "{url:'https://example.com/share/x'}",
                "{url:'https://user@chatgpt.com/share/x'}", "{url:'https://chatgpt.com:444/share/x'}",
                "{files:[new File(['fixture'],'fixture.txt')],text:'fixture'}"}) {
            assertEquals("false", js("navigator.canShare(" + data + ")"));
        }
        assertEquals("true", js("navigator.canShare({url:'/share/nova-test-owned-fixture'})"));
        assertEquals("true", js("navigator.canShare({text:'fixture'})"));
        button("navigator.share({url:'https://example.com/share/x'})");
        clickWeb("web-share");
        waitFor("invalid URL rejected", () -> "TypeError".equals(js("shareState")));
        assertNull(chooser.get());
        fixture(PAGE); // A new document has no previous user activation.
        ready();
        js("window.shareState='pending';navigator.share({text:'fixture'}).catch(e=>window.shareState=e.name)");
        waitFor("share requires a user gesture", () -> "NotAllowedError".equals(js("shareState")));
        assertNull(chooser.get());
    }

    @Test public void shareRemainsInstalledAcrossSpaRoutesAndReloads() {
        js("history.pushState({},'', '/c/nova-private-route-fixture')");
        ready();
        assertTrue(js("location.pathname").startsWith("/c/"));
        shareButton();
        Intent send = openedSharesheet().getParcelableExtra(Intent.EXTRA_INTENT);
        assertEquals("Fixture text\n" + PUBLIC_URL, send.getStringExtra(Intent.EXTRA_TEXT));
        instrument.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        waitFor("SPA share cancelled", () -> "AbortError".equals(js("shareState")));
        fixture(PAGE + "?reload=1");
        ready();
        chooser.set(null);
        shareButton();
        openedSharesheet();
        instrument.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        waitFor("reloaded share cancelled", () -> "AbortError".equals(js("shareState")));
    }

    @Test public void iframeMessagesAreRejectedAndUntrustedPagesHaveNoChannel() {
        js("(()=>{let f=document.createElement('iframe');f.id='share-frame';f.src='/nova-fixture-frame';document.body.append(f);})()");
        waitFor("same-origin fixture iframe loaded", () -> "ready".equals(
                js("document.getElementById('share-frame').contentDocument?.getElementById('ready')?.textContent")));
        js("(()=>{let w=document.getElementById('share-frame').contentWindow;w.shareReply='pending';"
                + "w.NovaWebShare.onmessage=e=>w.shareReply=JSON.parse(e.data).error;"
                + "w.NovaWebShare.postMessage(JSON.stringify({kind:'share',id:'iframe',title:'',text:'fixture',url:''}));})()");
        waitFor("iframe cannot launch native Share", () -> "NotAllowedError".equals(
                js("document.getElementById('share-frame').contentWindow.shareReply")));
        assertNull(chooser.get());
        fixture("https://example.com/nova-fixture");
        assertEquals("undefined", js("typeof window.NovaWebShare"));
        assertNull(chooser.get());
    }
}
