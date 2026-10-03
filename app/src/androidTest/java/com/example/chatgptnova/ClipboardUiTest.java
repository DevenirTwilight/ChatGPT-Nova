package com.example.chatgptnova;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.SystemClock;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Regression tests for the native clipboard-paste path in the WebView composer. */
public final class ClipboardUiTest extends FixtureActivity {
    @Before public void before() {
        clipboardFixture = true;
        start();
    }

    @After public void after() {
        if (scenario != null) scenario.close();
    }

    @Test public void nativePasteRestoresComposerSelectionForContentEditable() {
        clickWeb("prompt-textarea");
        js("""
                (() => {
                  const e = document.getElementById('prompt-textarea');
                  e.textContent = 'hello';
                  e.focus();
                  const r = document.createRange();
                  r.selectNodeContents(e);
                  r.collapse(false);
                  const s = getSelection();
                  s.removeAllRanges();
                  s.addRange(r);
                  return e.textContent;
                })()
                """);
        putClipboard(" world");

        click("菜单");
        click("从剪贴板粘贴");

        waitFor("contenteditable paste", () -> "hello world".equals(js(
                "document.getElementById('prompt-textarea').textContent")));
        assertEquals("hello world", js("document.getElementById('prompt-textarea').textContent"));
    }

    @Test public void nativePastePrefersLiveSelectionOverDeferredCache() {
        clickWeb("prompt-textarea");
        assertEquals("abXYef", js("""
                (() => {
                  const e = document.getElementById('prompt-textarea');
                  e.textContent = 'abcdef';
                  e.focus();
                  const stale = document.createRange();
                  stale.selectNodeContents(e);
                  stale.collapse(false);
                  window.__novaPasteRange = stale.cloneRange();
                  const current = document.createRange();
                  current.setStart(e.firstChild, 2);
                  current.setEnd(e.firstChild, 4);
                  getSelection().removeAllRanges();
                  getSelection().addRange(current);
                  window.__novaPasteText('XY');
                  return e.textContent;
                })()
                """));
    }

    @Test public void nativePasteSurvivesSpaComposerReplacement() {
        clickWeb("prompt-textarea");
        js("""
                (() => {
                  const old = document.getElementById('prompt-textarea');
                  old.textContent = 'before';
                  old.focus();
                  const s = getSelection();
                  const r = document.createRange();
                  r.selectNodeContents(old);
                  r.collapse(false);
                  s.removeAllRanges();
                  s.addRange(r);
                  history.pushState({}, '', '/c/spa-fixture');
                  old.replaceWith(Object.assign(document.createElement('div'), {
                    id:'prompt-textarea', className:'ProseMirror'
                  }));
                  const current = document.getElementById('prompt-textarea');
                  current.contentEditable = 'true';
                  current.textContent = 'after';
                  current.focus();
                  const range = document.createRange();
                  range.selectNodeContents(current);
                  range.collapse(false);
                  s.removeAllRanges();
                  s.addRange(range);
                  return location.pathname + ':' + current.textContent;
                })()
                """);
        putClipboard(" route");
        click("菜单");
        click("从剪贴板粘贴");
        waitFor("SPA replacement paste", () -> "after route".equals(js(
                "document.getElementById('prompt-textarea').textContent")));
        assertEquals("after route", js("document.getElementById('prompt-textarea').textContent"));
    }

    @Test public void nativePasteReplacesTextareaSelection() {
        clickWeb("mobile-composer-prompt");
        js("""
                (() => {
                  const e = document.getElementById('mobile-composer-prompt');
                  e.value = 'abcdef';
                  e.focus();
                  e.setSelectionRange(2, 4);
                  return e.value;
                })()
                """);
        putClipboard("XY");

        click("菜单");
        click("从剪贴板粘贴");

        waitFor("textarea paste", () -> "abXYef".equals(js(
                "document.getElementById('mobile-composer-prompt').value")));
        assertEquals("abXYef", js("document.getElementById('mobile-composer-prompt').value"));
    }

