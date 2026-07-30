package com.zzy.ksongfloat.ai;

public class AiConfigState {
    public final boolean configured;
    public final String baseUrl;
    public final String normalizedChatUrl;
    public final String model;
    public final boolean hasApiKey;
    public final String statusLabel;
    public final long updatedAt;

    public AiConfigState(boolean configured, String baseUrl, String normalizedChatUrl,
                         String model, boolean hasApiKey, String statusLabel, long updatedAt) {
        this.configured = configured;
        this.baseUrl = baseUrl == null ? "" : baseUrl;
        this.normalizedChatUrl = normalizedChatUrl == null ? "" : normalizedChatUrl;
        this.model = model == null ? "" : model;
        this.hasApiKey = hasApiKey;
        this.statusLabel = statusLabel == null ? "" : statusLabel;
        this.updatedAt = updatedAt;
    }

    public static AiConfigState empty() {
        return new AiConfigState(false, "", "", "", false, "未配置", System.currentTimeMillis());
    }
}
