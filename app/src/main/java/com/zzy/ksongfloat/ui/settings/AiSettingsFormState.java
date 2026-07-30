package com.zzy.ksongfloat.ui.settings;

import com.zzy.ksongfloat.ai.AiSettings;

public class AiSettingsFormState {
    public String baseUrl = "";
    public String apiKeyInput = "";
    public boolean hasStoredApiKey;
    public String model = "";
    public String timeout = "60";
    public double temperature = 0.7;
    public String maxTokens = "1200";
    public boolean strictJson;
    public String style = "自然友好";
    public String customPrompt = "";
    public boolean allowOcr = true;
    public boolean allowAccessibility = true;
    public boolean autoDeleteScreenshots = true;
    public boolean consent;

    public static AiSettingsFormState from(AiSettings s, boolean hasStoredApiKey, boolean autoDeleteScreenshots) {
        AiSettingsFormState f = new AiSettingsFormState();
        f.baseUrl = s.baseUrl == null ? "" : s.baseUrl;
        f.model = s.model == null ? "" : s.model;
        f.timeout = String.valueOf(s.timeoutSeconds);
        f.temperature = s.temperature;
        f.maxTokens = String.valueOf(s.maxTokens);
        f.strictJson = s.strictJson;
        f.style = s.userStyle == null || s.userStyle.length() == 0 ? "自然友好" : s.userStyle;
        f.customPrompt = s.customPrompt == null ? "" : s.customPrompt;
        f.allowOcr = s.allowOcrText;
        f.allowAccessibility = s.allowAccessibilityText;
        f.consent = s.aiConsent;
        f.hasStoredApiKey = hasStoredApiKey;
        f.autoDeleteScreenshots = autoDeleteScreenshots;
        return f;
    }

    public String snapshot() {
        return baseUrl + "\n" + model + "\n" + timeout + "\n" + temperature + "\n" + maxTokens + "\n" + strictJson
                + "\n" + style + "\n" + customPrompt + "\n" + allowOcr + "\n" + allowAccessibility + "\n" + autoDeleteScreenshots + "\n" + consent + "\n" + hasStoredApiKey;
    }
}
