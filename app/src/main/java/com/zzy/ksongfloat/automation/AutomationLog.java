package com.zzy.ksongfloat.automation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class AutomationLog {
    private static final int MAX = 200;
    private static final Pattern KEY_PATTERN = Pattern.compile("(?i)(sk-[a-z0-9\\-_]{8,}|api[_-]?key\\s*[:=]\\s*\\S+)");
    private static final List<String> lines = Collections.synchronizedList(new ArrayList<>());
    private static final SimpleDateFormat FMT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private static volatile Listener listener;

    public interface Listener {
        void onLog(String line);
    }

    public static void setListener(Listener l) {
        listener = l;
    }

    public static void event(String state, String pkg, String page, String action, String result) {
        append("INFO", state + " | pkg=" + safe(pkg) + " | page=" + safe(page) + " | action=" + safe(action) + " | result=" + safe(result));
    }

    public static void nodeScan(String page, int nodeCount, String targetInfo, int retry) {
        append("INFO", "scan page=" + safe(page) + " nodes=" + nodeCount + " target=" + safe(targetInfo) + " retry=" + retry);
    }

    public static void pauseReason(String reason) {
        append("WARN", "pause=" + safe(reason));
    }

    public static void info(String msg) {
        append("INFO", msg);
    }

    public static void warn(String msg) {
        append("WARN", msg);
    }

    public static void error(String msg) {
        append("ERR ", msg);
    }

    private static void append(String level, String msg) {
        String line = FMT.format(new Date()) + " [" + level + "] " + redact(msg == null ? "" : msg);
        synchronized (lines) {
            lines.add(line);
            while (lines.size() > MAX) lines.remove(0);
        }
        Listener l = listener;
        if (l != null) l.onLog(line);
    }

    static String redact(String s) {
        if (s == null) return "";
        return KEY_PATTERN.matcher(s).replaceAll("[REDACTED]");
    }

    private static String safe(String s) {
        return s == null ? "" : redact(s);
    }

    public static List<String> snapshot() {
        synchronized (lines) {
            return new ArrayList<>(lines);
        }
    }

    public static List<String> recent(int n) {
        List<String> all = snapshot();
        if (all.size() <= n) return all;
        return new ArrayList<>(all.subList(all.size() - n, all.size()));
    }

    public static void clear() {
        lines.clear();
    }
}
