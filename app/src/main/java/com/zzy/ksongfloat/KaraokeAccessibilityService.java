package com.zzy.ksongfloat;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class KaraokeAccessibilityService extends AccessibilityService {
    private static volatile String pageSummary = "未开启无障碍或暂无页面";

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        String pkg = event.getPackageName() == null ? "" : event.getPackageName().toString();
        String cls = event.getClassName() == null ? "" : event.getClassName().toString();
        StringBuilder sb = new StringBuilder();
        sb.append(shortPkg(pkg)).append(" / ").append(shortCls(cls));
        AccessibilityNodeInfo root = getRootInActiveWindow();
        String visible = collectText(root, 0, new StringBuilder(), 120).toString().trim();
        if (!visible.isEmpty()) sb.append("\n").append(visible);
        pageSummary = sb.toString();
    }

    @Override public void onInterrupt() { pageSummary = "无障碍服务被中断"; }
    @Override protected void onServiceConnected() { super.onServiceConnected(); pageSummary = "无障碍服务已连接，等待打开全民 K 歌"; }
    public static String getPageSummary() { return pageSummary == null || pageSummary.length() == 0 ? "暂无页面" : pageSummary; }

    private StringBuilder collectText(AccessibilityNodeInfo node, int depth, StringBuilder out, int maxLen) {
        if (node == null || out.length() >= maxLen || depth > 8) return out;
        append(out, node.getText(), maxLen);
        append(out, node.getContentDescription(), maxLen);
        int count = Math.min(node.getChildCount(), 20);
        for (int i = 0; i < count && out.length() < maxLen; i++) collectText(node.getChild(i), depth + 1, out, maxLen);
        return out;
    }

    private void append(StringBuilder out, CharSequence s, int maxLen) {
        if (s == null) return;
        String v = s.toString().trim();
        if (v.isEmpty() || out.indexOf(v) >= 0) return;
        if (out.length() > 0) out.append(" | ");
        int remain = maxLen - out.length();
        out.append(v.length() > remain ? v.substring(0, Math.max(0, remain)) : v);
    }

    private String shortPkg(String p) { if (p == null || p.isEmpty()) return "unknown"; int i = p.lastIndexOf('.'); return i >= 0 ? p.substring(i + 1) : p; }
    private String shortCls(String c) { if (c == null || c.isEmpty()) return "Window"; int i = c.lastIndexOf('.'); return i >= 0 ? c.substring(i + 1) : c; }
}