package com.zzy.ksongfloat.automation;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

/** 构造 Shizuku / adb input text 命令参数。 */
public final class ShellTextEscaper {
    private ShellTextEscaper() {
    }

    public static String buildInputTextCommand(String text) {
        if (text == null || text.isEmpty()) {
            return "input text ''";
        }
        if (isAsciiInputSafe(text)) {
            return "input text " + quote(escapeForInputText(text));
        }
        String b64 = Base64.encodeToString(text.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        return "input text \"$(echo " + b64 + " | base64 -d)\"";
    }

    private static boolean isAsciiInputSafe(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c > 127) return false;
        }
        return true;
    }

    private static String escapeForInputText(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') sb.append("%s");
            else if (c == '%') sb.append("\\%");
            else if (c == '\'') sb.append("\\'");
            else if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String quote(String s) {
        return "'" + s + "'";
    }
}