    @Test public void nativeLongPasteUsesPageTransactionAndUndo() throws Exception {
        pageOwnedLongPaste(true);
    }

    @Test public void systemLongPasteUsesPageTransactionAndUndo() throws Exception {
        pageOwnedLongPaste(false);
    }

    private void pageOwnedLongPaste(boolean nativeMenu) throws Exception {
        // A test-owned editor with its own paste transaction and history. This
        // tests DOM event ownership, not the live ChatGPT editor implementation.
        clickWeb("prompt-textarea");
        String setup = """
                (() => {
                  const e = document.getElementById('prompt-textarea');
                  e.textContent = 'abcdef';
                  window.fixtureModel = 'abcdef';
                  window.fixtureHistory = [];
                  window.fixturePasteCount = 0;
                  window.fixtureNativeEdits = 0;
                  window.fixturePaintMs = null;
                  const original = document.execCommand;
                  document.execCommand = function(...args) {
                    fixtureNativeEdits++;
                    return original.apply(this, args);
                  };
                  window.fixtureApplyPaste = (text, started = performance.now()) => {
                    const range = getSelection().getRangeAt(0);
                    const start = range.startOffset, end = range.endOffset;
                    fixtureHistory.push(fixtureModel);
                    fixtureModel = fixtureModel.slice(0, start) + text + fixtureModel.slice(end);
                    e.textContent = fixtureModel;
                    const caret = document.createRange();
                    caret.setStart(e.firstChild, start + text.length);
                    caret.collapse(true);
                    getSelection().removeAllRanges();
                    getSelection().addRange(caret);
                    e.dispatchEvent(new InputEvent('input', {
                      bubbles:true, inputType:'insertFromPaste', data:text
                    }));
                    window.fixtureTransactionMs = performance.now() - started;
                    requestAnimationFrame(() => requestAnimationFrame(() => {
                      fixturePaintMs = performance.now() - started;
                    }));
                  };
                  // A document handler must also run before Nova's fallback.
                  document.addEventListener('paste', event => {
                    if (event.target !== e) return;
                    const started = performance.now();
                    event.preventDefault();
                    fixturePasteCount++;
                    fixtureApplyPaste(event.clipboardData.getData('text/plain'), started);
                  });
                  window.fixtureUndo = () => {
                    fixtureModel = fixtureHistory.pop();
                    e.textContent = fixtureModel;
                    return fixtureModel;
                  };
                  window.fixtureReset = () => {
                    fixtureModel = 'abcdef';
                    e.textContent = fixtureModel;
                    fixtureHistory = [];
                    fixturePasteCount = fixtureNativeEdits = 0;
                    fixturePaintMs = null;
                    window.fixtureTransactionMs = null;
                    const r = document.createRange();
                    r.setStart(e.firstChild, 2);
                    r.setEnd(e.firstChild, 4);
                    getSelection().removeAllRanges();
                    getSelection().addRange(r);
                    return true;
                  };
                  return fixtureReset();
                })()
                """;
        js(setup);
        String text = "# Heading\n\n中文 English 😀 <script> & literal\n- **bold**\n\n".repeat(4096);
        double[] transactions = new double[3], paints = new double[3];
        double[] baselineTransactions = new double[3], baselinePaints = new double[3];
        JSONArray samples = new JSONArray();
        // Shared emulator scheduling varies between single pastes. Use three
        // paired samples; preserve the existing median regression thresholds.
        for (int sample = 0; sample < transactions.length; sample++) {
            String editorSize = js("(()=>{const e=document.getElementById('prompt-textarea');"
                    + "return e.clientWidth+':'+e.clientHeight})()");
            // The reference must use the same paste route, not a direct DOM update:
            // old WebViews also do selection/IME work around clipboard events.
            // This same-origin test iframe has identical editor CSS but no Nova
            // paste/selection listeners (the production script is top-frame only).
            js("(()=>{const f=document.createElement('iframe');f.id='paste-baseline';"
                    + "f.style='position:fixed;inset:0;width:100%;height:100%;border:0;z-index:99999';"
                    + "f.srcdoc=" + JSONObject.quote(CLIPBOARD_HTML) + ";document.body.append(f);return true})()");
            waitFor("baseline iframe ready", () -> "true".equals(js(
                    "document.getElementById('paste-baseline')?.contentDocument?.getElementById('ready')?.textContent === 'ready'")));
            assertEquals("reference must not run Nova paste listeners", "undefined", js(
                    "typeof document.getElementById('paste-baseline').contentWindow.__novaPasteCompatibilityInstalled"));
            assertEquals("reference editor has the same dimensions", editorSize, js(
                    "(()=>{const e=document.getElementById('paste-baseline').contentDocument.getElementById('prompt-textarea');"
                            + "return e.clientWidth+':'+e.clientHeight})()"));
            js("document.getElementById('paste-baseline').contentWindow.eval(" + JSONObject.quote(setup) + ");true");
            js("(()=>{const f=document.getElementById('paste-baseline');"
                    + "f.contentDocument.getElementById('prompt-textarea').focus();return f.contentWindow.fixtureReset()})()");
            putClipboard(text);
            SystemClock.sleep(400);
            if (nativeMenu) {
                js("(()=>{const f=document.getElementById('paste-baseline'),w=f.contentWindow;"
                        + "const data=new w.DataTransfer();data.setData('text/plain'," + JSONObject.quote(text) + ");"
                        + "f.contentDocument.getElementById('prompt-textarea').dispatchEvent(new w.ClipboardEvent('paste',"
                        + "{bubbles:true,cancelable:true,clipboardData:data}));return true})()");
            } else {
                systemPaste();
            }
            waitFor("editor paste baseline painted", () -> "true".equals(js(
                    "document.getElementById('paste-baseline').contentWindow.fixturePaintMs !== null")));
            assertEquals("baseline uses one paste event", "1", js(
                    "document.getElementById('paste-baseline').contentWindow.fixturePasteCount"));
            assertLongTextEquals("ab" + text + "ef", js(
                    "document.getElementById('paste-baseline').contentWindow.fixtureModel"));
            double baselinePaintMs = Double.parseDouble(js(
                    "document.getElementById('paste-baseline').contentWindow.fixturePaintMs"));
            double baselineTransactionMs = Double.parseDouble(js(
                    "document.getElementById('paste-baseline').contentWindow.fixtureTransactionMs"));
            js("document.getElementById('paste-baseline').remove();true");
            clickWeb("prompt-textarea");
            js("fixtureReset()");
            putClipboard(text);
            SystemClock.sleep(400); // Let Chromium observe the new OS clipboard value.
            if (nativeMenu) {
                click("菜单");
                click("从剪贴板粘贴");
            } else {
                systemPaste();
            }
            waitFor("page editor long paste painted", () -> "true".equals(js("fixturePaintMs !== null")));
            assertEquals("one page transaction", "1", js("fixturePasteCount"));
            assertEquals("handled paste must bypass native HTML editing", "0", js("fixtureNativeEdits"));
            assertLongTextEquals("ab" + text + "ef", js("fixtureModel"));
            assertLongTextEquals("ab" + text + "ef", js("document.getElementById('prompt-textarea').innerText"));
            assertEquals("plain text never creates executable elements", "0", js(
                    "document.getElementById('prompt-textarea').querySelectorAll('script').length"));
            assertEquals(String.valueOf(2 + text.length()), js("getSelection().anchorOffset"));
            double paintMs = Double.parseDouble(js("fixturePaintMs"));
            double transactionMs = Double.parseDouble(js("fixtureTransactionMs"));
            baselineTransactions[sample] = baselineTransactionMs;
            baselinePaints[sample] = baselinePaintMs;
            transactions[sample] = transactionMs;
            paints[sample] = paintMs;
            samples.put(new JSONObject().put("transactionMs", transactionMs).put("paintMs", paintMs)
                    .put("baselineTransactionMs", baselineTransactionMs).put("baselinePaintMs", baselinePaintMs));
            assertEquals("one undo restores the pre-paste draft", "abcdef", js("fixtureUndo()"));
            assertEquals("abcdef", js("document.getElementById('prompt-textarea').innerText"));
        }
        double transactionMs = median(transactions), paintMs = median(paints);
        double baselineTransactionMs = median(baselineTransactions), baselinePaintMs = median(baselinePaints);
        JSONObject result = new JSONObject().put("page", "test-owned paste transaction and history")
                .put("route", nativeMenu ? "native-menu" : "system-paste")
                .put("utf8Bytes", text.getBytes(StandardCharsets.UTF_8).length)
                .put("utf16Units", text.length()).put("transactionMs", transactionMs)
                .put("paintMs", paintMs).put("baselineTransactionMs", baselineTransactionMs)
                .put("baselinePaintMs", baselinePaintMs).put("aggregation", "median of three paired samples")
                .put("samples", samples)
                .put("baseline", "same paste route in an identical iframe without Nova paste listeners");
        File directory = new File(instrument.getTargetContext().getExternalFilesDir(null), "clipboard-probe");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream out = new FileOutputStream(new File(directory,
                nativeMenu ? "page-editor-native.json" : "page-editor-system.json"))) {
            out.write(result.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        assertTrue("Long paste transaction took " + transactionMs + " ms; editor baseline " + baselineTransactionMs,
                transactionMs < Math.max(1000, baselineTransactionMs + 500));
        assertTrue("Long paste paint took " + paintMs + " ms; editor baseline " + baselinePaintMs,
                paintMs < Math.max(3000, baselinePaintMs + 1500));
    }

    private static void assertLongTextEquals(String expected, String actual) {
        int prefix = 0;
        while (prefix < Math.min(expected.length(), actual.length())
                && expected.charAt(prefix) == actual.charAt(prefix)) prefix++;
        assertTrue("Long text mismatch at UTF-16 index " + prefix + "; expected length "
                + expected.length() + ", actual length " + actual.length(), expected.equals(actual));
    }

    private static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private void systemPaste() {
        AtomicBoolean accepted = new AtomicBoolean();
        main(() -> {
            InputConnection connection = web.onCreateInputConnection(new EditorInfo());
            accepted.set(connection != null
                    && connection.performContextMenuAction(android.R.id.paste));
        });
        assertTrue("System paste command accepted", accepted.get());
    }

    private void putClipboard(String text) {
        main(() -> {
            ClipboardManager manager = (ClipboardManager)
                    activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("test", text));
        });
        SystemClock.sleep(100);
    }

