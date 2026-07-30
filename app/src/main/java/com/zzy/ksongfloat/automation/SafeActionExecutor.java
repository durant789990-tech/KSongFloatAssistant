package com.zzy.ksongfloat.automation;

import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;

import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;
import com.zzy.ksongfloat.engine.ActionResult;
import com.zzy.ksongfloat.engine.AutomationEngineSelector;
import com.zzy.ksongfloat.runtime.ForegroundAppResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 只执行一步已验证动作，禁止坐标点击。 */
public final class SafeActionExecutor {
    private final Map<String, UserCardDetector.Candidate> candidateMap = new HashMap<>();

    public void indexCandidates(List<UserCardDetector.Candidate> list) {
        candidateMap.clear();
        if (list == null) return;
        int i = 0;
        for (UserCardDetector.Candidate c : list) {
            candidateMap.put("n" + (++i), c);
        }
    }

    public String execute(Context ctx, AutomationSession session, AiActionPlan plan) {
        if (!AutomationSessionManager.get().isActive(session.sessionId)) {
            return "session 已失效";
        }
        switch (plan.action) {
            case "WAIT":
                return "WAIT";
            case "RESCAN":
                PageCacheManager.get().invalidate("RESCAN");
                return "RESCAN";
            case "STOP":
                AutomationLog.warn("AI 请求停止（强行模式已忽略）");
                return "STOP";
            case "SWIPE_UP":
                if (!AutomationGuard.checkAction(ctx, AutomationOrchestrator.get()).allowed) {
                    return "SWIPE_BLOCKED";
                }
                ActionResult r = AutomationEngineSelector.swipeUp(ctx, AutomationOrchestrator.get());
                AutomationLog.event("AI_ACTION", ForegroundAppResolver.TARGET_PKG, session.currentPage, "swipe_up", r.name());
                PageCacheManager.get().invalidate("swipe");
                return r.name();
            case "OPEN_USER_CARD":
                return openUserCard(plan.targetNodeId);
            case "OPEN_COMMENT":
                return clickSafeText(session, "评论", "说点什么");
            case "SELECT_TEXT_INPUT":
                return clickSafeText(session, "说点什么", "写评论", "输入");
            case "GENERATE_COMMENT":
            case "FILL_TEXT":
                return fillTextPreview(session, plan.text);
            case "OPEN_PRIVATE_MESSAGE":
                return clickSafeText(session, "私信", "发消息");
            default:
                return "UNSUPPORTED";
        }
    }

    private String openUserCard(String targetNodeId) {
        UserCardDetector.Candidate c = targetNodeId == null ? null : candidateMap.get(targetNodeId);
        if (c == null) {
            AutomationLog.warn("OPEN_USER_CARD 未找到候选节点 id=" + targetNodeId);
            return "NO_TARGET";
        }
        if (UserCardDetector.isExcludedNavLabel(c.label) || DangerousActionGuard.isDangerousLabel(c.label)) {
            AutomationLog.warn("危险节点已跳过：" + c.label);
            return "DANGEROUS";
        }
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        if (svc == null) return "NO_A11Y";
        boolean ok = new NodeActionController(svc).click(new NodeFinder.Match(c.node, "user_card", c.label));
        PageCacheManager.get().invalidate("open_user");
        AutomationLog.info("AI_ACTION_EXECUTED OPEN_USER_CARD " + c.label + " result=" + ok);
        return ok ? "SUCCESS" : "FAILED";
    }

    private String extractNodeId(AutomationSession session) {
        // lastAiPlan may contain target; fallback map first entry
        for (String k : candidateMap.keySet()) return k;
        return "";
    }

    private String planNode(AutomationSession session) {
        for (String k : candidateMap.keySet()) return k;
        return "";
    }

    private String clickSafeText(AutomationSession session, String... texts) {
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        if (svc == null) return "NO_A11Y";
        AccessibilityNodeInfo root = null;
        try {
            root = svc.getRootInActiveWindowSafe();
            NodeFinder.Match m = NodeFinder.findClickableByTexts(root, texts);
            if (m == null || DangerousActionGuard.isDangerousNode(m.node)) {
                return "NO_SAFE_NODE";
            }
            boolean ok = new NodeActionController(svc).click(m);
            PageCacheManager.get().invalidate("click_text");
            AutomationLog.info("AI_ACTION_EXECUTED click texts result=" + ok);
            return ok ? "SUCCESS" : "FAILED";
        } finally {
            NodeFinder.recycle(root);
        }
    }

    private String fillTextPreview(AutomationSession session, String text) {
        AutomationSettings settings = AutomationSettingsRepository.load(
                KSongAccessibilityService.getInstance() != null
                        ? KSongAccessibilityService.getInstance().getApplicationContext() : null);
        if (text == null || text.trim().isEmpty()) return "NO_TEXT";
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        if (svc == null) return "NO_A11Y";
        NodeActionController.FillResult r = new NodeActionController(svc).fillInputAndSend(text, false);
        AutomationLog.info("AI_ACTION_EXECUTED FILL_TEXT preview=" + settings.testMode + " filled=" + r.filled);
        return r.filled ? "SUCCESS" : "FAILED";
    }
}
