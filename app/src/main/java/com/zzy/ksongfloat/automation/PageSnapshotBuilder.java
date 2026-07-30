package com.zzy.ksongfloat.automation;

import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;

import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;
import com.zzy.ksongfloat.capture.PageTextResult;
import com.zzy.ksongfloat.classifier.PageClassificationResult;
import com.zzy.ksongfloat.classifier.PageType;
import com.zzy.ksongfloat.runtime.ForegroundAppDetector;
import com.zzy.ksongfloat.runtime.ForegroundAppResolver;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 构建脱敏页面快照供 AI 规划。
 */
public final class PageSnapshotBuilder {
    public static class Snapshot {
        public final PageTextResult page;
        public final PageClassificationResult cls;
        public final JSONArray candidates;
        public final int windowId;
        public final String sessionId;

        Snapshot(PageTextResult page, PageClassificationResult cls, JSONArray candidates, int windowId, String sessionId) {
            this.page = page;
            this.cls = cls;
            this.candidates = candidates;
            this.windowId = windowId;
            this.sessionId = sessionId;
        }

        public JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("pageType", cls == null ? PageType.UNKNOWN.name() : cls.pageType.name());
            o.put("confidence", cls == null ? 0 : cls.confidence);
            o.put("windowId", windowId);
            o.put("packageName", page == null ? "" : page.packageName);
            String summary = page == null ? "" : trim(page.mergedText, 800);
            o.put("visibleTextSummary", summary);
            o.put("candidates", candidates == null ? new JSONArray() : candidates);
            return o;
        }
    }

    public static Snapshot build(Context ctx, AutomationSession session, PageTextResult page,
                                 PageClassificationResult cls, AccessibilityNodeInfo root) {
        int windowId = ForegroundAppDetector.lastWindowId();
        JSONArray arr = new JSONArray();
        if (root != null) {
            int sh = ctx.getResources().getDisplayMetrics().heightPixels;
            int sw = ctx.getResources().getDisplayMetrics().widthPixels;
            List<UserCardDetector.Candidate> cards = UserCardDetector.findCandidates(root, sh, sw);
            int i = 0;
            for (UserCardDetector.Candidate c : cards) {
                try {
                    JSONObject item = new JSONObject();
                    item.put("id", "n" + (++i));
                    item.put("label", trim(c.label, 40));
                    item.put("clickable", true);
                    item.put("top", c.bounds.top);
                    item.put("bottom", c.bounds.bottom);
                    arr.put(item);
                } catch (Exception ignored) {
                } finally {
                    NodeFinder.recycle(c.node);
                }
            }
        }
        return new Snapshot(page, cls, arr, windowId, session == null ? "" : session.sessionId);
    }

    public static String rootSignature(AccessibilityNodeInfo root) {
        if (root == null) return "";
        String cls = root.getClassName() == null ? "" : root.getClassName().toString();
        return cls + "|" + root.getChildCount() + "|" + ForegroundAppDetector.lastWindowId();
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        return s.length() <= max ? s : s.substring(0, max);
    }
}
