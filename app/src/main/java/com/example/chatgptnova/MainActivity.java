package com.example.chatgptnova;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
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
import android.window.OnBackInvokedDispatcher;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.File;
import java.util.ArrayList;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER = 1001;
    private static final int WEB_PERMISSIONS = 1002;
    private static final int DOWNLOAD_PERMISSION = 1003;
    private static final int CAMERA_PERMISSION = 1004;
    private static final int SAVE_BLOB = 1005;
    private static final String HOME = "https://chatgpt.com/";

    private WebView webView;
    private ProgressBar progress;
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingWebPermission;
    private WebChromeClient.FileChooserParams pendingChooserParams;
    private Uri cameraUri;
    private File cameraFile;
    private BlobDownload blobDownload;
    private boolean oauthDialogVisible;

    private String pendingDownloadUrl;
    private String pendingDownloadUserAgent;
    private String pendingDownloadContentDisposition;
    private String pendingDownloadMimeType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File photoDirectory = new File(getCacheDir(), "camera");
        File[] oldPhotos = photoDirectory.listFiles();
        if (oldPhotos != null) for (File photo : oldPhotos) photo.delete();
        buildUi();
        configureWebView();
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(HOME);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::navigateBack);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
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
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));

        TextView title = new TextView(this);
        title.setText("ChatGPT Nova · Unofficial");
        title.setTextSize(16);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTextColor(0xFF111111);
        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.START);
        bar.addView(title, titleLp);

        TextView menu = new TextView(this);
        menu.setText("⋮");
        menu.setTextSize(28);
        menu.setGravity(Gravity.CENTER);
        menu.setContentDescription("Menu");
        menu.setOnClickListener(v -> showMenu());
        FrameLayout.LayoutParams menuLp = new FrameLayout.LayoutParams(
                dp(48), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END);
        bar.addView(menu, menuLp);
        root.addView(bar, barLp);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        root.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));

        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
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
        s.setJavaScriptCanOpenWindowsAutomatically(true);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                return handleUri(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUri(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this,
                            "页面加载失败，请检查网络后刷新。",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() >= 400) {
                    Toast.makeText(MainActivity.this, "网页暂时不可用，可刷新或用浏览器打开。",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (view != webView) { view.destroy(); return true; }
                if (blobDownload != null) { blobDownload.cancel(); blobDownload = null; }
                if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
                pendingWebPermission = null;
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
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                cancelFileChooser();
                fileCallback = callback;
                pendingChooserParams = params;
                boolean acceptsImages = params.getAcceptTypes().length == 0;
                for (String type : params.getAcceptTypes()) {
                    if (type == null || type.isEmpty() || "*/*".equals(type)
                            || type.startsWith("image/")) acceptsImages = true;
                }
                if (acceptsImages) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("上传文件或图片")
                            .setItems(new String[]{"选择文件 / 图片", "拍照"}, (d, which) -> {
                                if (which == 0) launchFilePicker(); else requestPhoto();
                            })
                            .setOnCancelListener(d -> cancelFileChooser()).show();
                } else {
                    launchFilePicker();
                }
                return true;
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
            if ("accounts.google.com".equalsIgnoreCase(host)
                    || "login.microsoftonline.com".equalsIgnoreCase(host)
                    || "login.live.com".equalsIgnoreCase(host)
                    || "appleid.apple.com".equalsIgnoreCase(host)) {
                showOAuthHelp();
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
                        || "javascript".equalsIgnoreCase(target.getScheme())) return true;
                try { startActivity(intent); }
                catch (ActivityNotFoundException e) {
                    if (fallback != null && "https".equalsIgnoreCase(Uri.parse(fallback).getScheme())) {
                        webView.loadUrl(fallback);
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

    private void showOAuthHelp() {
        if (oauthDialogVisible) return;
        oauthDialogVisible = true;
        new AlertDialog.Builder(this).setTitle("第三方登录兼容性")
                .setMessage("Google / Microsoft / Apple 可能禁止网页容器登录。可在系统浏览器中从 ChatGPT 首页重新登录。浏览器与 Nova 的 Cookie 独立，浏览器登录不会自动迁移回 Nova。要在 Nova 内保留第二账号，请使用官网支持的邮箱登录方式。")
                .setNegativeButton("留在 Nova", null)
                .setPositiveButton("在浏览器中登录", (d, w) -> openInBrowser(Uri.parse(HOME)))
                .setOnDismissListener(d -> oauthDialogVisible = false).show();
    }

    static boolean isTrustedOrigin(Uri uri) {
        String host = uri == null ? null : uri.getHost();
        return uri != null && "https".equalsIgnoreCase(uri.getScheme()) && host != null
                && ("chatgpt.com".equalsIgnoreCase(host)
                    || host.toLowerCase(java.util.Locale.ROOT).endsWith(".chatgpt.com"));
    }

    private void launchFilePicker() {
        if (fileCallback == null || pendingChooserParams == null) return;
        try {
            Intent intent = pendingChooserParams.createIntent();
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, FILE_CHOOSER);
        } catch (Exception e) {
            cancelFileChooser();
            Toast.makeText(this, "没有可用的文件选择器。", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestPhoto() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchPhoto();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        }
    }

    private void launchPhoto() {
        if (fileCallback == null) return;
        try {
            File directory = new File(getCacheDir(), "camera");
            if (!directory.exists() && !directory.mkdirs()) throw new java.io.IOException();
            cameraFile = File.createTempFile("nova-", ".jpg", directory);
            cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", cameraFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            intent.setClipData(ClipData.newRawUri("Nova photo", cameraUri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, FILE_CHOOSER);
        } catch (Exception e) {
            cancelFileChooser();
            Toast.makeText(this, "无法调用相机，可选择已有图片。", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelFileChooser() {
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        pendingChooserParams = null;
        if (cameraUri != null) revokeUriPermission(cameraUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cameraUri = null;
        if (cameraFile != null) { cameraFile.delete(); cameraFile = null; }
    }

    private void requestWebPermissions(PermissionRequest request) {
        if (!isTrustedOrigin(request.getOrigin())) { request.deny(); return; }
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
        if (isTrustedOrigin(request.getOrigin())) {
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

    private void requestDownload(String url,
                                 String userAgent,
                                 String contentDisposition,
                                 String mimeType) {
        if (url.startsWith("blob:") && isTrustedOrigin(Uri.parse(webView.getUrl() == null ? "" : webView.getUrl()))) {
            if (blobDownload != null) { Toast.makeText(this, "请先完成当前下载。", Toast.LENGTH_SHORT).show(); return; }
            blobDownload = new BlobDownload(this, webView, url);
            Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            save.addCategory(Intent.CATEGORY_OPENABLE);
            save.setType(mimeType == null || mimeType.isEmpty() ? "application/octet-stream" : mimeType);
            save.putExtra(Intent.EXTRA_TITLE, URLUtil.guessFileName(url, contentDisposition, mimeType));
            try { startActivityForResult(save, SAVE_BLOB); }
            catch (Exception e) { blobDownload.cancel(); blobDownload = null;
                Toast.makeText(this, "无法打开保存位置选择器。", Toast.LENGTH_SHORT).show(); }
            return;
        }
        if (!"https".equalsIgnoreCase(Uri.parse(url).getScheme())
                && !"http".equalsIgnoreCase(Uri.parse(url).getScheme())) {
            Toast.makeText(this, "无法下载此类型链接，可用系统浏览器打开。", Toast.LENGTH_LONG).show();
            return;
        }
        pendingDownloadUrl = url;
        pendingDownloadUserAgent = userAgent;
        pendingDownloadContentDisposition = contentDisposition;
        pendingDownloadMimeType = mimeType;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    DOWNLOAD_PERMISSION);
            return;
        }
        enqueuePendingDownload();
    }

    private void enqueuePendingDownload() {
        if (pendingDownloadUrl == null) return;

        try {
            String filename = URLUtil.guessFileName(
                    pendingDownloadUrl,
                    pendingDownloadContentDisposition,
                    pendingDownloadMimeType);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(pendingDownloadUrl));
            request.setTitle(filename);
            request.setDescription("ChatGPT Nova 下载");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);

            if (pendingDownloadMimeType != null && !pendingDownloadMimeType.isEmpty()) {
                request.setMimeType(pendingDownloadMimeType);
            }
            if (pendingDownloadUserAgent != null && !pendingDownloadUserAgent.isEmpty()) {
                request.addRequestHeader("User-Agent", pendingDownloadUserAgent);
            }

            String cookies = CookieManager.getInstance().getCookie(pendingDownloadUrl);
            if (cookies != null && !cookies.isEmpty()) {
                request.addRequestHeader("Cookie", cookies);
            }

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(this,
                    "已开始下载：" + filename,
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this,
                    "下载失败，可尝试在系统浏览器中打开当前页面。",
                    Toast.LENGTH_LONG).show();
        } finally {
            clearPendingDownload();
        }
    }

    private void clearPendingDownload() {
        pendingDownloadUrl = null;
        pendingDownloadUserAgent = null;
        pendingDownloadContentDisposition = null;
        pendingDownloadMimeType = null;
    }

    private void showMenu() {
        String[] items = {
                "刷新",
                "回到 ChatGPT",
                "清除第二账号登录数据",
                "用系统浏览器打开当前页",
                "关于 / 登录帮助"
        };

        new AlertDialog.Builder(this)
                .setTitle("ChatGPT Nova")
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        webView.reload();
                    } else if (which == 1) {
                        webView.loadUrl(HOME);
                    } else if (which == 2) {
                        confirmClear();
                    } else if (which == 3) {
                        openCurrentPageInBrowser();
                    } else if (which == 4) {
                        new AlertDialog.Builder(this).setTitle("ChatGPT Nova 1.2.0")
                                .setMessage("非官方客户端，不由 OpenAI 发布、维护或背书。\n网页内容来自 chatgpt.com。\n应用使用独立的网站数据，不读取或保存账号密码。\n\n第三方登录可能受 WebView 限制；浏览器登录不会自动迁移到 Nova。可尝试官网邮箱登录。")
                                .setPositiveButton("知道了", null)
                                .setNeutralButton("浏览器登录帮助", (dialog, w) -> showOAuthHelp()).show();
                    }
                })
                .show();
    }

    private void openCurrentPageInBrowser() {
        String url = webView.getUrl();
        if (url == null || url.isEmpty()) url = HOME;
        Uri uri = Uri.parse(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) uri = Uri.parse(HOME);
        openInBrowser(uri);
    }

    private void openInBrowser(Uri uri) {
        try {
            String browserPackage = CustomTabsClient.getPackageName(this, null);
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
        new AlertDialog.Builder(this)
                .setTitle("清除登录？")
                .setMessage("会清除此 APK 内的 Cookie、缓存和网站数据，不影响官方 ChatGPT App 的账号。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清除", (d, w) -> {
                    if (blobDownload != null) { blobDownload.cancel(); blobDownload = null; }
                    cancelFileChooser();
                    if (pendingWebPermission != null) { pendingWebPermission.deny(); pendingWebPermission = null; }
                    webView.stopLoading();
                    webView.clearCache(true);
                    webView.clearHistory();
                    webView.clearFormData();
                    ((ViewGroup) webView.getParent()).removeView(webView);
                    webView.destroy();
                    webView = null;
                    WebStorage.getInstance().deleteAllData();
                    buildUi();
                    configureWebView();
                    CookieManager.getInstance().removeAllCookies(value -> {
                        CookieManager.getInstance().flush();
                        if (webView != null && !isFinishing() && !isDestroyed()) webView.loadUrl(HOME);
                    });
                })
                .show();
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
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER && fileCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            if (resultCode == RESULT_OK && cameraUri != null && cameraFile != null && cameraFile.length() > 0) {
                results = new Uri[]{cameraUri};
            }
            if (results != null) {
                ArrayList<Uri> safe = new ArrayList<>();
                for (Uri uri : results) {
                    if (uri != null && "content".equalsIgnoreCase(uri.getScheme())
                            && (! (getPackageName() + ".fileprovider").equals(uri.getAuthority())
                                || uri.equals(cameraUri))) safe.add(uri);
                }
                results = safe.isEmpty() ? null : safe.toArray(new Uri[0]);
            }
            fileCallback.onReceiveValue(results);
            fileCallback = null;
            pendingChooserParams = null;
            if (cameraUri != null) revokeUriPermission(cameraUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        if (requestCode == SAVE_BLOB && blobDownload != null) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                BlobDownload saving = blobDownload;
                saving.saveTo(data.getData(), () -> { if (blobDownload == saving) blobDownload = null; });
            } else {
                blobDownload.cancel();
                blobDownload = null;
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

        if (requestCode == CAMERA_PERMISSION) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchPhoto();
            else {
                Toast.makeText(this, "相机权限未授予，可选择已有图片。", Toast.LENGTH_SHORT).show();
                launchFilePicker();
            }
        }

        if (requestCode == DOWNLOAD_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enqueuePendingDownload();
            } else {
                Toast.makeText(this,
                        "没有存储权限，无法保存下载文件。",
                        Toast.LENGTH_SHORT).show();
                clearPendingDownload();
            }
        }
    }

    @Override
    protected void onDestroy() {
        cancelFileChooser();
        if (blobDownload != null) { blobDownload.cancel(); blobDownload = null; }
        if (pendingWebPermission != null) {
            pendingWebPermission.deny();
            pendingWebPermission = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
