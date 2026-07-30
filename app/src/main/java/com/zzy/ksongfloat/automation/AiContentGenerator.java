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
import com.zzy.ksongfloat.ai.BaseUrlNormalizer;
import com.zzy.ksongfloat.security.SecureStorage;

import org.json.JSONObject;

/** AI 生成评论/私信草稿。 */
public final class AiContentGenerator {
    public static class Draft {
        public final String type;
        public final String text;
        public final String reason;

        Draft(String type, String text, String reason) {
            this.type = type == null ? "" : type;
            this.text = text == null ? "" : text;
            this.reason = reason == null ? "" : reason;
        }
    }

    public static Draft generateComment(Context ctx, AutomationSession session, String pageSummary, String userName) {
        return generate(ctx, session, "COMMENT_DRAFT", pageSummary, userName,
                "根据公开页面内容生成一条自然、简短的中文评论草稿，15字以内。返回 JSON: {\"type\":\"COMMENT_DRAFT\",\"text\":\"...\",\"reason\":\"...\"}");
    }

    public static Draft generatePrivateMessage(Context ctx, AutomationSession session, String pageSummary, String userName) {
        return generate(ctx, session, "PRIVATE_MESSAGE_DRAFT", pageSummary, userName,
                "根据公开主页信息生成一条自然、简短的中文私信草稿，20字以内。返回 JSON: {\"type\":\"PRIVATE_MESSAGE_DRAFT\",\"text\":\"...\",\"reason\":\"...\"}");
    }

    private static Draft generate(Context ctx, AutomationSession session, String type,
                                  String pageSummary, String userName, String instruction) {
        if (!AutomationSessionManager.get().isValid(session.sessionId)) {
            return new Draft(type, "", "session 无效");
        }
        if (!AiConfigRepository.get().isReadyForRequest(ctx)) {
            AutomationLog.warn("AI 未配置，无法生成草稿");
            return new Draft(type, "", "AI 未配置");
        }
        try {
            AutomationLog.info("AI_REQUEST_STARTED draft=" + type);
            AiSettings s = AiSettingsRepository.load(ctx);
            String key = SecureStorage.loadApiKey(ctx);
            BaseUrlNormalizer.Result nr = BaseUrlNormalizer.normalize(s.baseUrl);
            JSONObject user = new JSONObject();
            user.put("task", type);
            user.put("userName", userName == null ? "" : userName);
            user.put("pageSummary", trim(pageSummary, 600));
            user.put("instruction", instruction);
            AiPromptBuilder pb = new AiPromptBuilder();
            JSONObject body = pb.buildChatRequest(s,
                    "只返回严格 JSON，不要 markdown。", user.toString(), false);
            AiClient client = AutomationSessionManager.get().aiClient();
            if (client == null) client = new AiClient();
            AiCallResult cr = client.chat(new AiRequest(nr.url, key, body.toString(), s.timeoutSeconds), false);
            if (!cr.success) {
                AutomationLog.warn("AI_RESPONSE_FAILED " + cr.error.message);
                return new Draft(type, "", cr.error.message);
            }
            AutomationLog.info("AI_RESPONSE_RECEIVED");
            String json = extractJson(cr.response.content);
            JSONObject o = new JSONObject(json);
            String text = o.optString("text", "").trim();
            if (text.length() > 30) text = text.substring(0, 30);
            AutomationLog.info("AI_DRAFT_GENERATED type=" + type + " len=" + text.length());
            return new Draft(o.optString("type", type), text, o.optString("reason", ""));
        } catch (Exception e) {
            AutomationLog.warn("AI_DRAFT_ERROR " + e.getMessage());
            return new Draft(type, "", e.getMessage());
        }
    }

    private static String extractJson(String content) throws Exception {
        if (content == null) return "{}";
        String s = content.trim();
        int a = s.indexOf('{');
        int b = s.lastIndexOf('}');
        if (a >= 0 && b > a) return s.substring(a, b + 1);
        throw new Exception("无 JSON");
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        return s.length() <= max ? s : s.substring(0, max);
    }
}
