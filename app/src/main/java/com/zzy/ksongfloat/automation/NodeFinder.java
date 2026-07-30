package com.zzy.ksongfloat.automation;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NodeFinder {
    private static final int MAX_DEPTH = 12;
    private static final int MAX_NODES = 260;

    public static class Match {
        public final AccessibilityNodeInfo node;
        public final String matchedBy;
        public final String matchedText;

        Match(AccessibilityNodeInfo node, String matchedBy, String matchedText) {
            this.node = node;
            this.matchedBy = matchedBy;
            this.matchedText = matchedText;
        }
    }

    public static Match findClickableByTexts(AccessibilityNodeInfo root, String... keywords) {
        if (root == null || keywords == null) return null;
        int[] size = ScreenBoundsGuard.screenSize(root);
        List<Match> all = new ArrayList<>();
        collect(root, keywords, 0, all, size[0], size[1]);
        Match best = null;
        for (Match m : all) {
            if (best == null || score(m, size[0], size[1]) > score(best, size[0], size[1])) best = m;
        }
        return best;
    }

    public static Match findEditable(AccessibilityNodeInfo root) {
        if (root == null) return null;
        int[] size = ScreenBoundsGuard.screenSize(root);
        List<Match> all = new ArrayList<>();
        walkEditable(root, 0, all, size[0], size[1]);
        return all.isEmpty() ? null : all.get(0);
    }

    public static Match findInputField(AccessibilityNodeInfo root, String... hints) {
        int[] size = ScreenBoundsGuard.screenSize(root);
        Match editable = findEditable(root);
        if (editable != null) return editable;
        if (root == null || hints == null) return null;
        List<Match> all = new ArrayList<>();
        collectInputHints(root, hints, 0, all, size[0], size[1]);
        Match best = null;
        for (Match m : all) {
            if (best == null || score(m, size[0], size[1]) > score(best, size[0], size[1])) best = m;
        }
        return best;
    }

    public static List<Match> findUserCards(AccessibilityNodeInfo root) {
        List<Match> out = new ArrayList<>();
        if (root == null) return out;
        int[] size = ScreenBoundsGuard.screenSize(root);
        for (UserCardDetector.Candidate c : UserCardDetector.findCandidates(root, size[1], size[0])) {
            if (ScreenBoundsGuard.isSafeNode(c.node, size[0], size[1])) {
                out.add(new Match(c.node, "user_card", c.label));
            }
        }
        return out;
    }

    private static void collect(AccessibilityNodeInfo n, String[] keywords, int depth, List<Match> out,
                                  int screenW, int screenH) {
        if (n == null || depth > MAX_DEPTH || out.size() > MAX_NODES) return;
        String text = cs(n.getText());
        String desc = cs(n.getContentDescription());
        String hay = (text + " " + desc).toLowerCase(Locale.ROOT);
        for (String kw : keywords) {
            if (kw == null || kw.isEmpty()) continue;
            if (ScreenBoundsGuard.isBottomNavLabel(kw) || ScreenBoundsGuard.isBottomNavLabel(text)
                    || ScreenBoundsGuard.isBottomNavLabel(desc)) {
                continue;
            }
            if (hay.contains(kw.toLowerCase(Locale.ROOT))) {
                AccessibilityNodeInfo target = clickableTarget(n, screenW, screenH);
                if (target != null) {
                    out.add(new Match(target, "text", kw));
                }
            }
        }
        int c = Math.min(n.getChildCount(), 40);
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo child = null;
            try {
                child = n.getChild(i);
                collect(child, keywords, depth + 1, out, screenW, screenH);
            } finally {
                recycle(child);
            }
        }
    }

    private static void walkEditable(AccessibilityNodeInfo n, int depth, List<Match> out,
                                       int screenW, int screenH) {
        if (n == null || depth > MAX_DEPTH || out.size() > 8) return;
        String cls = n.getClassName() == null ? "" : n.getClassName().toString();
        boolean inputLike = n.isEditable()
                || cls.contains("EditText")
                || cls.contains("TextInput")
                || (n.isFocusable() && (cs(n.getText()).contains("说") || cs(n.getContentDescription()).contains("输入")));
        if (inputLike && ScreenBoundsGuard.isSafeNode(n, screenW, screenH)) {
            out.add(new Match(AccessibilityNodeInfo.obtain(n), "editable", cs(n.getText())));
            return;
        }
        int c = Math.min(n.getChildCount(), 40);
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo child = null;
            try {
                child = n.getChild(i);
                walkEditable(child, depth + 1, out, screenW, screenH);
            } finally {
                recycle(child);
            }
        }
    }

    private static AccessibilityNodeInfo clickableTarget(AccessibilityNodeInfo n, int screenW, int screenH) {
        AccessibilityNodeInfo cur = n;
        for (int i = 0; i < 4 && cur != null; i++) {
            if (cur.isClickable() && cur.isVisibleToUser()
                    && ScreenBoundsGuard.isSafeNode(cur, screenW, screenH)) {
                return AccessibilityNodeInfo.obtain(cur);
            }
            AccessibilityNodeInfo p = cur.getParent();
            if (cur != n) recycle(cur);
            cur = p;
        }
        if (n.isVisibleToUser() && ScreenBoundsGuard.isSafeNode(n, screenW, screenH)) {
            return AccessibilityNodeInfo.obtain(n);
        }
        return null;
    }

    private static void collectInputHints(AccessibilityNodeInfo n, String[] hints, int depth, List<Match> out,
                                          int screenW, int screenH) {
        if (n == null || depth > MAX_DEPTH || out.size() > MAX_NODES) return;
        String text = cs(n.getText());
        String desc = cs(n.getContentDescription());
        String hay = (text + " " + desc).toLowerCase(Locale.ROOT);
        for (String hint : hints) {
            if (hint == null || hint.isEmpty()) continue;
            if (ScreenBoundsGuard.isBottomNavLabel(hint)) continue;
            if (hay.contains(hint.toLowerCase(Locale.ROOT))) {
                AccessibilityNodeInfo target = editableTarget(n, screenW, screenH);
                if (target != null) out.add(new Match(target, "hint", hint));
            }
        }
        int c = Math.min(n.getChildCount(), 40);
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo child = null;
            try {
                child = n.getChild(i);
                collectInputHints(child, hints, depth + 1, out, screenW, screenH);
            } finally {
                recycle(child);
            }
        }
    }

    private static AccessibilityNodeInfo editableTarget(AccessibilityNodeInfo n, int screenW, int screenH) {
        AccessibilityNodeInfo cur = n;
        for (int i = 0; i < 5 && cur != null; i++) {
            if ((cur.isEditable() || cur.isFocusable())
                    && ScreenBoundsGuard.isSafeNode(cur, screenW, screenH)) {
                return AccessibilityNodeInfo.obtain(cur);
            }
            AccessibilityNodeInfo p = cur.getParent();
            if (cur != n) recycle(cur);
            cur = p;
        }
        return clickableTarget(n, screenW, screenH);
    }

    private static int score(Match m, int screenW, int screenH) {
        if (m == null || m.node == null) return 0;
        Rect r = new Rect();
        m.node.getBoundsInScreen(r);
        int centerY = (r.top + r.bottom) / 2;
        int safeMid = screenH / 2;
        int distFromMid = Math.abs(centerY - safeMid);
        int area = Math.max(1, r.width() * r.height());
        return area / 1000 + (m.node.isClickable() ? 50 : 0) + (500 - Math.min(500, distFromMid));
    }

    private static String cs(CharSequence c) {
        return c == null ? "" : c.toString().trim();
    }

    public static void recycle(AccessibilityNodeInfo n) {
        try {
            if (n != null) n.recycle();
        } catch (Exception ignored) {
        }
    }
}
