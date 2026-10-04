package com.example.chatgptnova;

import android.content.Context;
import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.TextAttribute;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import java.util.ArrayDeque;
import java.util.function.BooleanSupplier;

/** Handles the IME clipboard route, which does not dispatch a DOM paste event. */
final class ComposerWebView extends WebView {
    interface BulkCommit {
        void insert(String text, ValueCallback<Boolean> complete);
    }

    private final BulkCommit bulkCommit;
    private volatile int documentGeneration;

    ComposerWebView(Context context, BulkCommit bulkCommit) {
        super(context);
        this.bulkCommit = bulkCommit;
    }

    void navigationStarted() { documentGeneration++; }

    @Override public void destroy() {
        navigationStarted();
        super.destroy();
    }

    @Override public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        InputConnection connection = super.onCreateInputConnection(editorInfo);
        Uri page = Uri.parse(getUrl() == null ? "" : getUrl());
        int kind = editorInfo.inputType & InputType.TYPE_MASK_CLASS;
        int variation = editorInfo.inputType & InputType.TYPE_MASK_VARIATION;
        boolean password = kind == InputType.TYPE_CLASS_TEXT
                && (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                || kind == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        // Password fields and other origins never enter the adapter at all.
        if (connection == null || password || !MainActivity.isTrustedOrigin(page)
                || !"chatgpt.com".equalsIgnoreCase(page.getHost())) return connection;
        return connection == null ? null : new BulkConnection(connection);
    }

    private final class BulkConnection extends InputConnectionWrapper {
        private final int generation = documentGeneration;
        private final Handler inputHandler;
        private final ArrayDeque<Command> commands = new ArrayDeque<>();
        private boolean waiting;
        private boolean closed;
        private boolean composing;

        BulkConnection(InputConnection connection) {
            super(connection, false);
            Handler handler = connection.getHandler();
            inputHandler = handler == null ? new Handler(Looper.getMainLooper()) : handler;
        }

        private boolean current() { return !closed && generation == documentGeneration; }

        // A JS transaction completes asynchronously. Keep following IME edits in
        // order until it has finished; otherwise a queued keystroke can overtake
        // the large commit and be inserted at the previous caret.
        private synchronized boolean edit(BooleanSupplier operation) {
            if (!current()) return false;
            if (!waiting) return operation.getAsBoolean();
            commands.add(done -> { operation.getAsBoolean(); done.run(); });
            return true;
        }

        private synchronized void next() {
            if (!current()) { commands.clear(); waiting = false; return; }
            Command command = commands.poll();
            if (command == null) { waiting = false; return; }
            inputHandler.post(() -> {
                synchronized (BulkConnection.this) {
                    if (!current()) { next(); return; }
                }
                command.run(this::next);
            });
        }

        private boolean commit(CharSequence text, int cursor, BooleanSupplier original) {
            // Ordinary typing, non-default cursor semantics and active IME
            // composition retain the original connection. Firefox similarly
            // batches a whole replacement, but keeps composing commits native:
            // GeckoInputConnection.commitText, revision 66b70484 (2026-09-30).
            boolean bulk = text != null && cursor == 1 && (text.length() >= 1024
                    || (text.length() > 1 && text.toString().indexOf('\n') >= 0));
            if (!bulk) return edit(() -> {
                boolean accepted = original.getAsBoolean();
                composing = false;
                return accepted;
            });
            String plainText = text.toString();
            synchronized (this) {
                if (!current()) return false;
                commands.add(done -> {
                    if (composing) {
                        original.getAsBoolean();
                        composing = false;
                        done.run();
                        return;
                    }
                    post(() -> {
                        synchronized (BulkConnection.this) {
                            if (!current()) { done.run(); return; }
                        }
                        bulkCommit.insert(plainText, handled -> inputHandler.post(() -> {
                            synchronized (BulkConnection.this) {
                                if (current() && !Boolean.TRUE.equals(handled)) original.getAsBoolean();
                            }
                            done.run();
                        }));
                    });
                });
                if (!waiting) { waiting = true; next(); }
            }
            return true;
        }

        @Override public boolean commitText(CharSequence text, int cursor) {
            return commit(text, cursor, () -> super.commitText(text, cursor));
        }

        @TargetApi(33)
        @Override public boolean commitText(CharSequence text, int cursor, TextAttribute attributes) {
            return commit(text, cursor, () -> super.commitText(text, cursor, attributes));
        }

        @Override public boolean setComposingText(CharSequence text, int cursor) {
            return edit(() -> { composing = text != null && text.length() > 0;
                return super.setComposingText(text, cursor); });
        }

        @TargetApi(33)
        @Override public boolean setComposingText(CharSequence text, int cursor, TextAttribute attributes) {
            return edit(() -> { composing = text != null && text.length() > 0;
                return super.setComposingText(text, cursor, attributes); });
        }

        @Override public boolean setComposingRegion(int start, int end) {
            return edit(() -> { composing = start != end; return super.setComposingRegion(start, end); });
        }

        @TargetApi(33)
        @Override public boolean setComposingRegion(int start, int end, TextAttribute attributes) {
            return edit(() -> { composing = start != end; return super.setComposingRegion(start, end, attributes); });
        }

        @Override public boolean finishComposingText() {
            return edit(() -> { composing = false; return super.finishComposingText(); });
        }

        @Override public boolean setSelection(int start, int end) {
            return edit(() -> super.setSelection(start, end));
        }

        @Override public boolean deleteSurroundingText(int before, int after) {
            return edit(() -> super.deleteSurroundingText(before, after));
        }

        @Override public boolean deleteSurroundingTextInCodePoints(int before, int after) {
            return edit(() -> super.deleteSurroundingTextInCodePoints(before, after));
        }

        @Override public boolean sendKeyEvent(KeyEvent event) {
            return edit(() -> super.sendKeyEvent(event));
        }

        @Override public boolean performContextMenuAction(int id) {
            return edit(() -> super.performContextMenuAction(id));
        }

        @Override public boolean performEditorAction(int action) {
            return edit(() -> super.performEditorAction(action));
        }

        @Override public boolean beginBatchEdit() { return edit(super::beginBatchEdit); }
        @Override public boolean endBatchEdit() { return edit(super::endBatchEdit); }

        @Override public synchronized void closeConnection() {
            closed = true;
            commands.clear();
            super.closeConnection();
        }
    }

    private interface Command { void run(Runnable complete); }
}
