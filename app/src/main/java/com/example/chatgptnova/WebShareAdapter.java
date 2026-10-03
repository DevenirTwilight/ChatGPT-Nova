package com.example.chatgptnova;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.webkit.WebView;
import androidx.core.content.ContextCompat;
import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.ScriptHandler;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.util.Collections;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.json.JSONObject;

/** One-purpose, origin-scoped Web Share transport; never exposes a Java object. */
final class WebShareAdapter {
    private static final String CHANNEL = "NovaWebShare";
    private static final int MAX_MESSAGE = 65536;
    private static int nextRequestCode = 0x7000;
    private static final String INSTALL = """
            (() => {
              if (window !== window.top || location.origin !== 'https://chatgpt.com'
                  || typeof navigator.share === 'function') return;
              const channel = window.NovaWebShare;
              if (!channel || typeof channel.postMessage !== 'function') return;
              let pending = null, serial = 0;
              const error = (name, message) => new DOMException(message, name);
              const normalize = data => {
                if (!data || typeof data !== 'object' || (data.files && data.files.length))
                  throw new TypeError('Only title, text and ChatGPT URLs are supported');
                const value = {title: '', text: '', url: ''};
                for (const key of ['title', 'text', 'url'])
                  if (data[key] !== undefined) value[key] = String(data[key]);
                if (!value.title && !value.text && !value.url) throw new TypeError('Share data is empty');
                if (value.url) {
                  const url = new URL(value.url, document.baseURI);
                  if (url.origin !== 'https://chatgpt.com' || url.username || url.password)
                    throw new TypeError('Only trusted ChatGPT URLs can be shared');
                  value.url = url.href;
                }
                if (JSON.stringify(value).length > 65000) throw new TypeError('Share data is too large');
                return value;
              };
              channel.onmessage = event => {
                let result;
                try { result = JSON.parse(event.data); } catch (ignored) { return; }
                if (!pending || result.id !== pending.id) return;
                const request = pending;
                pending = null;
                if (result.error) request.reject(error(result.error, 'Native share did not complete'));
                else request.resolve();
              };
              const share = data => {
                if (pending) return Promise.reject(error('InvalidStateError', 'A share is already pending'));
                let value;
                try { value = normalize(data); } catch (failure) { return Promise.reject(failure); }
                if (navigator.userActivation && !navigator.userActivation.isActive)
                  return Promise.reject(error('NotAllowedError', 'Sharing requires a user gesture'));
                return new Promise((resolve, reject) => {
                  const id = Date.now() + '-' + (++serial);
                  pending = {id, resolve, reject};
                  try { channel.postMessage(JSON.stringify({kind: 'share', id, ...value})); }
                  catch (failure) { pending = null; reject(error('AbortError', 'Native share is unavailable')); }
                });
              };
              Object.defineProperty(channel, 'share', {
                value: (title, text, url) => share({title, text, url}), configurable: true
              });
              Object.defineProperty(navigator, 'share', {value: share, configurable: true});
              if (typeof navigator.canShare !== 'function') Object.defineProperty(navigator, 'canShare', {
                configurable: true,
                value: data => { try { normalize(data); return true; } catch (ignored) { return false; } }
              });
            })()
            """;

    private final Activity activity;
    private final WebView web;
    private final BooleanSupplier active;
    private boolean installed;
    private ScriptHandler script;
    private Request pending;

