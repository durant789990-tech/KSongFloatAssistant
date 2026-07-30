package com.zzy.ksongfloat.automation;

import android.view.accessibility.AccessibilityNodeInfo;

import com.zzy.ksongfloat.capture.PageTextResult;
import com.zzy.ksongfloat.classifier.PageType;

import java.util.List;
import java.util.Locale;

/** 判断当前是否为同城/附近用户列表页。 */
public final class CityUserListDetector {
    public static class Result {
        public final boolean cityList;
        public final PageType pageType;
        public final double confidence;
        public final String reason;
        public final int cardCount;

        Result(boolean cityList, PageType pageType, double confidence, String reason, int cardCount) {
            this.cityList = cityList;
            this.pageType = pageType;
            this.confidence = confidence;
            this.reason = reason == null ? "" : reason;
            this.cardCount = cardCount;
        }
    }

    public static Result detect(PageTextResult page, AccessibilityNodeInfo root,
                                List<UserCardDetector.Candidate> cards) {
        int cardCount = cards == null ? 0 : cards.size();
        String hay = buildHaystack(page, root);
        boolean hasCityCtx = hay.contains("同城") || hay.contains("附近") || hay.contains("距离")
                || hay.contains("km") || hay.contains("米");
        boolean hasNav = hay.contains("动态") && hay.contains("我的");
        boolean enoughCards = cardCount >= 2;

        if (hasCityCtx && enoughCards) {
            return new Result(true, PageType.CITY_USER_LIST, 0.85,
                    "同城上下文+用户卡片=" + cardCount, cardCount);
        }
        if (enoughCards && hasNav && !hay.contains("设置") && !hay.contains("切换账号")) {
            return new Result(true, PageType.CITY_USER_LIST, 0.72,
                    "列表结构+底部导航+卡片=" + cardCount, cardCount);
        }
        if (cardCount >= 1 && hasCityCtx) {
            return new Result(true, PageType.CITY_USER_LIST, 0.65,
                    "弱匹配：有同城上下文", cardCount);
        }
        if (hay.contains("设置") || hay.contains("切换账号") || hay.contains("退出")) {
            return new Result(false, PageType.KSONG_SETTINGS, 0.9, "敏感/设置页", cardCount);
        }
        return new Result(false, PageType.UNKNOWN_KSONG_PAGE, 0.3,
                "不是同城列表（卡片=" + cardCount + "）", cardCount);
    }

    private static String buildHaystack(PageTextResult page, AccessibilityNodeInfo root) {
        StringBuilder sb = new StringBuilder();
        if (page != null) {
            append(sb, page.mergedText);
            append(sb, page.windowTitle);
        }
        appendTree(sb, root, 0);
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private static void appendTree(StringBuilder sb, AccessibilityNodeInfo n, int depth) {
        if (n == null || depth > 8) return;
        append(sb, n.getText());
        append(sb, n.getContentDescription());
        int c = Math.min(n.getChildCount(), 20);
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo ch = n.getChild(i);
            try {
                appendTree(sb, ch, depth + 1);
            } finally {
                NodeFinder.recycle(ch);
            }
        }
    }

    private static void append(StringBuilder sb, CharSequence s) {
        if (s == null || s.length() == 0) return;
        if (sb.length() > 0) sb.append('\n');
        sb.append(s);
    }
}
