package com.example.chatgptnova;

import android.content.Context;
import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.InputContentInfo;
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
        if (Build.VERSION.SDK_INT >= 34) return new Api34Connection(connection);
        if (Build.VERSION.SDK_INT >= 33) return new Api33Connection(connection);
        return new BulkConnection(connection);
    }

    private class BulkConnection extends InputConnectionWrapper {
        private final int generation = documentGeneration;
        private final Handler inputHandler;
        private final ArrayDeque<Command> commands = new ArrayDeque<>();
        private boolean waiting;
        private boolean closed;
        protected boolean composing;

        BulkConnection(InputConnection connection) {
            super(connection, false);
            Handler handler = connection.getHandler();
            inputHandler = handler == null ? new Handler(Looper.getMainLooper()) : handler;
        }

        private boolean current() { return !closed && generation == documentGeneration; }

        // A JS transaction completes asynchronously. Keep following IME edits in
        // order until it has finished; otherwise a queued keystroke can overtake
        // the large commit and be inserted at the previous caret.
        protected synchronized boolean edit(BooleanSupplier operation) {
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

        protected boolean commit(CharSequence text, int cursor, BooleanSupplier original) {
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
                    // Match the browser's batch-edit boundary as well as the
                    // editor's single transaction. Intermediate DOM/selection
                    // changes must not trigger separate IME synchronization.
                    boolean batched = super.beginBatchEdit();
                    Runnable finish = () -> {
                        if (batched) super.endBatchEdit();
                        done.run();
                    };
                    post(() -> {
                        synchronized (BulkConnection.this) {
                            if (!current()) { inputHandler.post(finish); return; }
                        }
                        bulkCommit.insert(plainText, handled -> inputHandler.post(() -> {
                            try {
                                synchronized (BulkConnection.this) {
                                    if (current() && !Boolean.TRUE.equals(handled)) original.getAsBoolean();
                                }
                            } finally { finish.run(); }
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

        @Override public boolean setComposingText(CharSequence text, int cursor) {
            return edit(() -> { composing = text != null && text.length() > 0;
                return super.setComposingText(text, cursor); });
        }

        @Override public boolean setComposingRegion(int start, int end) {
            return edit(() -> { composing = start != end; return super.setComposingRegion(start, end); });
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

        @Override public boolean commitCompletion(CompletionInfo completion) {
            return edit(() -> super.commitCompletion(completion));
        }

        @Override public boolean commitCorrection(CorrectionInfo correction) {
            return edit(() -> super.commitCorrection(correction));
        }

        @Override public boolean commitContent(InputContentInfo content, int flags, Bundle options) {
            return edit(() -> super.commitContent(content, flags, options));
        }

        @Override public boolean performPrivateCommand(String action, Bundle data) {
            return edit(() -> super.performPrivateCommand(action, data));
        }

        @Override public boolean beginBatchEdit() { return edit(super::beginBatchEdit); }
        @Override public boolean endBatchEdit() { return edit(super::endBatchEdit); }

        @Override public synchronized void closeConnection() {
            closed = true;
            commands.clear();
            super.closeConnection();
        }
    }

    // Keep newer SDK types out of the connection instantiated on Android 8-12.
    @TargetApi(33)
    private class Api33Connection extends BulkConnection {
        Api33Connection(InputConnection connection) { super(connection); }

        @Override public boolean commitText(CharSequence text, int cursor, TextAttribute attributes) {
            return commit(text, cursor, () -> super.commitText(text, cursor, attributes));
        }

        @Override public boolean setComposingText(CharSequence text, int cursor, TextAttribute attributes) {
            return edit(() -> { composing = text != null && text.length() > 0;
                return super.setComposingText(text, cursor, attributes); });
        }

        @Override public boolean setComposingRegion(int start, int end, TextAttribute attributes) {
            return edit(() -> { composing = start != end; return super.setComposingRegion(start, end, attributes); });
        }
    }

    @TargetApi(34)
    private final class Api34Connection extends Api33Connection {
        Api34Connection(InputConnection connection) { super(connection); }

        @Override public boolean replaceText(int start, int end, CharSequence text,
                                             int cursor, TextAttribute attributes) {
            // Android specifies this as finish-composition, select, commit.
            // Route that standard bulk-text entry through the same adapter,
            // regardless of which keyboard called it.
            beginBatchEdit();
            try {
                finishComposingText();
                setSelection(start, end);
                return commitText(text, cursor, attributes);
            } finally { endBatchEdit(); }
        }
    }

    private interface Command { void run(Runnable complete); }
}
