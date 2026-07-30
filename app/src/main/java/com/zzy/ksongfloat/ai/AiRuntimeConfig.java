package com.zzy.ksongfloat.ai;

import android.content.Context;

import com.zzy.ksongfloat.security.SecureStorage;

/**
 * 自动化运行时从 SharedPreferences + 加密存储实时解析 AI 配置，避免缓存过期导致 Key 为空。
 */
public final class AiRuntimeConfig {
    public final String baseUrl;
    public final String apiKey;
    public final String model;
    public final AiSettings settings;
    public final boolean ready;

    private AiRuntimeConfig(String baseUrl, String apiKey, String model, AiSettings settings, boolean ready) {
        this.baseUrl = baseUrl == null ? "" : baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model == null ? "" : model;
        this.settings = settings;
        this.ready = ready;
    }

    public static AiRuntimeConfig resolve(Context context) {
        if (context == null) {
            return new AiRuntimeConfig("", "", "", new AiSettings(), false);
        }
        Context app = context.getApplicationContext();
        AiConfigRepository.get().refresh(app);
        AiSettings s = AiSettingsRepository.load(app);
        String apiKey = SecureStorage.loadApiKeySafe(app);
        BaseUrlNormalizer.Result nr = BaseUrlNormalizer.normalize(s.baseUrl == null ? "" : s.baseUrl);
        String url = nr.ok ? nr.url : "";
        String model = s.model == null ? "" : s.model.trim();
        boolean ready = !url.isEmpty() && !apiKey.isEmpty() && !model.isEmpty();
        return new AiRuntimeConfig(url, apiKey, model, s, ready);
    }
}
