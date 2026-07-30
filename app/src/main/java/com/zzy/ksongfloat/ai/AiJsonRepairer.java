package com.zzy.ksongfloat.ai;

public class AiJsonRepairer {
    public String buildRepairUserMessage(String bad, String reason) {
        return "下面内容不是合法 JSON。请修复为一个 JSON 对象，不要输出 Markdown，不要解释。错误原因："
                + limit(reason, 200)
                + "\n必须包含字段：pageType,nickname,profileSummary,musicPreferences,conversationAngles,"
                + "commentSuggestions[{text,reason}],privateMessageSuggestions[{text,reason}],"
                + "riskFlags[{code,message}],confidence。\n原始内容：\n"
                + limit(bad, 3000);
    }

    private String limit(String s, int n) {
        return s == null ? "" : (s.length() > n ? s.substring(0, n) : s);
    }
}
