package com.example.chatgptnova;

import android.content.Context;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.TextAttribute;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import androidx.annotation.RequiresApi;
import java.util.ArrayDeque;
import java.util.function.BooleanSupplier;

/** Keeps keyboard clipboard commits on the same composer path as explicit paste. */
final class NovaWebView extends WebView {
    interface ComposerEditor {
        void prepare(ValueCallback<Boolean> result);
        void commit(String text, ValueCallback<Boolean> result);
        void paste(ValueCallback<Boolean> result);
    }

    private final ComposerEditor editor;
    private ComposerConnection connection;

    NovaWebView(Context context, ComposerEditor editor) {
        super(context);
        this.editor = editor;
    }

    @Override public InputConnection onCreateInputConnection(EditorInfo info) {
        InputConnection original = super.onCreateInputConnection(info);
        if (connection != null && connection.isActiveFor(original)) return connection;
        invalidateInputConnection();
        if (original == null) return null;
        connection = new ComposerConnection(original);
        return connection;
    }

    // A pending command belongs to the old editor, even when the URL stays the same.
    void invalidateInputConnection() {
        if (connection != null) connection.cancel();
        connection = null;
    }

    @Override public void destroy() {
        invalidateInputConnection();
        super.destroy();
    }

    private final class ComposerConnection extends InputConnectionWrapper {
        private final Object lock = new Object();
        private final ArrayDeque<BooleanSupplier> waiting = new ArrayDeque<>();
        private final Handler inputHandler;
        private final InputConnection original;
        private boolean active = true;
        private boolean pending;

        ComposerConnection(InputConnection original) {
            super(original, false);
            this.original = original;
            Handler handler = original.getHandler();
            inputHandler = handler == null ? new Handler(getContext().getMainLooper()) : handler;
        }

        boolean isActiveFor(InputConnection candidate) {
            synchronized (lock) { return active && candidate == original; }
        }

        private boolean dispatch(BooleanSupplier command) {
            synchronized (lock) {
                if (!active) return false;
                if (pending) {
                    waiting.add(command);
                    return true;
                }
            }
            // Chromium can wait for a renderer response (e.g. endBatchEdit).
            // Its UI must remain able to close/recreate this connection meanwhile.
            return command.getAsBoolean();
        }

        private boolean compatibleCommit(CharSequence text, int cursor, BooleanSupplier nativeCommit) {
            // Keyboard clipboard history commonly uses commitText, without any DOM
            // paste event. Leave ordinary typing, Enter and active compositions to
            // Chromium; replacing a composition is different from inserting at a caret.
            if (text == null) return dispatch(nativeCommit);
            String plainText = text.toString();
            return dispatch(() -> {
                if (cursor == 1 && (plainText.length() >= 4096
                        || (plainText.length() > 1 && (plainText.indexOf('\n') >= 0
                        || plainText.indexOf('\r') >= 0)))) {
                    return compatibility(result -> editor.commit(plainText, result), nativeCommit);
                }
                return nativeCommit.getAsBoolean();
            });
        }

        private boolean compatibility(java.util.function.Consumer<ValueCallback<Boolean>> action,
                                      BooleanSupplier fallback) {
            synchronized (lock) { pending = true; }
            boolean posted = NovaWebView.this.post(() -> {
                synchronized (lock) { if (!active) return; }
                try {
                    // This renderer round trip also lets earlier native selection
                    // edits settle before the composer insertion is evaluated.
                    editor.prepare(ready -> {
                        synchronized (lock) { if (!active) return; }
                        if (!Boolean.TRUE.equals(ready)) { complete(false, fallback); return; }
                        try { action.accept(handled -> complete(Boolean.TRUE.equals(handled), fallback)); }
                        catch (RuntimeException error) { complete(false, fallback); }
                    });
                }
                catch (RuntimeException error) { complete(false, fallback); }
            });
            if (!posted) synchronized (lock) { pending = false; }
            return posted;
        }

        private void complete(boolean handled, BooleanSupplier fallback) {
            // Never block the UI waiting for JS, and resume on Chromium's own IME
            // handler. Later keystrokes/batch edits cannot overtake a pending paste.
            inputHandler.post(() -> {
                synchronized (lock) { if (!active) return; }
                if (!handled) fallback.getAsBoolean();
                synchronized (lock) { pending = false; }
                while (true) {
                    BooleanSupplier next;
                    synchronized (lock) {
                        if (!active || pending || waiting.isEmpty()) return;
                        next = waiting.remove();
                    }
                    next.getAsBoolean();
                }
            });
        }

        void cancel() {
            synchronized (lock) { active = false; waiting.clear(); }
        }

        @Override public boolean commitText(CharSequence text, int cursor) {
            return compatibleCommit(text, cursor, () -> super.commitText(text, cursor));
        }

        @RequiresApi(33)
        @Override public boolean commitText(CharSequence text, int cursor, TextAttribute attribute) {
            return compatibleCommit(text, cursor, () -> super.commitText(text, cursor, attribute));
        }

        @Override public boolean performContextMenuAction(int id) {
            return dispatch(() -> {
                if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText)
                    return compatibility(editor::paste, () -> super.performContextMenuAction(id));
                return super.performContextMenuAction(id);
            });
        }

        @Override public boolean setComposingText(CharSequence text, int cursor) {
            return dispatch(() -> super.setComposingText(text, cursor));
        }

        @RequiresApi(33)
        @Override public boolean setComposingText(CharSequence text, int cursor, TextAttribute attribute) {
            return dispatch(() -> super.setComposingText(text, cursor, attribute));
        }

        @Override public boolean setComposingRegion(int start, int end) {
            return dispatch(() -> super.setComposingRegion(start, end));
        }

        @RequiresApi(33)
        @Override public boolean setComposingRegion(int start, int end, TextAttribute attribute) {
            return dispatch(() -> super.setComposingRegion(start, end, attribute));
        }

        @Override public boolean finishComposingText() {
            return dispatch(super::finishComposingText);
        }

        @Override public boolean beginBatchEdit() { return dispatch(super::beginBatchEdit); }
        @Override public boolean endBatchEdit() { return dispatch(super::endBatchEdit); }
        @Override public boolean setSelection(int start, int end) {
            return dispatch(() -> super.setSelection(start, end));
        }
        @Override public boolean deleteSurroundingText(int before, int after) {
            return dispatch(() -> super.deleteSurroundingText(before, after));
        }
        @Override public boolean deleteSurroundingTextInCodePoints(int before, int after) {
            return dispatch(() -> super.deleteSurroundingTextInCodePoints(before, after));
        }
        @Override public boolean sendKeyEvent(KeyEvent event) { return dispatch(() -> super.sendKeyEvent(event)); }
        @Override public boolean performEditorAction(int action) { return dispatch(() -> super.performEditorAction(action)); }
        @Override public boolean commitCompletion(CompletionInfo info) { return dispatch(() -> super.commitCompletion(info)); }
        @Override public boolean commitCorrection(CorrectionInfo info) { return dispatch(() -> super.commitCorrection(info)); }

        @RequiresApi(34)
        @Override public boolean replaceText(int start, int end, CharSequence text, int cursor, TextAttribute attribute) {
            return dispatch(() -> super.replaceText(start, end, text, cursor, attribute));
        }

        @Override public void closeConnection() {
            cancel();
            super.closeConnection();
        }
    }
}
