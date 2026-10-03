package com.example.chatgptnova;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.SystemClock;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
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
        js("""
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
                  // A document handler must also run before Nova's fallback.
                  document.addEventListener('paste', event => {
                    if (event.target !== e) return;
                    const started = performance.now();
                    event.preventDefault();
                    fixturePasteCount++;
                    const text = event.clipboardData.getData('text/plain');
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
                    requestAnimationFrame(() => requestAnimationFrame(() => {
                      fixturePaintMs = performance.now() - started;
                    }));
                  });
                  window.fixtureUndo = () => {
                    fixtureModel = fixtureHistory.pop();
                    e.textContent = fixtureModel;
                    return fixtureModel;
                  };
                  const r = document.createRange();
                  r.setStart(e.firstChild, 2);
                  r.setEnd(e.firstChild, 4);
                  getSelection().removeAllRanges();
                  getSelection().addRange(r);
                  return true;
                })()
                """);
        String text = "# Heading\n\n中文 English 😀 <script> & literal\n- **bold**\n\n".repeat(4096);
        putClipboard(text);
        SystemClock.sleep(400); // Let Chromium observe the new OS clipboard value.
        if (nativeMenu) {
            click("菜单");
            click("从剪贴板粘贴");
        } else {
            AtomicBoolean accepted = new AtomicBoolean();
            main(() -> {
                InputConnection connection = web.onCreateInputConnection(new EditorInfo());
                accepted.set(connection != null
                        && connection.performContextMenuAction(android.R.id.paste));
            });
            assertTrue("System paste command accepted", accepted.get());
        }
        waitFor("page editor long paste painted", () -> "true".equals(js("fixturePaintMs !== null")));
        assertEquals("one page transaction", "1", js("fixturePasteCount"));
        assertEquals("handled paste must bypass native HTML editing", "0", js("fixtureNativeEdits"));
        assertEquals("ab" + text + "ef", js("fixtureModel"));
        assertEquals("ab" + text + "ef", js("document.getElementById('prompt-textarea').innerText"));
        assertEquals("plain text never creates executable elements", "0", js(
                "document.getElementById('prompt-textarea').querySelectorAll('script').length"));
        assertEquals(String.valueOf(2 + text.length()), js("getSelection().anchorOffset"));
        double paintMs = Double.parseDouble(js("fixturePaintMs"));
        JSONObject result = new JSONObject().put("page", "test-owned paste transaction and history")
                .put("route", nativeMenu ? "native-menu" : "system-paste")
                .put("utf8Bytes", text.getBytes(StandardCharsets.UTF_8).length)
                .put("utf16Units", text.length()).put("paintMs", paintMs);
        File directory = new File(instrument.getTargetContext().getExternalFilesDir(null), "clipboard-probe");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream out = new FileOutputStream(new File(directory,
                nativeMenu ? "page-editor-native.json" : "page-editor-system.json"))) {
            out.write(result.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        assertTrue("Long paste transaction and paint took " + paintMs + " ms", paintMs < 3000);
        assertEquals("one undo restores the pre-paste draft", "abcdef", js("fixtureUndo()"));
        assertEquals("abcdef", js("document.getElementById('prompt-textarea').innerText"));
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
