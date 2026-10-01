package com.example.chatgptnova;

import android.webkit.URLUtil;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DownloadNames {
    private static final Pattern UTF8 = Pattern.compile("filename\\*\\s*=\\s*UTF-8'[^']*'([^;]+)", Pattern.CASE_INSENSITIVE);
    static String guess(String url, String disposition, String mime) {
        String name = URLUtil.guessFileName(url, disposition, mime);
        if (disposition != null) {
            Matcher match = UTF8.matcher(disposition);
            if (match.find()) try {
                name = URLDecoder.decode(match.group(1).trim().replace("+", "%2B"), StandardCharsets.UTF_8.name());
            } catch (Exception ignored) { }
        }
        name = name.replaceAll("[\\\\/\\p{Cntrl}]", "_").trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = "Nova-download";
        if (name.length() > 160) name = name.substring(0, 160);
        return name;
    }
}
