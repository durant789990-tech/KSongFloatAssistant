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
        List<Match> all = new ArrayList<>();
        collect(root, keywords, 0, all);
        Match best = null;
        for (Match m : all) {
            if (best == null || score(m) > score(best)) best = m;
        }
        return best;
    }

    public static Match findEditable(AccessibilityNodeInfo root) {
        if (root == null) return null;
        List<Match> all = new ArrayList<>();
        walkEditable(root, 0, all);
        return all.isEmpty() ? null : all.get(0);
    }

    public static Match findInputField(AccessibilityNodeInfo root, String... hints) {
        Match editable = findEditable(root);
        if (editable != null) return editable;
        if (root == null || hints == null) return null;
        List<Match> all = new ArrayList<>();
        collectInputHints(root, hints, 0, all);
        Match best = null;
        for (Match m : all) {
            if (best == null || score(m) > score(best)) best = m;
        }
        return best;
    }

    public static List<Match> findUserCards(AccessibilityNodeInfo root) {
        List<Match> out = new ArrayList<>();
        if (root == null) return out;
        Rect screen = new Rect();
        root.getBoundsInScreen(screen);
        int sh = Math.max(screen.height(), 1);
        int sw = Math.max(screen.width(), 1);
        for (UserCardDetector.Candidate c : UserCardDetector.findCandidates(root, sh, sw)) {
            out.add(new Match(c.node, "user_card", c.label));
        }
        return out;
    }

    private static void collect(AccessibilityNodeInfo n, String[] keywords, int depth, List<Match> out) {
        if (n == null || depth > MAX_DEPTH || out.size() > MAX_NODES) return;
        String text = cs(n.getText());
        String desc = cs(n.getContentDescription());
        String hay = (text + " " + desc).toLowerCase(Locale.ROOT);
        for (String kw : keywords) {
            if (kw == null || kw.isEmpty()) continue;
            if (hay.contains(kw.toLowerCase(Locale.ROOT))) {
                AccessibilityNodeInfo target = clickableTarget(n);
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
                collect(child, keywords, depth + 1, out);
            } finally {
                recycle(child);
            }
        }
    }

    private static void walkEditable(AccessibilityNodeInfo n, int depth, List<Match> out) {
        if (n == null || depth > MAX_DEPTH || out.size() > 8) return;
        String cls = n.getClassName() == null ? "" : n.getClassName().toString();
        boolean inputLike = n.isEditable()
                || cls.contains("EditText")
                || cls.contains("TextInput")
                || (n.isFocusable() && (cs(n.getText()).contains("说") || cs(n.getContentDescription()).contains("输入")));
        if (inputLike) {
            out.add(new Match(AccessibilityNodeInfo.obtain(n), "editable", cs(n.getText())));
            return;
        }
        int c = Math.min(n.getChildCount(), 40);
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo child = null;
            try {
                child = n.getChild(i);
                walkEditable(child, depth + 1, out);
            } finally {
                recycle(child);
            }
        }
    }

    private static void walkCards(AccessibilityNodeInfo n, int depth, List<Match> out) {
        if (n == null || depth > MAX_DEPTH || out.size() > 30) return;
        String text = cs(n.getText());
        String desc = cs(n.getContentDescription());
        String id = cs(n.getViewIdResourceName());
        String cls = n.getClassName() == null ? "" : n.getClassName().toString().toLowerCase(Locale.ROOT);
        String merged = (text + " " + desc).toLowerCase(Locale.ROOT);
        boolean hasDistance = merged.contains("km") || merged.contains("距离") || merged.contains("米");
        boolean looksLikeCard = n.isClickable() && n.isVisibleToUser() && (
                (text.length() >= 2 && text.length() <= 24 && !isSystemText(text))
                || (desc.length() >= 2 && desc.length() <= 24 && !isSystemText(desc))
                || hasDistance
                || id.toLowerCase(Locale.ROOT).contains("user")
                || id.toLowerCase(Locale.ROOT).contains("avatar")
                || id.toLowerCase(Locale.ROOT).contains("nearby")
                || cls.contains("recyclerview")
                || cls.contains("viewgroup"));
        if (looksLikeCard && (hasDistance || (!isSystemText(text) && text.length() >= 2) || (!isSystemText(desc) && desc.length() >= 2))) {
            AccessibilityNodeInfo target = clickableTarget(n);
            if (target != null) {
                String label = text.length() > 0 ? text : desc;
                if (!label.isEmpty() && !isSystemText(label)) {
                    out.add(new Match(target, "card", label));
                } else if (hasDistance) {
                    out.add(new Match(target, "card", merged.length() > 24 ? merged.substring(0, 24) : merged));
                }
            }
        }
        int c = Math.min(n.getChildCount(), 40);
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo child = null;
            try {
                child = n.getChild(i);
                walkCards(child, depth + 1, out);
            } finally {
                recycle(child);
            }
        }
    }

    private static AccessibilityNodeInfo clickableTarget(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur = n;
        for (int i = 0; i < 4 && cur != null; i++) {
            if (cur.isClickable() && cur.isVisibleToUser()) return AccessibilityNodeInfo.obtain(cur);
            AccessibilityNodeInfo p = cur.getParent();
            if (cur != n) recycle(cur);
            cur = p;
        }
        if (n.isVisibleToUser()) return AccessibilityNodeInfo.obtain(n);
        return null;
    }

    private static void collectInputHints(AccessibilityNodeInfo n, String[] hints, int depth, List<Match> out) {
        if (n == null || depth > MAX_DEPTH || out.size() > MAX_NODES) return;
        String text = cs(n.getText());
        String desc = cs(n.getContentDescription());
        String hay = (text + " " + desc).toLowerCase(Locale.ROOT);
        for (String hint : hints) {
            if (hint == null || hint.isEmpty()) continue;
            if (hay.contains(hint.toLowerCase(Locale.ROOT))) {
                AccessibilityNodeInfo target = editableTarget(n);
                if (target != null) out.add(new Match(target, "hint", hint));
            }
        }
        int c = Math.min(n.getChildCount(), 40);
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo child = null;
            try {
                child = n.getChild(i);
                collectInputHints(child, hints, depth + 1, out);
            } finally {
                recycle(child);
            }
        }
    }

    private static AccessibilityNodeInfo editableTarget(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur = n;
        for (int i = 0; i < 5 && cur != null; i++) {
            if (cur.isEditable() || cur.isFocusable()) return AccessibilityNodeInfo.obtain(cur);
            AccessibilityNodeInfo p = cur.getParent();
            if (cur != n) recycle(cur);
            cur = p;
        }
        return clickableTarget(n);
    }

    private static int score(Match m) {
        if (m == null || m.node == null) return 0;
        Rect r = new Rect();
        m.node.getBoundsInScreen(r);
        int area = Math.max(1, r.width() * r.height());
        return area / 1000 + (m.node.isClickable() ? 50 : 0);
    }

    private static boolean isSystemText(String s) {
        if (s == null || s.isEmpty()) return true;
        String l = s.toLowerCase(Locale.ROOT);
        return l.contains("附近") || l.contains("距离") || l.contains("关注") || l.contains("粉丝")
                || l.contains("评论") || l.contains("私信") || l.contains("返回") || l.contains("搜索")
                || l.contains("同城") || l.contains("推荐") || l.contains("消息");
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