    private void click(String label) {
        waitFor("click " + label, () -> {
            android.view.accessibility.AccessibilityNodeInfo node =
                    accessible(label, false);
            while (node != null && !node.isClickable()) node = node.getParent();
            return node != null && node.performAction(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
        });
        instrument.waitForIdleSync();
        SystemClock.sleep(100);
    }

    private boolean shown(String label) {
        return accessible(label, false) != null;
    }

    private android.view.accessibility.AccessibilityNodeInfo accessible(
            String label, boolean partial) {
        android.view.accessibility.AccessibilityNodeInfo found =
                find(instrument.getUiAutomation().getRootInActiveWindow(), label, partial);
        if (found != null) return found;
        for (android.view.accessibility.AccessibilityWindowInfo window :
                instrument.getUiAutomation().getWindows()) {
            found = find(window.getRoot(), label, partial);
            if (found != null) return found;
        }
        return null;
    }

    private static android.view.accessibility.AccessibilityNodeInfo find(
            android.view.accessibility.AccessibilityNodeInfo node,
            String label, boolean partial) {
        if (node == null || !node.isVisibleToUser()) return null;
        String value = node.getText() == null ? "" : node.getText().toString();
        String description = node.getContentDescription() == null
                ? "" : node.getContentDescription().toString();
        if (partial ? value.contains(label) || description.contains(label)
                : value.equals(label) || description.equals(label)) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            android.view.accessibility.AccessibilityNodeInfo result =
                    find(node.getChild(i), label, partial);
            if (result != null) return result;
        }
        return null;
    }
}
