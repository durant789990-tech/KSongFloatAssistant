package com.zzy.ksongfloat.automation;

import android.content.Context;

import com.zzy.ksongfloat.automation.AutomationSettings;
import com.zzy.ksongfloat.classifier.PageType;
import com.zzy.ksongfloat.runtime.ForegroundAppResolver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** 本地白名单校验，AI 建议可被否决。 */
public final class ActionPlanValidator {
    private static final Set<String> BASE = new HashSet<>(Arrays.asList(
            "WAIT", "RESCAN", "SWIPE_UP", "OPEN_USER_CARD", "OPEN_COMMENT", "SELECT_TEXT_INPUT",
            "GENERATE_COMMENT", "FILL_TEXT", "OPEN_PRIVATE_MESSAGE", "STOP"
    ));
    private static final Set<String> PROD_FORBIDDEN = new HashSet<>(Arrays.asList(
            "SEND_TEXT", "FOLLOW_USER"
    ));

    public static class Result {
        public final boolean ok;
        public final AiActionPlan plan;
        public final String reason;

        Result(boolean ok, AiActionPlan plan, String reason) {
            this.ok = ok;
            this.plan = plan;
            this.reason = reason == null ? "" : reason;
        }
    }

    public static Result validate(Context ctx, AutomationSession session, AiActionPlan plan,
                                  PageType pageType, String targetNodeIdExists) {
        if (session == null || !AutomationSessionManager.get().isValid(session.sessionId)) {
            return new Result(false, AiActionPlan.stopPlan("session 无效"), "session 无效");
        }
        if (plan == null) return new Result(false, AiActionPlan.waitPlan("空计划"), "空计划");
        String action = plan.action == null ? "" : plan.action.trim().toUpperCase(Locale.ROOT);
        if (PROD_FORBIDDEN.contains(action)) {
            return new Result(false, AiActionPlan.stopPlan("禁止动作 " + action), "禁止动作");
        }
        AutomationSettings settings = AutomationSettingsRepository.load(ctx);
        if (!BASE.contains(action)) {
            return new Result(false, AiActionPlan.stopPlan("未定义动作 " + action), "未在白名单");
        }
        if ("SEND_TEXT".equals(action) || "FOLLOW_USER".equals(action)) {
            return new Result(false, AiActionPlan.stopPlan("测试模式未通过"), "永久禁止");
        }
        if (isSensitivePage(pageType)) {
            return new Result(false, AiActionPlan.stopPlan("敏感页面"), "敏感页面");
        }
        ForegroundAppResolver.Result fr = ForegroundAppResolver.resolve(ctx);
        if (fr.presence != ForegroundAppResolver.AppPresence.TARGET_APP
                && fr.presence != ForegroundAppResolver.AppPresence.ASSISTANT_OVERLAY) {
            return new Result(false, AiActionPlan.stopPlan("不在目标 App"), "包名不符");
        }
        if ("OPEN_USER_CARD".equals(action) && (plan.targetNodeId == null || plan.targetNodeId.isEmpty())) {
            return new Result(false, AiActionPlan.waitPlan("缺少 targetNodeId"), "缺少节点");
        }
        if (("FILL_TEXT".equals(action) || "GENERATE_COMMENT".equals(action)) && settings.testMode) {
            // 测试模式允许生成/填入预览，但不发送
        }
        AutomationLog.info("AI_PLAN_VALIDATED action=" + action);
        return new Result(true, new AiActionPlan(action, plan.targetNodeId, plan.text, plan.reason, plan.rawJson), "");
    }

    private static boolean isSensitivePage(PageType t) {
        return t == PageType.KSONG_SETTINGS || t == PageType.ACCOUNT_SETTINGS
                || t == PageType.SYSTEM_SETTINGS || t == PageType.SYSTEM_DIALOG;
    }
}
