package com.example.chatgptnova;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceResponse;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.window.OnBackInvokedDispatcher;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;
import java.util.Arrays;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int WEB_PERMISSIONS = 1002;
    private static final int SAVE_BLOB = 1005;
    private static final String HOME = "https://chatgpt.com/";
    private static final String LOGIN = "https://chatgpt.com/auth/login";
    private enum AccountUiState { UNKNOWN, SIGNED_OUT, SIGNED_IN }
    // Read only visible public controls. Never read input values, cookies,
    // storage, account details, or private authentication endpoints.
    private static final String ACCOUNT_STATE_QUERY = """
            (() => {
              const visible = e => !!(e.getClientRects().length) &&
                getComputedStyle(e).visibility !== 'hidden' && getComputedStyle(e).display !== 'none';
              const profiles = document.querySelectorAll('[data-testid="accounts-profile-button"], [data-testid="profile-button"], [data-testid="user-menu-button"], [data-testid="account-menu-button"]');
              if ([...profiles].some(visible)) return 'signed_in';
              const controls = [...document.querySelectorAll('button, a, [role="button"]')].slice(0, 250);
              const login = controls.some(e => {
                if (!visible(e)) return false;
                if (e.getAttribute('data-testid') === 'login-button') return true;
                const label = (e.getAttribute('aria-label') || e.textContent || '').replace(/\\s+/g, ' ').trim();
                return /^(log in|login|sign in|登录|登入|se connecter|connexion|anmelden|iniciar sesión|accedi|로그인|ログイン)$/i.test(label);
              });
              return login ? 'signed_out' : 'unknown';
            })()
            """;

    private WebView webView;
    private ProgressBar progress;
    private PermissionRequest pendingWebPermission;
    private UploadController uploads;
    private BlobDownload blobDownload;
    private HttpDownload httpDownload;
    private TextView origin;
    private LinearLayout errorPanel;
    private TextView errorMessage;
    private String failedUrl;
    private String loadingUrl;
    private boolean clearing;
    private boolean loginDialogVisible;
    private AlertDialog clearDialog;
    private AlertDialog settingsDialog;
    private View menuButton;
    private PopupMenu overflowMenu;
    private AccountUiState accountUiState = AccountUiState.UNKNOWN;
    private int accountQuerySerial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splash = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        splash.setOnExitAnimationListener(provider -> {
            provider.remove();
            applySystemBars();
        });
        uploads = new UploadController(this);
        buildUi();
        configureWebView();
        Bundle webState = savedInstanceState == null ? null : savedInstanceState.getBundle("nova.webState");
        boolean restored = false;
        try { restored = webState != null && webView.restoreState(webState) != null; }
        catch (RuntimeException ignored) { }
        if (!restored) {
            String last = savedInstanceState == null ? null : savedInstanceState.getString("nova.lastUrl");
            webView.loadUrl(safePage(last) ? last : HOME);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::navigateBack);
        }
    }

    private void applySystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(0xFFFFFFFF);
        getWindow().setNavigationBarColor(Build.VERSION.SDK_INT >= 27
                ? 0xFFFFFFFF : getColor(R.color.nova_night));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(Build.VERSION.SDK_INT >= 27);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);
        applySystemBars();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return new WindowInsetsCompat.Builder(insets)
                    .setInsets(WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime(), Insets.NONE).build();
        });

        FrameLayout bar = new FrameLayout(this);
        bar.setPadding(dp(12), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(0, 0, dp(52), 0);
        TextView title = new TextView(this);
        title.setText("ChatGPT Nova");
        title.setTextSize(15);
        title.setTextColor(0xFF111111);
        heading.addView(title);
        origin = new TextView(this);
        origin.setText("chatgpt.com");
        origin.setTextSize(12);
        origin.setTextColor(0xFF555555);
        origin.setSingleLine(true);
        origin.setEllipsize(android.text.TextUtils.TruncateAt.END);
        heading.addView(origin);
        bar.addView(heading, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));

        TextView menu = new TextView(this);
        menu.setText("⋮");
        menu.setTextSize(28);
        menu.setGravity(Gravity.CENTER);
        menu.setContentDescription("菜单");
        menu.setOnClickListener(v -> showMenu());
        menuButton = menu;
        FrameLayout.LayoutParams menuLp = new FrameLayout.LayoutParams(
                dp(48), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END);
        bar.addView(menu, menuLp);
        root.addView(bar, barLp);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        root.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));

        FrameLayout content = new FrameLayout(this);
        webView = new WebView(this);
        content.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        ScrollView errors = new ScrollView(this);
        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setPadding(dp(24), dp(24), dp(24), dp(24));
        errorPanel.setBackgroundColor(0xFFFFFFFF);
        errorMessage = new TextView(this);
        errorMessage.setTextSize(17);
        errorPanel.addView(errorMessage);
        Button retry = new Button(this);
        retry.setText("重试加载");
        retry.setOnClickListener(v -> { if (!clearing && webView != null) webView.loadUrl(safePage(failedUrl) ? failedUrl : HOME); });
        errorPanel.addView(retry);
        Button home = new Button(this);
        home.setText("返回 ChatGPT 首页");
        home.setOnClickListener(v -> { if (!clearing && webView != null) webView.loadUrl(HOME); });
        errorPanel.addView(home);
        Button browser = new Button(this);
        browser.setText("外部查看（独立会话）");
        browser.setOnClickListener(v -> openCurrentPageInBrowser());
        errorPanel.addView(browser);
        errors.addView(errorPanel);
        errors.setTag("nova.error");
        errors.setVisibility(View.GONE);
        content.addView(errors, new FrameLayout.LayoutParams(-1, -1));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
        ViewCompat.requestApplyInsets(root);
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSafeBrowsingEnabled(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        webView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                return clearing || handleUri(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return clearing || handleUri(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (view != webView || clearing) return;
                accountUiState = AccountUiState.UNKNOWN;
                accountQuerySerial++;
                cancelPageRequests();
                failedUrl = null;
                loadingUrl = url;
                errorContainer().setVisibility(View.GONE);
                displayOrigin(url);
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (view != webView) return;
                progress.setVisibility(View.GONE);
                displayOrigin(url);
                CookieManager.getInstance().flush();
                refreshAccountUiState(null);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (view == webView && request.isForMainFrame()) {
                    showLoadError(request.getUrl().toString(), "页面未能加载。请检查网络，然后重试。");
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                if (view == webView && (error.getUrl().equals(loadingUrl) || error.getUrl().equals(view.getUrl())))
                    showLoadError(error.getUrl(), "无法安全连接此页面。请检查设备时间和网络后重试。");
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            WebResourceResponse response) {
                if (view == webView && request.isForMainFrame() && response.getStatusCode() >= 400) {
                    Toast.makeText(MainActivity.this, "网页暂时不可用，请在 Nova 中刷新或重试。",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (view != webView) { view.destroy(); return true; }
                if (blobDownload != null) { blobDownload.cancel(); blobDownload = null; }
                cancelPageRequests();
                if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
                view.destroy();
                webView = null;
                buildUi();
                configureWebView();
                webView.loadUrl(HOME);
                Toast.makeText(MainActivity.this, "网页进程已重启。", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (view != webView || clearing) return;
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (clearing || webView != MainActivity.this.webView || !isTrustedOrigin(Uri.parse(webView.getUrl() == null ? "" : webView.getUrl()))) {
                    callback.onReceiveValue(null);
                    return true;
                }
                return uploads.show(callback, params);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> requestWebPermissions(request));
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                if (pendingWebPermission == request) pendingWebPermission = null;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
                requestDownload(url, userAgent, contentDisposition, mimeType));
    }

    private boolean handleUri(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) return false;
        if ("https".equalsIgnoreCase(scheme)) {
            String host = uri.getHost();
            if ("accounts.google.com".equalsIgnoreCase(host)) {
                showLoginHelp(true);
                return true;
            }
            return false;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            openInBrowser(uri);
            return true;
        }
        if ("file".equalsIgnoreCase(scheme) || "content".equalsIgnoreCase(scheme)
                || "javascript".equalsIgnoreCase(scheme) || "data".equalsIgnoreCase(scheme)) {
            return true;
        }

        try {
            Intent intent;
            if ("intent".equalsIgnoreCase(scheme)) {
                intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                String fallback = intent.getStringExtra("browser_fallback_url");
                intent.setComponent(null);
                intent.setSelector(null);
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setFlags(0);
                intent.removeExtra("browser_fallback_url");
                Uri target = intent.getData();
                if (target == null || "file".equalsIgnoreCase(target.getScheme())
                        || "content".equalsIgnoreCase(target.getScheme())
                        || "javascript".equalsIgnoreCase(target.getScheme())
                        || "data".equalsIgnoreCase(target.getScheme())
                        || "intent".equalsIgnoreCase(target.getScheme())) return true;
                if ("https".equalsIgnoreCase(target.getScheme()) || "http".equalsIgnoreCase(target.getScheme())) {
                    if (!handleUri(target)) webView.loadUrl(target.toString());
                    return true;
                }
                try { startActivity(intent); }
                catch (ActivityNotFoundException e) {
                    if (fallback != null && "https".equalsIgnoreCase(Uri.parse(fallback).getScheme())) {
                        Uri next = Uri.parse(fallback);
                        if (!handleUri(next)) webView.loadUrl(next.toString());
                    } else {
                        Toast.makeText(this, "没有应用可以打开这个链接。", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
            intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "没有应用可以打开这个链接。", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void showLoginHelp(boolean google) {
        if (loginDialogVisible) return;
        loginDialogVisible = true;
        new AlertDialog.Builder(this).setTitle(google ? "Google 登录" : "选择登录方式")
                .setIcon(R.mipmap.ic_launcher)
                .setMessage("可使用 Chrome / Brave 在浏览器中完成 Google 登录。打开 ChatGPT 官方登录页后，请选择“使用 Google 继续”。\n\n登录成功后可继续在浏览器聊天。浏览器与 Nova 的会话独立，回到 Nova 不会自动带入浏览器会话；浏览器已有账号可能影响登录。")
                .setNeutralButton("取消", null)
                .setNegativeButton("应用内登录", (d, w) -> openLoginInNova())
                .setPositiveButton("浏览器登录", (d, w) -> {
                    if (google) openLoginInNova();
                    openLoginInBrowser();
                })
                .setOnDismissListener(d -> loginDialogVisible = false).show();
    }

    private void openLoginInNova() {
        if (!clearing && webView != null) webView.loadUrl(LOGIN);
    }

    private void openLoginInBrowser() {
        if (!clearing) {
            // Start a fresh browser-owned flow. WebView OAuth URLs contain state
            // tied to WebView cookies, so forwarding them cannot complete login.
            openInBrowser(Uri.parse(LOGIN), true);
        }
    }

    static boolean isTrustedOrigin(Uri uri) {
        String host = uri == null ? null : uri.getHost();
        return uri != null && "https".equalsIgnoreCase(uri.getScheme()) && host != null
                && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getUserInfo() == null
                && ("chatgpt.com".equalsIgnoreCase(host)
                    || host.toLowerCase(java.util.Locale.ROOT).endsWith(".chatgpt.com"));
    }

    private void requestWebPermissions(PermissionRequest request) {
        if (clearing || webView == null || !isTrustedOrigin(request.getOrigin())
                || !isTrustedOrigin(Uri.parse(webView.getUrl() == null ? "" : webView.getUrl()))) { request.deny(); return; }
        if (pendingWebPermission != null) { request.deny(); return; }
        pendingWebPermission = request;
        boolean needCamera = false;
        boolean needMic = false;

        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) needCamera = true;
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) needMic = true;
        }

        java.util.ArrayList<String> needed = new java.util.ArrayList<>();
        if (needCamera && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CAMERA);
        }
        if (needMic && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO);
        }

        if (needed.isEmpty()) {
            finishWebPermission();
        } else {
            requestPermissions(needed.toArray(new String[0]), WEB_PERMISSIONS);
        }
    }

    private void finishWebPermission() {
        PermissionRequest request = pendingWebPermission;
        pendingWebPermission = null;
        if (request == null) return;
        ArrayList<String> granted = new ArrayList<>();
        if (webView != null && isTrustedOrigin(request.getOrigin())
                && isTrustedOrigin(Uri.parse(webView.getUrl() == null ? "" : webView.getUrl()))) {
            for (String resource : request.getResources()) {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                        && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    granted.add(resource);
                } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                        && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    granted.add(resource);
                }
            }
        }
        if (granted.isEmpty()) request.deny(); else request.grant(granted.toArray(new String[0]));
    }

    private void requestDownload(String url, String userAgent, String disposition, String mime) {
        if (url == null || clearing) return;
        if (webView == null || !isTrustedOrigin(Uri.parse(webView.getUrl() == null ? "" : webView.getUrl()))) {
            Uri external = Uri.parse(url);
            if ("https".equalsIgnoreCase(external.getScheme()) || "http".equalsIgnoreCase(external.getScheme())) openInBrowser(external);
            return;
        }
        if (blobDownload != null || httpDownload != null) {
            Toast.makeText(this, "请先完成当前下载。", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri address = Uri.parse(url);
        if ("blob".equalsIgnoreCase(address.getScheme())) {
            if (webView == null || !isTrustedOrigin(Uri.parse(webView.getUrl() == null ? "" : webView.getUrl()))) return;
            blobDownload = new BlobDownload(this, webView, url);
            BlobDownload pending = blobDownload;
            WebView page = webView;
            String fallback = DownloadNames.guess("https://chatgpt.com/Nova-download", null, mime);
            // DownloadListener omits the anchor's download attribute. Read only the
            // suggested filename of the exact user-clicked blob, with a bounded result.
            page.evaluateJavascript("(()=>{for(let a of document.querySelectorAll('a[download]'))"
                    + "if(a.href===" + JSONObject.quote(url) + ")return (a.getAttribute('download')||'').slice(0,160);return ''})()", result -> {
                if (clearing || webView != page || blobDownload != pending) return;
                String name = fallback;
                try {
                    Object value = new JSONTokener(result).nextValue();
                    if (value instanceof String && !((String) value).trim().isEmpty())
                        name = DownloadNames.sanitize((String) value);
                } catch (Exception ignored) { }
                openSaveLocation(name, mime);
            });
            return;
        } else if ("https".equalsIgnoreCase(address.getScheme()) && address.getUserInfo() == null) {
            httpDownload = new HttpDownload(this, url, userAgent);
        } else {
            if ("http".equalsIgnoreCase(address.getScheme())) openInBrowser(address);
            else Toast.makeText(this, "无法下载此类型链接。", Toast.LENGTH_LONG).show();
            return;
        }
        openSaveLocation(DownloadNames.guess(url, disposition, mime), mime);
    }

    private void openSaveLocation(String name, String mime) {
        String saveType = mime == null ? "" : mime.split(";", 2)[0].trim();
        if (!saveType.matches("[a-zA-Z0-9!#$&^_.+*-]+/[a-zA-Z0-9!#$&^_.+*-]+")) saveType = "application/octet-stream";
        Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType(saveType)
                .putExtra(Intent.EXTRA_TITLE, name);
        try { startActivityForResult(save, SAVE_BLOB); }
        catch (RuntimeException error) { cancelDownloads();
            Toast.makeText(this, "无法打开保存位置选择器。", Toast.LENGTH_LONG).show(); }
    }

    private void cancelDownloads() {
        if (blobDownload != null) { blobDownload.cancel(); blobDownload = null; }
        if (httpDownload != null) { httpDownload.cancel(); httpDownload = null; }
    }

    private void cancelPageRequests() {
        if (uploads != null) uploads.cancel();
        if (pendingWebPermission != null) { pendingWebPermission.deny(); pendingWebPermission = null; }
        if (blobDownload != null) { blobDownload.cancel(); blobDownload = null; }
    }

    private View errorContainer() { return (View) errorPanel.getParent(); }

    private void showLoadError(String url, String message) {
        if (clearing || isDestroyed()) return;
        failedUrl = safePage(url) ? url : HOME;
        errorMessage.setText(message);
        errorContainer().setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
        displayOrigin(failedUrl);
    }

    private void displayOrigin(String url) {
        Uri uri = Uri.parse(url == null ? HOME : url);
        String host = uri.getHost();
        origin.setText(host == null ? "chatgpt.com" : host + (uri.getPort() != -1 && uri.getPort() != 443 ? ":" + uri.getPort() : ""));
    }

    private static boolean safePage(String url) {
        if (url == null || url.length() > 16384) return false;
        Uri uri = Uri.parse(url);
        return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
    }

    private void showMenu() {
        if (clearing || webView == null) return;
        refreshAccountUiState(() -> {
            if (overflowMenu != null) overflowMenu.dismiss();
            overflowMenu = new PopupMenu(this, menuButton);
            Menu items = overflowMenu.getMenu();
            items.add(0, 1, 0, "刷新");
            items.add(0, 2, 1, "ChatGPT 首页");
            if (accountUiState == AccountUiState.SIGNED_OUT) items.add(0, 3, 2, "登录");
            items.add(0, 4, 3, "用浏览器打开");
            items.add(0, 5, 4, "设置");
            overflowMenu.setOnMenuItemClickListener(item -> {
                if (clearing || webView == null) return true;
                switch (item.getItemId()) {
                    case 1:
                        if (failedUrl != null) webView.loadUrl(failedUrl); else webView.reload();
                        break;
                    case 2:
                        webView.loadUrl(HOME);
                        break;
                    case 3:
                        showLoginHelp(false);
                        break;
                    case 4:
                        openCurrentPageInBrowser();
                        break;
                    case 5:
                        showSettings();
                        break;
                    default: break;
                }
                return true;
            });
            overflowMenu.show();
        });
    }

    private void refreshAccountUiState(Runnable finished) {
        WebView page = webView;
        String address = page == null ? null : page.getUrl();
        int serial = ++accountQuerySerial;
        accountUiState = AccountUiState.UNKNOWN;
        if (clearing || page == null || failedUrl != null || !isTrustedOrigin(Uri.parse(address == null ? "" : address))) {
            if (finished != null) finished.run();
            return;
        }
        boolean[] completed = {false};
        Runnable complete = () -> {
            if (completed[0]) return;
            completed[0] = true;
            if (!isFinishing() && !isDestroyed() && !clearing && webView != null && finished != null) finished.run();
        };
        // A failed or busy webpage must not prevent opening the native menu.
        page.postDelayed(complete, 350);
        try {
            page.evaluateJavascript(ACCOUNT_STATE_QUERY, value -> {
                if (completed[0]) return;
                if (serial == accountQuerySerial && page == webView && address.equals(page.getUrl())) {
                    if ("\"signed_in\"".equals(value)) accountUiState = AccountUiState.SIGNED_IN;
                    else if ("\"signed_out\"".equals(value)) accountUiState = AccountUiState.SIGNED_OUT;
                }
                complete.run();
            });
        } catch (RuntimeException error) { complete.run(); }
    }

    private void showSettings() {
        if (clearing || webView == null) return;
        refreshAccountUiState(() -> {
            ArrayList<String> items = new ArrayList<>();
            if (accountUiState == AccountUiState.SIGNED_IN) items.add("退出当前账号");
            items.add("清除登录与网站数据");
            items.add("登录帮助");
            items.add("关于 ChatGPT Nova");
            if (settingsDialog != null) settingsDialog.dismiss();
            settingsDialog = new AlertDialog.Builder(this).setTitle("设置")
                    .setItems(items.toArray(new String[0]), (dialog, which) -> {
                        String selected = items.get(which);
                        if ("退出当前账号".equals(selected)) confirmClear(true);
                        else if ("清除登录与网站数据".equals(selected)) confirmClear();
                        else if ("登录帮助".equals(selected)) showLoginHelp(false);
                        else showAbout();
                    }).setNegativeButton("关闭", null).create();
            AlertDialog shown = settingsDialog;
            shown.setOnDismissListener(dialog -> { if (settingsDialog == shown) settingsDialog = null; });
            settingsDialog.show();
        });
    }

    private void showAbout() {
        new AlertDialog.Builder(this).setTitle("ChatGPT Nova 1.3.4")
                .setIcon(R.mipmap.ic_launcher)
                .setMessage("ChatGPT Nova 是用于访问 chatgpt.com 的个人客户端，与官方 ChatGPT App 独立存储登录状态。\n\n这是非官方客户端，不由 OpenAI 发布、维护或背书。应用不读取或保存账号密码。")
                .setPositiveButton("知道了", null).show();
    }

    private void openCurrentPageInBrowser() {
        String url = failedUrl != null ? failedUrl : (webView == null ? HOME : webView.getUrl());
        if (url == null || url.isEmpty()) url = HOME;
        Uri uri = Uri.parse(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) uri = Uri.parse(HOME);
        openInBrowser(uri);
    }

    private void openInBrowser(Uri uri) {
        openInBrowser(uri, false);
    }

    private void openInBrowser(Uri uri, boolean login) {
        try {
            String browserPackage = CustomTabsClient.getPackageName(this, null);
            if (login && !"com.android.chrome".equals(browserPackage) && !"com.brave.browser".equals(browserPackage)) {
                String supported = CustomTabsClient.getPackageName(this,
                        Arrays.asList("com.android.chrome", "com.brave.browser"), true);
                if (supported != null) browserPackage = supported;
            }
            if (browserPackage != null) {
                CustomTabsIntent customTab = new CustomTabsIntent.Builder().setShowTitle(true).build();
                customTab.intent.setPackage(browserPackage);
                customTab.launchUrl(this, uri);
                return;
            }
            Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org/"));
            probe.addCategory(Intent.CATEGORY_BROWSABLE);
            ArrayList<Intent> browsers = new ArrayList<>();
            for (android.content.pm.ResolveInfo info : getPackageManager().queryIntentActivities(probe, 0)) {
                String candidate = info.activityInfo.packageName;
                if (!candidate.equals(getPackageName())) {
                    browsers.add(new Intent(Intent.ACTION_VIEW, uri).setPackage(candidate)
                            .addCategory(Intent.CATEGORY_BROWSABLE));
                }
            }
            if (browsers.isEmpty()) throw new ActivityNotFoundException();
            Intent first = browsers.remove(0);
            Intent chooser = Intent.createChooser(first, "选择浏览器");
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, browsers.toArray(new Intent[0]));
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, "没有可用的系统浏览器。", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmClear() {
        confirmClear(false);
    }

    private void confirmClear(boolean signOut) {
        if (clearDialog != null || clearing) return;
        clearDialog = new AlertDialog.Builder(this)
                .setTitle(signOut ? "退出当前账号？" : "清除登录与网站数据？")
                .setMessage(signOut
                        ? "将退出 Nova 内的账号，并清除本应用的登录与网站数据。不会退出官方 ChatGPT App 或浏览器中的账号。"
                        : "将清除 Nova 的登录状态、Cookie、缓存、网站数据和临时照片。不会影响官方 ChatGPT App 或浏览器中的账号。")
                .setNegativeButton("取消", null)
                .setPositiveButton(signOut ? "退出" : "清除", (d, w) -> {
                    if (clearing) return;
                    clearing = true;
                    accountUiState = AccountUiState.UNKNOWN;
                    accountQuerySerial++;
                    cancelPageRequests();
                    cancelDownloads();
                    webView.stopLoading();
                    webView.clearCache(true);
                    webView.clearHistory();
                    webView.clearFormData();
                    ((ViewGroup) webView.getParent()).removeView(webView);
                    webView.destroy();
                    webView = null;
                    uploads.clearCachedPhotos();
                    WebStorage.getInstance().deleteAllData();
                    errorMessage.setText("正在清除 Nova 的登录数据…");
                    errorContainer().setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                    CookieManager.getInstance().removeAllCookies(value -> {
                        CookieManager.getInstance().flush();
                        if (!isFinishing() && !isDestroyed()) {
                            clearing = false;
                            failedUrl = null;
                            buildUi();
                            configureWebView();
                            webView.loadUrl(HOME);
                        }
                    });
                })
                .create();
        clearDialog.setOnDismissListener(dialog -> clearDialog = null);
        clearDialog.show();
    }

    @Override
    public void onBackPressed() {
        navigateBack();
    }

    private void navigateBack() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    @Override
    protected void onPause() {
        CookieManager.getInstance().flush();
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null && !clearing) {
            String last = webView.getUrl();
            if (safePage(last)) outState.putString("nova.lastUrl", last);
            Bundle state = new Bundle();
            Parcel parcel = Parcel.obtain();
            try {
                webView.saveState(state);
                parcel.writeBundle(state);
                if (parcel.dataSize() <= 256 * 1024) outState.putBundle("nova.webState", state);
            } catch (RuntimeException ignored) {
                // The bounded HTTPS URL above remains a usable fallback.
            } finally { parcel.recycle(); }
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UploadController.PICK_FILE) uploads.result(resultCode, data);
        if (requestCode == SAVE_BLOB) {
            Uri destination = resultCode == RESULT_OK && data != null ? data.getData() : null;
            if (destination == null || !"content".equalsIgnoreCase(destination.getScheme())) { cancelDownloads(); return; }
            if (blobDownload != null) {
                Toast.makeText(this, "正在保存文件，请等待下载完成提示。", Toast.LENGTH_LONG).show();
                BlobDownload saving = blobDownload;
                saving.saveTo(destination, () -> { if (blobDownload == saving) blobDownload = null; });
            } else if (httpDownload != null) {
                Toast.makeText(this, "正在保存文件，请等待下载完成提示。", Toast.LENGTH_LONG).show();
                HttpDownload saving = httpDownload;
                saving.saveTo(destination, () -> { if (httpDownload == saving) httpDownload = null; });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WEB_PERMISSIONS && pendingWebPermission != null) {
            finishWebPermission();
            return;
        }

        if (requestCode == UploadController.CAMERA_PERMISSION) uploads.cameraPermissionResult();
    }

    @Override
    protected void onDestroy() {
        if (overflowMenu != null) { overflowMenu.dismiss(); overflowMenu = null; }
        if (settingsDialog != null) { settingsDialog.dismiss(); settingsDialog = null; }
        if (clearDialog != null) { clearDialog.dismiss(); clearDialog = null; }
        cancelPageRequests();
        cancelDownloads();
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            if (webView.getParent() instanceof ViewGroup) ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
