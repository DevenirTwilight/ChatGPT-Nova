package com.example.chatgptnova;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.SystemClock;
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
