package com.zzy.ksongfloat.ui.settings;

import com.zzy.ksongfloat.ai.BaseUrlNormalizer;

public class AiSettingsValidator {
    public static class Result {
        public boolean ok;
        public String message;
        public int timeoutSeconds;
        public double temperature;
        public int maxTokens;

        static Result ok(int timeout, double temp, int max) {
            Result r = new Result(); r.ok = true; r.timeoutSeconds = timeout; r.temperature = temp; r.maxTokens = max; return r;
        }
        static Result fail(String m) { Result r = new Result(); r.ok = false; r.message = m; return r; }
    }

    public static Result validateForSave(String baseUrl, String model, String timeoutText, double temperature, String maxTokensText) {
        BaseUrlNormalizer.Result n = BaseUrlNormalizer.normalize(baseUrl);
        if (!n.ok) return Result.fail(n.error);
        if (model == null || model.trim().isEmpty()) return Result.fail("请填写模型名称。");
        int timeout;
        int maxTokens;
        try { timeout = Integer.parseInt(timeoutText.trim()); } catch (Exception e) { return Result.fail("超时时间必须是数字。"); }
        if (timeout < 5 || timeout > 180) return Result.fail("超时时间范围为 5–180 秒。");
        if (temperature < 0 || temperature > 2) return Result.fail("Temperature 范围为 0.0–2.0。");
        try { maxTokens = Integer.parseInt(maxTokensText.trim()); } catch (Exception e) { return Result.fail("最大输出长度必须是数字。"); }
        if (maxTokens < 100 || maxTokens > 4000) return Result.fail("最大输出长度范围为 100–4000 tokens。");
        return Result.ok(timeout, temperature, maxTokens);
    }

    public static Result validateForTest(String baseUrl, boolean hasApiKey, String keyInput, String model, String timeoutText, double temperature, String maxTokensText) {
        Result r = validateForSave(baseUrl, model, timeoutText, temperature, maxTokensText);
        if (!r.ok) return r;
        if (!hasApiKey && (keyInput == null || keyInput.trim().isEmpty())) return Result.fail("请先填写 API Key。");
        return r;
    }
}
