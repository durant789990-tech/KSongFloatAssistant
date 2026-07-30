package com.zzy.ksongfloat.automation;

import org.json.JSONObject;

/** AI 返回的单步动作计划。 */
public final class AiActionPlan {
    public final String action;
    public final String targetNodeId;
    public final String text;
    public final String reason;
    public final String rawJson;

    AiActionPlan(String action, String targetNodeId, String text, String reason, String rawJson) {
        this.action = action == null ? "WAIT" : action;
        this.targetNodeId = targetNodeId;
        this.text = text;
        this.reason = reason == null ? "" : reason;
        this.rawJson = rawJson == null ? "" : rawJson;
    }

    public static AiActionPlan waitPlan(String reason) {
        return new AiActionPlan("WAIT", null, null, reason, "");
    }

    public static AiActionPlan stopPlan(String reason) {
        return new AiActionPlan("STOP", null, null, reason, "");
    }

    public static AiActionPlan fromJson(String json) throws Exception {
        JSONObject o = new JSONObject(json);
        return new AiActionPlan(
                o.optString("action", "WAIT"),
                o.has("targetNodeId") && !o.isNull("targetNodeId") ? o.optString("targetNodeId") : null,
                o.has("text") && !o.isNull("text") ? o.optString("text") : null,
                o.optString("reason", ""),
                json
        );
    }
}
