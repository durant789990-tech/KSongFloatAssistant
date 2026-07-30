package com.zzy.ksongfloat.ai;

import com.zzy.ksongfloat.ai.model.*;
import org.json.*;

public class AiPromptBuilder {
    public String buildUserMessage(AiAnalysisRequest r) {
        StringBuilder sb = new StringBuilder();
        sb.append("请求类型\n").append(r.requestType)
                .append("\n\n页面类型\n").append(r.pageType)
                .append("\n\n页面置信度\n").append(r.pageConfidence)
                .append("\n\n识别到的昵称\n").append(r.detectedNickname)
                .append("\n\n识别到的歌曲\n");
        for (String s : r.detectedSongTitles) sb.append("- ").append(s).append('\n');
        sb.append("\n请基于下面页面文本生成建议。不要臆测隐私，不要引导发送，不要输出 Markdown。\n\n<page_data>\n")
                .append(limit(r.visibleText, 7000))
                .append("\n</page_data>\n\n用户偏好风格\n")
                .append(limit(r.userStyle, 120))
                .append("\n\n最近已经给过的建议，避免重复\n");
        for (String p : r.previousSuggestions) sb.append("- ").append(limit(p, 180)).append('\n');
        sb.append("\n输出要求\n只返回 JSON 对象");
        if (r.customPrompt != null && r.customPrompt.trim().length() > 0) {
            sb.append("\n\n用户自定义要求\n").append(limit(r.customPrompt, 500));
        }
        return sb.toString();
    }

    public JSONObject buildChatRequest(AiSettings s, String system, String user, boolean useMaxCompletionTokens) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("model", s.model);
        JSONArray msgs = new JSONArray();
        msgs.put(new JSONObject().put("role", "system").put("content", system));
        msgs.put(new JSONObject().put("role", "user").put("content", user));
        root.put("messages", msgs);
        root.put("temperature", s.temperature);
        root.put(useMaxCompletionTokens ? "max_completion_tokens" : "max_tokens", s.maxTokens);
        if (s.strictJson) root.put("response_format", new JSONObject().put("type", "json_object"));
        return root;
    }

    static String limit(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) : s;
    }
}
