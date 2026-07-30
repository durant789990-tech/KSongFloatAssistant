package com.zzy.ksongfloat.automation;

import android.content.Context;

import com.zzy.ksongfloat.ai.AiCallResult;
import com.zzy.ksongfloat.ai.AiClient;
import com.zzy.ksongfloat.ai.AiPromptBuilder;
import com.zzy.ksongfloat.ai.AiRequest;
import com.zzy.ksongfloat.ai.AiRuntimeConfig;
import com.zzy.ksongfloat.ai.AiSettings;

import org.json.JSONObject;

import java.util.Random;

/** AI 生成评论/私信，失败时使用本地兜底文案。 */
public final class AiContentGenerator {
    private static final Random RND = new Random();
    private static final String[] FALLBACK_COMMENTS = {
            "唱得太棒啦！",
            "声音真好听",
            "很好听！",
            "太厉害了！",
            "听得入迷了"
    };

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

    public static String generateCommentWithFallback(Context ctx, AutomationSession session,
                                                     String pageSummary, String userName) {
        Draft d = generateComment(ctx, session, pageSummary, userName);
        if (d.text != null && !d.text.trim().isEmpty()) {
            return d.text.trim();
        }
        String fallback = pickFallback();
        AutomationLog.info("使用本地兜底评论：" + fallback);
        return fallback;
    }

    public static Draft generateComment(Context ctx, AutomationSession session, String pageSummary, String userName) {
        return generate(ctx, session, "COMMENT_DRAFT", pageSummary, userName,
                "根据公开页面内容生成一条自然、简短的中文评论，15字以内。返回 JSON: {\"type\":\"COMMENT_DRAFT\",\"text\":\"...\",\"reason\":\"...\"}");
    }

    public static Draft generatePrivateMessage(Context ctx, AutomationSession session, String pageSummary, String userName) {
        return generate(ctx, session, "PRIVATE_MESSAGE_DRAFT", pageSummary, userName,
                "根据公开主页信息生成一条自然、简短的中文私信，20字以内。返回 JSON: {\"type\":\"PRIVATE_MESSAGE_DRAFT\",\"text\":\"...\",\"reason\":\"...\"}");
    }

    private static Draft generate(Context ctx, AutomationSession session, String type,
                                  String pageSummary, String userName, String instruction) {
        if (session == null) {
            return new Draft(type, pickFallback(), "无 session，使用兜底");
        }
        AiRuntimeConfig cfg = AiRuntimeConfig.resolve(ctx);
        if (!cfg.ready) {
            AutomationLog.warn("AI 未配置（url/key/model 不完整），使用本地兜底");
            return new Draft(type, pickFallback(), "AI 未配置");
        }
        try {
            AutomationLog.info("AI_REQUEST_STARTED type=" + type + " model=" + cfg.model);
            JSONObject user = new JSONObject();
            user.put("task", type);
            user.put("userName", userName == null ? "" : userName);
            user.put("pageSummary", trim(pageSummary, 600));
            user.put("instruction", instruction);
            AiPromptBuilder pb = new AiPromptBuilder();
            JSONObject body = pb.buildChatRequest(cfg.settings,
                    "只返回严格 JSON，不要 markdown。", user.toString(), false);
            AiClient client = AutomationSessionManager.get().aiClient();
            if (client == null) client = new AiClient();
            AiCallResult cr = client.chat(
                    new AiRequest(cfg.baseUrl, cfg.apiKey, body.toString(), cfg.settings.timeoutSeconds), false);
            if (!cr.success) {
                AutomationLog.warn("AI_RESPONSE_FAILED " + cr.error.message);
                return new Draft(type, pickFallback(), cr.error.message);
            }
            AutomationLog.info("AI_RESPONSE_RECEIVED");
            String json = extractJson(cr.response.content);
            JSONObject o = new JSONObject(json);
            String text = o.optString("text", "").trim();
            if (text.isEmpty()) {
                return new Draft(type, pickFallback(), "AI 返回空文本");
            }
            if (text.length() > 30) text = text.substring(0, 30);
            AutomationLog.info("AI_COMMENT_GENERATED len=" + text.length());
            return new Draft(o.optString("type", type), text, o.optString("reason", ""));
        } catch (Exception e) {
            AutomationLog.warn("AI_ERROR " + e.getMessage() + "，使用兜底");
            return new Draft(type, pickFallback(), e.getMessage());
        }
    }

    private static String pickFallback() {
        return FALLBACK_COMMENTS[RND.nextInt(FALLBACK_COMMENTS.length)];
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
