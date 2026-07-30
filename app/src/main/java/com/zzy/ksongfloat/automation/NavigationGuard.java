package com.zzy.ksongfloat.automation;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Locale;

/** 检测误切到消息页并自动返回。 */
public final class NavigationGuard {
    private static final String[] MESSAGE_PAGE_MARKERS = {
            "私信列表", "会话", "聊天列表", "暂无消息", "全部消息"
    };

    private NavigationGuard() {
    }

    public static boolean recoverIfOnMessagePage(AccessibilityService service) {
        if (service == null) return false;
        AccessibilityNodeInfo root = null;
        try {
            root = service.getRootInActiveWindow();
            if (root == null) return false;
            if (!looksLikeMessageInbox(root)) return false;
            AutomationLog.warn("检测到消息/私信页，执行返回纠偏");
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            sleepQuiet(400);
            return true;
        } finally {
            NodeFinder.recycle(root);
        }
    }

    private static boolean looksLikeMessageInbox(AccessibilityNodeInfo root) {
        int[] size = ScreenBoundsGuard.screenSize(root);
        int sh = size[1];
        if (isSelectedBottomTab(root, sh, "消息")) return true;

        String pageText = collectVisibleText(root, 0, new StringBuilder(), 0).toString().toLowerCase(Locale.ROOT);
        if (pageText.contains("私信列表") || pageText.contains("聊天列表")) return true;
        for (String marker : MESSAGE_PAGE_MARKERS) {
            if (pageText.contains(marker.toLowerCase(Locale.ROOT))) return true;
        }
        int messageHits = 0;
        if (pageText.contains("消息")) messageHits++;
        if (pageText.contains("私信")) messageHits++;
        if (pageText.contains("会话")) messageHits++;
        return messageHits >= 2;
    }

    private static boolean isSelectedBottomTab(AccessibilityNodeInfo root, int screenH, String label) {
        return walkSelectedTab(root, label, screenH, 0);
    }

    private static boolean walkSelectedTab(AccessibilityNodeInfo n, String label, int screenH, int depth) {
        if (n == null || depth > 14) return false;
        String text = cs(n.getText());
        String desc = cs(n.getContentDescription());
        RectGuarded hit = matchBottomTab(n, text, desc, label, screenH);
        if (hit != null && hit.selected) return true;
        int c = Math.min(n.getChildCount(), 40);
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo child = null;
            try {
                child = n.getChild(i);
                if (walkSelectedTab(child, label, screenH, depth + 1)) return true;
            } finally {
                NodeFinder.recycle(child);
            }
        }
        return false;
    }

    private static RectGuarded matchBottomTab(AccessibilityNodeInfo n, String text, String desc,
                                              String label, int screenH) {
        if (!label.equals(text) && !label.equals(desc)) return null;
        android.graphics.Rect r = new android.graphics.Rect();
        n.getBoundsInScreen(r);
        int centerY = (r.top + r.bottom) / 2;
        if (centerY < screenH * 0.85f) return null;
        boolean selected = n.isSelected() || n.isChecked() || n.isFocused();
        AccessibilityNodeInfo p = n.getParent();
        try {
            if (!selected && p != null) {
                selected = p.isSelected() || p.isChecked();
            }
        } finally {
            NodeFinder.recycle(p);
        }
        return new RectGuarded(selected);
    }

    private static StringBuilder collectVisibleText(AccessibilityNodeInfo n, int depth,
                                                    StringBuilder sb, int nodes) {
        if (n == null || depth > 10 || nodes > 120 || sb.length() > 2000) return sb;
        if (n.isVisibleToUser()) {
            String t = cs(n.getText());
            String d = cs(n.getContentDescription());
            if (!t.isEmpty()) {
                sb.append(t).append(' ');
                nodes++;
            }
            if (!d.isEmpty()) {
                sb.append(d).append(' ');
                nodes++;
            }
        }
        int c = Math.min(n.getChildCount(), 30);
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo child = null;
            try {
                child = n.getChild(i);
                collectVisibleText(child, depth + 1, sb, nodes);
            } finally {
                NodeFinder.recycle(child);
            }
        }
        return sb;
    }

    private static String cs(CharSequence c) {
        return c == null ? "" : c.toString().trim();
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class RectGuarded {
        final boolean selected;

        RectGuarded(boolean selected) {
            this.selected = selected;
        }
    }
}
