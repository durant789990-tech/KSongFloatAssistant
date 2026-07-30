package com.zzy.ksongfloat.automation;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import com.zzy.ksongfloat.runtime.ForegroundAppResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 识别真实用户卡片，排除底部导航和系统控件。
 */
public final class UserCardDetector {
    public static class Candidate {
        public final String nodeKey;
        public final String label;
        public final Rect bounds;
        public final AccessibilityNodeInfo node;

        Candidate(String nodeKey, String label, Rect bounds, AccessibilityNodeInfo node) {
            this.nodeKey = nodeKey;
            this.label = label;
            this.bounds = bounds;
            this.node = node;
        }
    }

    private static final Set<String> EXCLUDED = new HashSet<>(Arrays.asList(
            "动态", "歌房", "录歌", "消息", "我的", "推荐", "同城", "关注", "直播", "搜索",
            "设置", "帮助与反馈", "切换账号", "退出当前账号", "退出登录", "首页", "广场", "附近"
    ));

    private static final float BOTTOM_EXCLUDE_RATIO = 0.16f;
    private static final float TOP_EXCLUDE_RATIO = 0.08f;

    public static List<Candidate> findCandidates(AccessibilityNodeInfo root, int screenHeight, int screenWidth) {
        List<Candidate> out = new ArrayList<>();
        if (root == null || screenHeight <= 0) return out;
        int bottomLimit = (int) (screenHeight * (1f - BOTTOM_EXCLUDE_RATIO));
        int topLimit = (int) (screenHeight * TOP_EXCLUDE_RATIO);
        walk(root, 0, out, bottomLimit, topLimit, screenWidth);
        return out;
    }

    private static void walk(AccessibilityNodeInfo n, int depth, List<Candidate> out,
                             int bottomLimit, int topLimit, int screenWidth) {
        if (n == null || depth > 14 || out.size() >= 12) return;
        String pkg = n.getPackageName() == null ? "" : n.getPackageName().toString();
        if (!pkg.contains(ForegroundAppResolver.TARGET_PKG)) return;

        Rect r = new Rect();
        n.getBoundsInScreen(r);
        if (r.height() <= 0 || r.width() <= 0) {
            recurse(n, depth, out, bottomLimit, topLimit, screenWidth);
            return;
        }
        if (r.bottom >= bottomLimit || r.top <= topLimit) {
            recurse(n, depth, out, bottomLimit, topLimit, screenWidth);
            return;
        }
        if (r.width() > screenWidth * 0.92f && r.height() < screenWidth * 0.12f) {
            recurse(n, depth, out, bottomLimit, topLimit, screenWidth);
            return;
        }

        String text = cs(n.getText());
        String desc = cs(n.getContentDescription());
        String label = text.length() >= 2 ? text : desc;
        if (label.isEmpty() || isExcluded(label)) {
            recurse(n, depth, out, bottomLimit, topLimit, screenWidth);
            return;
        }
        if (isNavLabel(label)) {
            recurse(n, depth, out, bottomLimit, topLimit, screenWidth);
            return;
        }

        boolean hasDistance = label.toLowerCase(Locale.ROOT).contains("km")
                || label.contains("距离") || label.contains("米");
        boolean hasAvatarChild = hasImageChild(n, 2);
        boolean listLike = r.height() >= 80 && r.height() <= screenWidth;
        boolean clickable = n.isClickable() || hasClickableParent(n, 3);

        if (clickable && listLike && (hasDistance || hasAvatarChild || looksLikeUsername(label))) {
            AccessibilityNodeInfo target = clickableTarget(n);
            if (target != null) {
                String key = cs(target.getViewIdResourceName()) + "|" + label + "|" + r.top;
                out.add(new Candidate(key, label, new Rect(r), target));
            }
        }
        recurse(n, depth, out, bottomLimit, topLimit, screenWidth);
    }

    public static boolean isExcludedNavLabel(String label) {
        return label == null || isExcluded(label.trim()) || isNavLabel(label.trim());
    }

    private static void recurse(AccessibilityNodeInfo n, int depth, List<Candidate> out,
                                int bottomLimit, int topLimit, int screenWidth) {
        int c = Math.min(n.getChildCount(), 40);
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo child = null;
            try {
                child = n.getChild(i);
                walk(child, depth + 1, out, bottomLimit, topLimit, screenWidth);
            } finally {
                NodeFinder.recycle(child);
            }
        }
    }

    private static boolean hasImageChild(AccessibilityNodeInfo n, int maxDepth) {
        return hasImageChild(n, 0, maxDepth);
    }

    private static boolean hasImageChild(AccessibilityNodeInfo n, int depth, int maxDepth) {
        if (n == null || depth > maxDepth) return false;
        String cls = n.getClassName() == null ? "" : n.getClassName().toString().toLowerCase(Locale.ROOT);
        if (cls.contains("image") || cls.contains("avatar")) return true;
        int c = Math.min(n.getChildCount(), 8);
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo ch = n.getChild(i);
            try {
                if (hasImageChild(ch, depth + 1, maxDepth)) return true;
            } finally {
                NodeFinder.recycle(ch);
            }
        }
        return false;
    }

    private static boolean looksLikeUsername(String label) {
        if (label.length() < 2 || label.length() > 20) return false;
        return !label.matches(".*\\d{4,}.*");
    }

    private static boolean isNavLabel(String label) {
        return EXCLUDED.contains(label) || label.length() <= 2;
    }

    private static boolean isExcluded(String label) {
        for (String ex : EXCLUDED) {
            if (label.equals(ex) || label.contains(ex)) return true;
        }
        return false;
    }

    private static boolean hasClickableParent(AccessibilityNodeInfo n, int maxUp) {
        AccessibilityNodeInfo cur = n;
        for (int i = 0; i < maxUp && cur != null; i++) {
            if (cur.isClickable()) return true;
            AccessibilityNodeInfo p = cur.getParent();
            if (cur != n) NodeFinder.recycle(cur);
            cur = p;
        }
        if (cur != null && cur != n) NodeFinder.recycle(cur);
        return false;
    }

    private static AccessibilityNodeInfo clickableTarget(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur = n;
        for (int i = 0; i < 4 && cur != null; i++) {
            if (cur.isClickable() && cur.isVisibleToUser()) return AccessibilityNodeInfo.obtain(cur);
            AccessibilityNodeInfo p = cur.getParent();
            if (cur != n) NodeFinder.recycle(cur);
            cur = p;
        }
        if (n.isVisibleToUser()) return AccessibilityNodeInfo.obtain(n);
        return null;
    }

    private static String cs(CharSequence c) {
        return c == null ? "" : c.toString().trim();
    }
}
