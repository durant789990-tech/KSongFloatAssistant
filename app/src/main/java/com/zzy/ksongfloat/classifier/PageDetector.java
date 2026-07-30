package com.zzy.ksongfloat.classifier;

import com.zzy.ksongfloat.capture.PageTextResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 在 PageClassifier 基础上增加导航栏/Feed 容错，减少 UNKNOWN 误判。
 */
public class PageDetector {
    private static final String S_NEAR = "\u9644\u8fd1";
    private static final String S_CITY = "\u540c\u57ce";
    private static final String S_RECOMMEND = "\u63a8\u8350";
    private static final String S_FOLLOW = "\u5173\u6ce8";
    private static final String S_HOME = "\u9996\u9875";
    private static final String S_DYNAMIC = "\u52a8\u6001";
    private static final String S_MESSAGE = "\u6d88\u606f";
    private static final String S_SQUARE = "\u5e7f\u573a";
    private static final String S_SING = "\u6b4c\u623f";
    private static final String S_DISTANCE = "\u8ddd\u79bb";

    private final PageClassifier classifier = new PageClassifier();

    public PageClassificationResult detect(PageTextResult page) {
        PageClassificationResult base = classifier.classify(page);
        if (base.pageType != PageType.UNKNOWN) return base;
        return detectFromNavigation(page, base);
    }

    private PageClassificationResult detectFromNavigation(PageTextResult page, PageClassificationResult base) {
        List<String> warnings = base.warnings == null ? new ArrayList<>() : new ArrayList<>(base.warnings);
        String all = buildHaystack(page);
        if (all.isEmpty()) return base;

        int nearbyHits = countHits(all, S_NEAR, S_CITY, S_DISTANCE, "km", "\u7c73", S_SQUARE, S_SING);
        int feedHits = countHits(all, S_RECOMMEND, S_FOLLOW, S_HOME, S_DYNAMIC, S_MESSAGE, S_SING, S_SQUARE);
        List<PageEvidence> evidence = new ArrayList<>();
        if (nearbyHits >= 1) {
            addEvidence(evidence, "nav", S_NEAR + "/" + S_CITY, 0.35);
            warnings.add("PageDetector: nearby nav fallback");
            return new PageClassificationResult(PageType.RECOMMEND_FEED, 0.58, evidence, warnings,
                    base.detectedNickname, base.detectedSongTitles);
        }
        if (feedHits >= 2) {
            addEvidence(evidence, "nav", S_RECOMMEND + "/" + S_FOLLOW, 0.3);
            warnings.add("PageDetector: feed nav fallback");
            return new PageClassificationResult(PageType.RECOMMEND_FEED, 0.52, evidence, warnings,
                    base.detectedNickname, base.detectedSongTitles);
        }
        if (page != null && page.editableCount > 0) {
            if (all.contains("\u79c1\u4fe1") || all.contains(S_MESSAGE) || all.contains("\u804a\u5929")) {
                addEvidence(evidence, "editable", "message", 0.4);
                return new PageClassificationResult(PageType.PRIVATE_MESSAGE_INPUT, 0.5, evidence, warnings, "", new ArrayList<>());
            }
            if (all.contains("\u8bc4\u8bba") || all.contains("\u8bf4\u70b9\u4ec0\u4e48")) {
                addEvidence(evidence, "editable", "comment", 0.4);
                return new PageClassificationResult(PageType.COMMENT_INPUT, 0.5, evidence, warnings, "", new ArrayList<>());
            }
        }
        return base;
    }

    public boolean looksLikeScrollableFeed(PageTextResult page) {
        String all = buildHaystack(page);
        return countHits(all, S_RECOMMEND, S_FOLLOW, S_HOME, S_DYNAMIC, S_NEAR, S_CITY, S_SING) >= 1;
    }

    private String buildHaystack(PageTextResult page) {
        if (page == null) return "";
        StringBuilder sb = new StringBuilder();
        appendLower(sb, page.packageName);
        appendLower(sb, page.windowTitle);
        appendLower(sb, page.mergedText);
        if (page.contentDescriptions != null) for (String d : page.contentDescriptions) appendLower(sb, d);
        return sb.toString();
    }

    private void appendLower(StringBuilder sb, String s) {
        if (s == null || s.isEmpty()) return;
        if (sb.length() > 0) sb.append('\n');
        sb.append(s.toLowerCase(Locale.ROOT));
    }

    private int countHits(String hay, String... keywords) {
        int hits = 0;
        for (String kw : keywords) {
            if (kw != null && !kw.isEmpty() && hay.contains(kw.toLowerCase(Locale.ROOT))) hits++;
        }
        return hits;
    }

    private void addEvidence(List<PageEvidence> list, String src, String text, double weight) {
        list.add(new PageEvidence(src, text, weight));
    }
}
