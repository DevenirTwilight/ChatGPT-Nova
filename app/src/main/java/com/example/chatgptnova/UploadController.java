package com.example.chatgptnova;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Locale;

/** Owns exactly one chooser callback. Completed photos outlive subsequent choosers. */
final class UploadController {
    static final int PICK_FILE = 1001;
    static final int CAMERA_PERMISSION = 1004;
    private final Activity activity;
    private ValueCallback<Uri[]> callback;
    private WebChromeClient.FileChooserParams params;
    private AlertDialog choice;
    private Uri photoUri;
    private File photo;

    UploadController(Activity activity) {
        this.activity = activity;
        File directory = new File(activity.getCacheDir(), "camera");
        File[] old = directory.listFiles();
        long expiry = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        if (old != null) for (File file : old) {
            if (file.isFile() && file.lastModified() < expiry) file.delete();
        }
    }

    boolean show(ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
        cancel();
        this.callback = callback;
        this.params = params;
        if (params.getMode() != WebChromeClient.FileChooserParams.MODE_OPEN
                && params.getMode() != WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
            cancel();
            return true;
        }
        String[] types = mimeTypes(params.getAcceptTypes());
        boolean images = false;
        for (String type : types) if ("*/*".equals(type) || "image/*".equals(type) || "image/jpeg".equals(type)) images = true;
        if (!images) { pick(); return true; }
        if (params.isCaptureEnabled() && types.length == 1 && types[0].startsWith("image/")) {
            requestPhoto();
            return true;
        }
        choice = new AlertDialog.Builder(activity).setTitle("上传文件或图片")
                .setItems(new String[]{"选择文件 / 图片", "拍照"}, (dialog, which) -> {
                    choice = null;
                    if (which == 0) pick(); else requestPhoto();
                }).setOnCancelListener(dialog -> cancel()).create();
        choice.show();
        return true;
    }

    static String[] mimeTypes(String[] accept) {
        LinkedHashSet<String> types = new LinkedHashSet<>();
        if (accept != null) for (String item : accept) {
            if (item == null) continue;
            for (String entry : item.split(",")) {
                String type = entry.trim().toLowerCase(Locale.ROOT);
                if (type.startsWith(".")) {
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(type.substring(1));
                }
                if (type != null && type.matches("[a-z0-9!#$&^_.+*-]+/[a-z0-9!#$&^_.+*-]+")) types.add(type);
            }
        }
        if (types.isEmpty() || types.contains("*/*")) return new String[]{"*/*"};
        return types.toArray(new String[0]);
    }

    private void pick() {
        if (callback == null) return;
        String[] types = mimeTypes(params.getAcceptTypes());
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType(types.length == 1 ? types[0] : "*/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                        params.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (types.length > 1) intent.putExtra(Intent.EXTRA_MIME_TYPES, types);
        try { activity.startActivityForResult(intent, PICK_FILE); }
        catch (RuntimeException error) { cancel(); message("没有可用的文件选择器。"); }
    }

    private void requestPhoto() {
        if (callback == null) return;
        if (activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) photo();
        else activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
    }

    void cameraPermissionResult() {
        if (callback == null) return;
        if (activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) photo();
        else { message("相机权限未授予，可选择已有图片。"); pick(); }
    }

    private void photo() {
        if (callback == null) return;
        try {
            File directory = new File(activity.getCacheDir(), "camera");
            if (!directory.isDirectory() && !directory.mkdirs()) throw new java.io.IOException();
            photo = File.createTempFile("nova-", ".jpg", directory);
            photoUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", photo);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    .setClipData(ClipData.newRawUri("Nova photo", photoUri))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            activity.startActivityForResult(intent, PICK_FILE);
        } catch (Exception error) { cancel(); message("无法调用相机，可选择已有图片。"); }
    }

    void result(int resultCode, Intent data) {
        if (callback == null) return;
        LinkedHashSet<Uri> selected = new LinkedHashSet<>();
        boolean captured = resultCode == Activity.RESULT_OK && photoUri != null && photo != null && photo.length() > 0;
        if (captured) selected.add(photoUri);
        else if (resultCode == Activity.RESULT_OK && data != null) {
            if (data.getData() != null) selected.add(data.getData());
            ClipData clip = data.getClipData();
            if (clip != null) for (int i = 0; i < Math.min(clip.getItemCount(), 100); i++) selected.add(clip.getItemAt(i).getUri());
        }
        String ownProvider = activity.getPackageName() + ".fileprovider";
        selected.removeIf(uri -> uri == null || !"content".equalsIgnoreCase(uri.getScheme())
                || (ownProvider.equals(uri.getAuthority()) && !uri.equals(photoUri)));
        if (params.getMode() != WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE && selected.size() > 1) {
            Uri first = selected.iterator().next();
            selected.clear(); selected.add(first);
        }
        ValueCallback<Uri[]> result = callback;
        callback = null;
        params = null;
        revokePhotoGrant();
        // WebView can still be streaming the first photo when another chooser opens.
        // Retain successful captures; delete only canceled/empty captures here.
        if (!captured && photo != null) photo.delete();
        photo = null; photoUri = null;
        result.onReceiveValue(selected.isEmpty() ? null : selected.toArray(new Uri[0]));
    }

    void cancel() {
        ValueCallback<Uri[]> pending = callback;
        callback = null; params = null;
        if (choice != null) { choice.dismiss(); choice = null; }
        revokePhotoGrant();
        if (photo != null) photo.delete();
        photo = null; photoUri = null;
        if (pending != null) pending.onReceiveValue(null);
    }

    void clearCachedPhotos() {
        File[] files = new File(activity.getCacheDir(), "camera").listFiles();
        if (files != null) for (File file : files) if (file.isFile()) file.delete();
    }

    private void revokePhotoGrant() {
        if (photoUri != null) activity.revokeUriPermission(photoUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    private void message(String text) { android.widget.Toast.makeText(activity, text, android.widget.Toast.LENGTH_LONG).show(); }
}
