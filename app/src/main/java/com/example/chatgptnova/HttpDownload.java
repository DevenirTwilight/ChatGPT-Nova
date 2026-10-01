package com.example.chatgptnova;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Streams a user-selected HTTPS download. Cookies never cross the original ChatGPT origin. */
final class HttpDownload {
    interface Connections { HttpURLConnection open(URL url) throws Exception; }
    private final Activity activity;
    private final String address;
    private final String userAgent;
    private final Connections connections;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AtomicBoolean canceled = new AtomicBoolean();
    private volatile HttpURLConnection active;
    private boolean started;

    HttpDownload(Activity activity, String address, String userAgent) {
        this(activity, address, userAgent, url -> (HttpURLConnection) url.openConnection());
    }

    HttpDownload(Activity activity, String address, String userAgent, Connections connections) {
        this.activity = activity; this.address = address; this.userAgent = userAgent; this.connections = connections;
    }

    static boolean allowed(URL url) {
        return "https".equalsIgnoreCase(url.getProtocol()) && url.getHost() != null
                && !url.getHost().isEmpty() && url.getUserInfo() == null;
    }

    private static boolean sameOrigin(URL first, URL next) {
        int firstPort = first.getPort() == -1 ? 443 : first.getPort();
        int nextPort = next.getPort() == -1 ? 443 : next.getPort();
        return first.getProtocol().equalsIgnoreCase(next.getProtocol())
                && first.getHost().equalsIgnoreCase(next.getHost()) && firstPort == nextPort;
    }

    void saveTo(Uri destination, Runnable completion) {
        if (started || canceled.get()) return;
        started = true;
        io.execute(() -> {
            boolean success = false;
            try {
                if (!"content".equalsIgnoreCase(destination.getScheme())) throw new java.io.IOException();
                URL current = new URL(address);
                final URL original = current;
                boolean cookiesAllowed = MainActivity.isTrustedOrigin(Uri.parse(address));
                HashSet<String> visited = new HashSet<>();
                for (int redirects = 0; redirects <= 8; redirects++) {
                    if (canceled.get() || !allowed(current) || !visited.add(current.toString())) throw new java.io.IOException();
                    HttpURLConnection connection = connections.open(current);
                    active = connection;
                    connection.setInstanceFollowRedirects(false);
                    connection.setConnectTimeout(30000); connection.setReadTimeout(30000);
                    connection.setRequestProperty("Accept-Encoding", "identity");
                    if (userAgent != null && !userAgent.isEmpty()) connection.setRequestProperty("User-Agent", userAgent);
                    if (cookiesAllowed && sameOrigin(original, current)) {
                        String cookies = CookieManager.getInstance().getCookie(current.toString());
                        if (cookies != null && !cookies.isEmpty()) connection.setRequestProperty("Cookie", cookies);
                    }
                    int status = connection.getResponseCode();
                    if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                        String location = connection.getHeaderField("Location");
                        if (location == null || redirects == 8) throw new java.io.IOException();
                        URL next = new URL(current, location);
                        // Once a redirect leaves the original origin, even a later return gets no Cookie.
                        if (!sameOrigin(original, next)) cookiesAllowed = false;
                        connection.disconnect(); active = null;
                        current = next;
                        continue;
                    }
                    if (status < 200 || status >= 300) throw new java.io.IOException();
                    long expected = connection.getContentLengthLong(), count = 0;
                    try (InputStream input = connection.getInputStream();
                         OutputStream output = activity.getContentResolver().openOutputStream(destination, "w")) {
                        if (output == null) throw new java.io.IOException();
                        byte[] buffer = new byte[64 * 1024];
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            if (canceled.get()) throw new java.io.IOException();
                            output.write(buffer, 0, read); count += read;
                        }
                        if (expected >= 0 && expected != count) throw new java.io.IOException();
                        output.flush();
                    }
                    success = !canceled.get();
                    break;
                }
            } catch (Exception ignored) { /* Never log URLs, cookies or authentication failures. */ }
            finally {
                HttpURLConnection connection = active;
                if (connection != null) connection.disconnect();
                active = null;
                final boolean saved = success;
                main.post(() -> {
                    if (!activity.isDestroyed()) Toast.makeText(activity,
                            saved ? "下载已保存。" : "下载未完成，请重试；所选位置可能留有不完整文件。",
                            Toast.LENGTH_LONG).show();
                    completion.run();
                });
                io.shutdown();
            }
        });
    }

    void cancel() {
        canceled.set(true);
        HttpURLConnection connection = active;
        if (connection != null) connection.disconnect();
        if (!started) io.shutdown();
    }
}
