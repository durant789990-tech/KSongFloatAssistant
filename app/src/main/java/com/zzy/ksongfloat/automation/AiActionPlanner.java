package com.zzy.ksongfloat.automation;

import android.content.Context;

import com.zzy.ksongfloat.ai.AiCallResult;
import com.zzy.ksongfloat.ai.AiClient;
import com.zzy.ksongfloat.ai.AiConfigRepository;
import com.zzy.ksongfloat.ai.AiJsonRepairer;
import com.zzy.ksongfloat.ai.AiPromptBuilder;
import com.zzy.ksongfloat.ai.AiRequest;
import com.zzy.ksongfloat.ai.AiSettings;
import com.zzy.ksongfloat.ai.AiSettingsRepository;
import com.zzy.ksongfloat.ai.AiSystemPrompt;
import com.zzy.ksongfloat.ai.BaseUrlNormalizer;
import com.zzy.ksongfloat.security.SecureStorage;

import org.json.JSONObject;

/**
 * 请求 AI 返回单步白名单动作。
 */
public final class AiActionPlanner {
    private static final String SYSTEM = "你是 K 歌自动化助手。只返回严格 JSON，不要 markdown。"
            + "允许 action: WAIT, RESCAN, SWIPE_UP, OPEN_USER_CARD, OPEN_COMMENT, SELECT_TEXT_INPUT,"
            + " GENERATE_COMMENT, FILL_TEXT, OPEN_PRIVATE_MESSAGE, STOP。"
            + "禁止坐标、Shell、Intent、设置、退出账号、关注、发送。"
            + "格式: {\"action\":\"WAIT\",\"targetNodeId\":null,\"text\":null,\"reason\":\"...\"}";

    public static AiActionPlan plan(Context ctx, AutomationSession session, PageSnapshotBuilder.Snapshot snap) {
        if (session == null || !AutomationSessionManager.get().isValid(session.sessionId)) {
            return AiActionPlan.stopPlan("session 无效");
        }
        if (!AiConfigRepository.get().isReadyForRequest(ctx)) {
            AutomationLog.warn("AI 未配置，只允许 WAIT/RESCAN/SWIPE_UP/STOP");
            return AiActionPlan.waitPlan("AI 未配置");
        }
        try {
            AutomationLog.info("AI_REQUEST_STARTED session=" + session.sessionId);
            AiSettings s = AiSettingsRepository.load(ctx);
            String key = SecureStorage.loadApiKey(ctx);
            BaseUrlNormalizer.Result nr = BaseUrlNormalizer.normalize(s.baseUrl);
            if (!nr.ok || key.isEmpty()) return AiActionPlan.waitPlan("AI 配置无效");

            JSONObject user = new JSONObject();
            user.put("task", "plan_next_action");
            user.put("testMode", AutomationSettingsRepository.load(ctx).testMode);
            user.put("snapshot", snap.toJson());
            user.put("allowedActions", "WAIT,RESCAN,SWIPE_UP,OPEN_USER_CARD,OPEN_COMMENT,SELECT_TEXT_INPUT,GENERATE_COMMENT,FILL_TEXT,OPEN_PRIVATE_MESSAGE,STOP");

            AiPromptBuilder pb = new AiPromptBuilder();
            JSONObject body = pb.buildChatRequest(s, SYSTEM, user.toString(), false);
            AiClient client = AutomationSessionManager.get().aiClient();
            if (client == null) client = new AiClient();
            AiCallResult cr = client.chat(new AiRequest(nr.url, key, body.toString(), s.timeoutSeconds), false);
            if (!cr.success) {
                AutomationLog.warn("AI_RESPONSE_FAILED " + cr.error.message);
                return AiActionPlan.waitPlan("AI 请求失败");
            }
            AutomationLog.info("AI_RESPONSE_RECEIVED");
            String content = extractJson(cr.response.content);
            AiActionPlan plan;
            try {
                plan = AiActionPlan.fromJson(content);
            } catch (Exception first) {
                String repair = new AiJsonRepairer().buildRepairUserMessage(content, first.getMessage());
                JSONObject repairBody = pb.buildChatRequest(s, SYSTEM, repair, false);
                AiCallResult rr = client.chat(new AiRequest(nr.url, key, repairBody.toString(), s.timeoutSeconds), false);
                if (!rr.success) throw first;
                plan = AiActionPlan.fromJson(extractJson(rr.response.content));
            }
            AutomationLog.info("AI_PLAN_PARSED action=" + plan.action + " reason=" + plan.reason);
            session.lastAiPlan = plan.action + ": " + plan.reason;
            return plan;
        } catch (Exception e) {
            AutomationLog.warn("AI_PLAN_ERROR " + e.getMessage());
            return AiActionPlan.waitPlan("AI 异常");
        }
    }

    private static String extractJson(String content) {
        if (content == null) return "{}";
        String s = content.trim();
        int a = s.indexOf('{');
        int b = s.lastIndexOf('}');
        if (a >= 0 && b > a) return s.substring(a, b + 1);
        return s;
    }
}
