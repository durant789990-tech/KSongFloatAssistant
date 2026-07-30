package com.zzy.ksongfloat.automation;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * 过滤屏幕顶部/底部导航敏感区域，避免误触 Tab 栏。
 */
public final class ScreenBoundsGuard {
    private static final float TOP_EXCLUDE_RATIO = 0.08f;
    private static final float BOTTOM_EXCLUDE_RATIO = 0.10f;

    private ScreenBoundsGuard() {
    }

    public static int[] screenSize(AccessibilityNodeInfo root) {
        if (root == null) return new int[]{0, 0};
        Rect r = new Rect();
        root.getBoundsInScreen(r);
        return new int[]{Math.max(r.width(), 1), Math.max(r.height(), 1)};
    }

    public static boolean isSafeInteractionBounds(Rect bounds, int screenW, int screenH) {
        if (bounds == null || bounds.isEmpty() || screenW <= 0 || screenH <= 0) {
            return false;
        }
        int centerY = (bounds.top + bounds.bottom) / 2;
        int topLimit = (int) (screenH * TOP_EXCLUDE_RATIO);
        int bottomLimit = (int) (screenH * (1f - BOTTOM_EXCLUDE_RATIO));
        if (centerY <= topLimit) return false;
        if (centerY >= bottomLimit) return false;
        if (bounds.bottom >= screenH * (1f - BOTTOM_EXCLUDE_RATIO)) return false;
        if (bounds.top <= screenH * TOP_EXCLUDE_RATIO) return false;
        return true;
    }

    public static boolean isSafeNode(AccessibilityNodeInfo node, int screenW, int screenH) {
        if (node == null || !node.isVisibleToUser()) return false;
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        return isSafeInteractionBounds(r, screenW, screenH);
    }

    public static boolean isBottomNavLabel(String text) {
        if (text == null || text.isEmpty()) return false;
        String t = text.trim();
        return t.equals("动态") || t.equals("歌房") || t.equals("消息")
                || t.equals("我的") || t.equals("首页") || t.equals("发现")
                || t.equals("同城") || t.equals("推荐");
    }
}