    WebShareAdapter(Activity activity, WebView web, BooleanSupplier active) {
        this.activity = activity;
        this.web = web;
        this.active = active;
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return;
        WebViewCompat.addWebMessageListener(web, CHANNEL, Collections.singleton("https://chatgpt.com"),
                (view, message, sourceOrigin, isMainFrame, reply) -> {
                    String id = "";
                    try {
                        String raw = message.getData();
                        if (raw == null || raw.length() > MAX_MESSAGE) return;
                        JSONObject data = new JSONObject(raw);
                        id = data.getString("id");
                        if (id.isEmpty() || id.length() > 100) return;
                        if (view != web || !isMainFrame || !active.getAsBoolean()
                                || !trusted(sourceOrigin) || !trusted(Uri.parse(web.getUrl() == null ? "" : web.getUrl()))) {
                            reply(reply, id, "NotAllowedError"); return;
                        }
                        if (!"share".equals(data.getString("kind"))) {
                            reply(reply, id, "TypeError"); return;
                        }
                        String title = data.getString("title"), text = data.getString("text"), url = data.getString("url");
                        if ((title.isEmpty() && text.isEmpty() && url.isEmpty()) || data.has("files")
                                || (!url.isEmpty() && !trusted(Uri.parse(url)))) {
                            reply(reply, id, "TypeError"); return;
                        }
                        launch(reply, id, title, text, url);
                    } catch (Exception malformed) { reply(reply, id, "TypeError"); }
                });
        installed = true;
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            script = WebViewCompat.addDocumentStartJavaScript(web, INSTALL,
                    Collections.singleton("https://chatgpt.com"));
        }
    }

    private static boolean trusted(Uri uri) {
        return MainActivity.isTrustedOrigin(uri) && "chatgpt.com".equalsIgnoreCase(uri.getHost());
    }

    void pageFinished() {
        // Also supports providers with WebMessageListener but without document-start scripts.
        if (installed && active.getAsBoolean() && trusted(Uri.parse(web.getUrl() == null ? "" : web.getUrl())))
            web.evaluateJavascript(INSTALL, null);
    }

    private void launch(JavaScriptReplyProxy proxy, String id, String title, String text, String url) {
        if (pending != null) { reply(proxy, id, "InvalidStateError"); return; }
        Request request = new Request(proxy, id);
        pending = request;
        String action = activity.getPackageName() + ".WEB_SHARE." + UUID.randomUUID();
        request.receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (pending == request && action.equals(intent.getAction())) complete(null);
            }
        };
        try {
            ContextCompat.registerReceiver(activity, request.receiver, new IntentFilter(action),
                    ContextCompat.RECEIVER_NOT_EXPORTED);
            request.registered = true;
            // No target identity is read; an immutable callback only reports target selection.
            request.callback = PendingIntent.getBroadcast(activity, request.code,
                    new Intent(action).setPackage(activity.getPackageName()),
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
            String body = text.isEmpty() ? url : url.isEmpty() ? text : text + "\n" + url;
            Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, body).putExtra(Intent.EXTRA_TITLE, title)
                    .putExtra(Intent.EXTRA_SUBJECT, title);
            activity.startActivityForResult(Intent.createChooser(send, "分享", request.callback.getIntentSender()), request.code);
        } catch (ActivityNotFoundException unavailable) { complete("AbortError"); }
        catch (RuntimeException failure) { complete("DataError"); }
    }

    boolean activityResult(int code, int result) {
        if (pending == null || pending.code != code) return false;
        // Target selection completes through the callback above, even when its Activity
        // result is CANCELED. A remaining request means the chooser itself was dismissed.
        complete(result == Activity.RESULT_OK ? null : "AbortError");
        return true;
    }

    void navigationStarted() { complete("AbortError"); }

    void destroy() {
        complete("AbortError");
        if (script != null) { script.remove(); script = null; }
        if (installed && WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))
            WebViewCompat.removeWebMessageListener(web, CHANNEL);
        installed = false;
    }

    private void complete(String error) {
        Request request = pending;
        if (request == null) return;
        pending = null;
        if (request.registered) activity.unregisterReceiver(request.receiver);
        if (request.callback != null) request.callback.cancel();
        reply(request.proxy, request.id, error);
    }

    private static void reply(JavaScriptReplyProxy proxy, String id, String error) {
        try {
            JSONObject result = new JSONObject().put("id", id);
            if (error != null) result.put("error", error);
            proxy.postMessage(result.toString());
        } catch (Exception detachedDocument) { /* The originating frame may have gone away. */ }
    }

    private static final class Request {
        final JavaScriptReplyProxy proxy;
        final String id;
        final int code;
        BroadcastReceiver receiver;
        PendingIntent callback;
        boolean registered;
        Request(JavaScriptReplyProxy proxy, String id) {
            this.proxy = proxy; this.id = id;
            code = nextRequestCode++;
            if (nextRequestCode > 0x7fff) nextRequestCode = 0x7000;
        }
    }
}
