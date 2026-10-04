package com.example.chatgptnova;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.SystemClock;
import android.os.Build;
import android.os.Handler;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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

    @Test public void nativePastePrefersLiveSelectionOverSavedCache() {
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
        pageOwnedLongPaste("native-menu");
    }

    @Test public void systemLongPasteUsesPageTransactionAndUndo() throws Exception {
        pageOwnedLongPaste("system-paste");
    }

    @Test public void keyboardLongCommitUsesPageTransactionAndUndo() throws Exception {
        pageOwnedLongPaste("keyboard-commit-text");
    }

    @Test public void keyboardBulkCommitKeepsFollowingEditsInOrder() throws Exception {
        clickWeb("mobile-composer-prompt");
        String text = "中文 English 😀\n\n".repeat(128);
        keyboardInput(connection -> {
            // Prepare the draft through the same native connection. Setting
            // textarea.value in JS immediately before creating an IME connection
            // can leave Chromium's native editing state on the previous draft.
            assertTrue(connection.finishComposingText());
            assertTrue(connection.commitText("abcdef", 1));
            assertTrue(connection.setSelection(2, 4));
            assertTrue(connection.beginBatchEdit());
            assertTrue(connection.commitText(text, 1));
            assertTrue(connection.commitText("!", 1));
            assertTrue(connection.deleteSurroundingText(1, 0));
            assertTrue(connection.commitText("?", 1));
            connection.endBatchEdit();
        });
        String expected = "ab" + text + "?ef";
        AtomicReference<String> actual = new AtomicReference<>("");
        try {
            waitFor("keyboard bulk commit and following edits", () -> {
                actual.set(js("document.getElementById('mobile-composer-prompt').value"));
                return expected.equals(actual.get());
            });
        } catch (AssertionError failure) {
            String value = actual.get();
            throw new AssertionError("Keyboard edit order: expected length " + expected.length()
                    + ", actual length " + value.length() + ", fixture tail "
                    + value.substring(Math.max(0, value.length() - 24)), failure);
        }
        assertLongTextEquals(expected, js("document.getElementById('mobile-composer-prompt').value"));
        assertEquals("one offered clipboard transaction", "1", js("pasteEvents.length"));
    }

    @Test public void keyboardNativeSemanticsRemainOutsideBulkComposerPath() throws Exception {
        clickWeb("mobile-composer-prompt");
        String text = "x".repeat(2048);
        keyboardInput(connection -> {
            assertTrue(connection.setComposingText("draft", 1));
            assertTrue(connection.commitText(text, 1));
        });
        waitFor("native composing replacement", () -> text.equals(js(
                "document.getElementById('mobile-composer-prompt').value")));
        assertEquals("active composition stays native", "0", js("pasteEvents.length"));
        js("(()=>{const e=document.getElementById('mobile-composer-prompt');e.value='';return true})()");
        keyboardInput(connection -> {
            assertTrue(connection.setSelection(0, 0));
            assertTrue(connection.commitText(text, 0));
        });
        waitFor("non-default cursor commit", () -> text.equals(js(
                "document.getElementById('mobile-composer-prompt').value")));
        assertEquals("native cursor position is preserved", "0", js(
                "document.getElementById('mobile-composer-prompt').selectionStart"));
        js("(()=>{const e=document.createElement('textarea');e.id='other-input';document.body.append(e);e.scrollIntoView({block:'center'});return true})()");
        clickWeb("other-input");
        keyboardInput(connection -> assertTrue(connection.commitText(text, 1)));
        waitFor("non-composer input uses native connection", () -> text.equals(js(
                "document.getElementById('other-input').value")));
        assertEquals("IME text never targets a remembered composer", text, js(
                "document.getElementById('mobile-composer-prompt').value"));
        assertEquals("non-composer commit does not synthesize paste", "0", js("pasteEvents.length"));
        js("(()=>{const e=document.createElement('input');e.type='password';e.id='fixture-password';e.style.cssText='display:block;width:95%;height:56px';document.body.append(e);e.focus();e.scrollIntoView({block:'center'});return true})()");
        clickWeb("fixture-password");
        waitFor("password fixture has DOM focus", () -> "fixture-password".equals(js("document.activeElement.id")));
        // Renderer focus and Android's EditorInfo update asynchronously. Do not
        // assert against the previous textarea's native input context.
        waitFor("Android password input context", () -> {
            AtomicBoolean ready = new AtomicBoolean();
            main(() -> {
                EditorInfo info = new EditorInfo();
                web.onCreateInputConnection(info);
                int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
                ready.set(variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                        || variation == InputType.TYPE_TEXT_VARIATION_PASSWORD);
            });
            return ready.get();
        });
        keyboardInput(connection -> {
            assertTrue("password input never receives Nova's wrapper",
                    !connection.getClass().getName().contains("ComposerWebView"));
            assertTrue(connection.commitText("synthetic-secret", 1));
        });
        waitFor("test-owned password retains native input", () -> "synthetic-secret".equals(js(
                "document.getElementById('fixture-password').value")));
        assertEquals("password input never emits a Nova paste", "0", js("pasteEvents.length"));
    }

    private void pageOwnedLongPaste(String route) throws Exception {
        boolean nativeMenu = "native-menu".equals(route);
        boolean keyboard = "keyboard-commit-text".equals(route);
        // A test-owned editor with its own paste transaction and history. This
        // tests DOM event ownership, not the live ChatGPT editor implementation.
        String setup = """
                (() => {
                  const e = document.getElementById('prompt-textarea');
                  e.textContent = 'abcdef';
                  window.fixtureModel = 'abcdef';
                  window.fixtureHistory = [];
                  window.fixturePasteCount = 0;
                  window.fixtureNativeEdits = 0;
                  window.fixturePaintMs = null;
                  window.fixtureApplying = false;
                  window.fixtureRangeReads = 0;
                  const originalGetRangeAt = Selection.prototype.getRangeAt;
                  Selection.prototype.getRangeAt = function(...args) {
                    if (fixtureApplying) fixtureRangeReads++;
                    return originalGetRangeAt.apply(this, args);
                  };
                  const original = document.execCommand;
                  document.execCommand = function(...args) {
                    fixtureNativeEdits++;
                    return original.apply(this, args);
                  };
                  window.fixtureApplyPaste = (text, started = performance.now()) => {
                    fixtureApplying = true;
                    const phases = {};
                    let phaseStarted = started;
                    const mark = name => {
                      const now = performance.now();
                      phases[name] = now - phaseStarted;
                      phaseStarted = now;
                    };
                    const range = getSelection().getRangeAt(0);
                    const start = range.startOffset, end = range.endOffset;
                    mark('selection');
                    fixtureHistory.push(fixtureModel);
                    fixtureModel = fixtureModel.slice(0, start) + text + fixtureModel.slice(end);
                    mark('model');
                    e.textContent = fixtureModel;
                    mark('dom');
                    const caret = document.createRange();
                    caret.setStart(e.firstChild, start + text.length);
                    caret.collapse(true);
                    getSelection().removeAllRanges();
                    getSelection().addRange(caret);
                    mark('caret');
                    e.dispatchEvent(new InputEvent('input', {
                      bubbles:true, inputType:'insertFromPaste', data:text
                    }));
                    mark('input');
                    fixtureApplying = false;
                    window.fixturePhaseMs = phases;
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
                    window.fixturePhaseMs = null;
                    fixtureRangeReads = 0;
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
        String text = "# Heading\n\n中文 English 😀 <script> & literal\n- **bold**\n\n".repeat(4096);
        double[] transactions = new double[3], paints = new double[3];
        double[] baselineTransactions = new double[3], baselinePaints = new double[3];
        JSONArray samples = new JSONArray();
        // Shared emulator scheduling varies between single pastes. Use three
        // paired samples; preserve the existing median regression thresholds.
        for (int sample = 0; sample < transactions.length; sample++) {
            // Use the same top-level WebView for both samples. An iframe has
            // different selection/layout and IME behavior, even with identical CSS.
            // The test-only reference origin receives no Nova paste scripts.
            fixture(CLIPBOARD_REFERENCE);
            assertEquals("reference must not run Nova paste listeners", "undefined", js(
                    "typeof window.__novaPasteCompatibilityInstalled"));
            clickWeb("prompt-textarea");
            js(setup);
            String editorSize = js("(()=>{const e=document.getElementById('prompt-textarea');"
                    + "return e.clientWidth+':'+e.clientHeight})()");
            putClipboard(text);
            SystemClock.sleep(400);
            selectRangeForPaste();
            if (nativeMenu) {
                js("(()=>{fixtureReset();const data=new DataTransfer();data.setData('text/plain'," + JSONObject.quote(text) + ");"
                        + "document.getElementById('prompt-textarea').dispatchEvent(new ClipboardEvent('paste',"
                        + "{bubbles:true,cancelable:true,clipboardData:data}));return true})()");
            } else {
                // The reference is the editor's normal clipboard transaction.
                // Raw multiline IME commit is the failing path being replaced;
                // do not claim this reference measures unadapted IME performance.
                systemPaste();
            }
            waitFor("editor paste baseline painted", () -> "true".equals(js("fixturePaintMs !== null")));
            assertEquals("baseline uses one paste event", "1", js("fixturePasteCount"));
            assertLongTextEquals("ab" + text + "ef", js("fixtureModel"));
            double baselinePaintMs = Double.parseDouble(js(
                    "fixturePaintMs"));
            double baselineTransactionMs = Double.parseDouble(js(
                    "fixtureTransactionMs"));
            JSONObject baselinePhases = new JSONObject(js(
                    "fixturePhaseMs"));
            fixture(PAGE);
            assertEquals("candidate runs the production paste adapter", "function", js("typeof window.__novaPasteText"));
            clickWeb("prompt-textarea");
            js(setup);
            assertEquals("reference editor has the same dimensions", editorSize, js(
                    "(()=>{const e=document.getElementById('prompt-textarea');"
                            + "return e.clientWidth+':'+e.clientHeight})()"));
            putClipboard(text);
            SystemClock.sleep(400); // Let Chromium observe the new OS clipboard value.
            selectRangeForPaste();
            long requestStarted = SystemClock.uptimeMillis();
            if (nativeMenu) {
                click("菜单");
                click("从剪贴板粘贴");
            } else if (keyboard) {
                int index = sample;
                keyboardInput(connection -> {
                    assertTrue(connection.setSelection(2, 4));
                    // Exercise standard entry points, without keyboard names.
                    boolean accepted;
                    if (Build.VERSION.SDK_INT >= 34 && index == 2) {
                        accepted = connection.replaceText(2, 4, text, 1, null);
                    } else if (Build.VERSION.SDK_INT >= 33 && index == 1) {
                        accepted = connection.commitText(text, 1, null);
                    } else {
                        accepted = connection.commitText(text, 1);
                    }
                    assertTrue("Keyboard clipboard commit accepted", accepted);
                });
            } else {
                systemPaste();
            }
            waitFor("page editor long paste painted", () -> "true".equals(js("fixturePaintMs !== null")));
            assertEquals("one page transaction", "1", js("fixturePasteCount"));
            assertEquals("handled paste must bypass native HTML editing", "0", js("fixtureNativeEdits"));
            assertEquals("handled paste must not trigger adapter Range reads", "1", js("fixtureRangeReads"));
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
                    .put("inputApi", keyboard ? Build.VERSION.SDK_INT >= 34 && sample == 2
                            ? "replaceText" : sample == 1 ? "commitText-with-attributes" : "commitText" : route)
                    .put("requestToVerifiedMs", SystemClock.uptimeMillis() - requestStarted)
                    .put("baselineTransactionMs", baselineTransactionMs).put("baselinePaintMs", baselinePaintMs)
                    .put("phasesMs", new JSONObject(js("fixturePhaseMs"))).put("baselinePhasesMs", baselinePhases));
            assertEquals("one undo restores the pre-paste draft", "abcdef", js("fixtureUndo()"));
            assertEquals("abcdef", js("document.getElementById('prompt-textarea').innerText"));
        }
        double transactionMs = median(transactions), paintMs = median(paints);
        double baselineTransactionMs = median(baselineTransactions), baselinePaintMs = median(baselinePaints);
        JSONObject result = new JSONObject().put("page", "test-owned paste transaction and history")
                .put("route", route)
                .put("utf8Bytes", text.getBytes(StandardCharsets.UTF_8).length)
                .put("utf16Units", text.length()).put("transactionMs", transactionMs)
                .put("paintMs", paintMs).put("baselineTransactionMs", baselineTransactionMs)
                .put("baselinePaintMs", baselinePaintMs).put("aggregation", "median of three paired samples")
                .put("samples", samples)
                .put("baseline", keyboard
                        ? "normal OS paste transaction in the same top-level WebView; identical test editor at an untrusted test-only origin without Nova adapters; not raw IME commit performance"
                        : "same paste route and top-level WebView; identical test editor at an untrusted test-only origin without Nova paste listeners");
        File directory = new File(instrument.getTargetContext().getExternalFilesDir(null), "clipboard-probe");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream out = new FileOutputStream(new File(directory,
                nativeMenu ? "page-editor-native.json" : keyboard ? "page-editor-keyboard.json" : "page-editor-system.json"))) {
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

    private void keyboardInput(Consumer<InputConnection> operation) throws Exception {
        AtomicReference<InputConnection> reference = new AtomicReference<>();
        main(() -> reference.set(web.onCreateInputConnection(new EditorInfo())));
        InputConnection connection = reference.get();
        assertTrue("Keyboard input connection available", connection != null);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable action = () -> {
            try { operation.accept(connection); }
            catch (Throwable error) { failure.set(error); }
            finally { completed.countDown(); }
        };
        Handler handler = connection.getHandler();
        if (handler == null) main(action); else handler.post(action);
        assertTrue("Keyboard input call returns without blocking on the long draft", completed.await(5, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private void selectRangeForPaste() {
        AtomicBoolean accepted = new AtomicBoolean();
        main(() -> {
            InputConnection connection = web.onCreateInputConnection(new EditorInfo());
            accepted.set(connection != null && connection.setSelection(2, 4));
        });
        assertTrue("IME selected range accepted", accepted.get());
        SystemClock.sleep(100);
        waitFor("selected editor range 2 to 4", () -> "true".equals(js("""
                (() => {
                  try {
                    const r = getSelection().getRangeAt(0);
                    const e = document.getElementById('prompt-textarea');
                    return e.contains(r.commonAncestorContainer) && r.startOffset === 2 && r.endOffset === 4;
                  } catch (ignored) { return false; }
                })()
                """)));
    }

    private void systemPaste() {
        AtomicBoolean selectionAccepted = new AtomicBoolean();
        AtomicBoolean accepted = new AtomicBoolean();
        main(() -> {
            InputConnection connection = web.onCreateInputConnection(new EditorInfo());
            if (connection != null) {
                // A JS Range alone can race Chromium's cached IME selection.
                // Establish the selected "cd" through the same OS input path
                // that performs the paste, in renderer command order.
                selectionAccepted.set(connection.setSelection(2, 4));
                accepted.set(connection.performContextMenuAction(android.R.id.paste));
            }
        });
        assertTrue("IME selected range accepted", selectionAccepted.get());
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
