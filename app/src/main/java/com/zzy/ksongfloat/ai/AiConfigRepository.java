package com.zzy.ksongfloat.ai;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.zzy.ksongfloat.security.SecureStorage;

/**
 * AI 配置唯一数据源。isConfigured = Base URL 有效 + 已保存 API Key + 模型非空。
 */
public final class AiConfigRepository {
    private static final AiConfigRepository INSTANCE = new AiConfigRepository();
    private final MutableLiveData<AiConfigState> configLive = new MutableLiveData<>(AiConfigState.empty());
    private volatile AiConfigState cached = AiConfigState.empty();

    public static AiConfigRepository get() {
        return INSTANCE;
    }

    public LiveData<AiConfigState> observeConfigState() {
        return configLive;
    }

    public AiConfigState getCurrentState() {
        return cached;
    }

    public boolean isConfigured() {
        return cached.configured;
    }

    public void refresh(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        AiSettings s = AiSettingsRepository.load(app);
        boolean hasKey = SecureStorage.hasApiKey(app);
        BaseUrlNormalizer.Result nr = BaseUrlNormalizer.normalize(s.baseUrl);
        boolean urlOk = nr.ok;
        boolean modelOk = s.model != null && !s.model.trim().isEmpty();
        boolean configured = urlOk && hasKey && modelOk;
        String label = configured ? "已配置" : buildMissingLabel(urlOk, hasKey, modelOk);
        AiConfigState state = new AiConfigState(
                configured,
                s.baseUrl == null ? "" : s.baseUrl.trim(),
                urlOk ? nr.url : "",
                s.model == null ? "" : s.model.trim(),
                hasKey,
                label,
                System.currentTimeMillis());
        cached = state;
        configLive.postValue(state);
    }

    public void saveConfig(Context context, AiSettings settings, String apiKeyInput) throws Exception {
        if (context == null || settings == null) return;
        Context app = context.getApplicationContext();
        AiSettingsRepository.save(app, settings);
        if (apiKeyInput != null && !apiKeyInput.trim().isEmpty()) {
            SecureStorage.saveApiKey(app, apiKeyInput.trim());
        }
        refresh(app);
    }

    public void clearApiKey(Context context) {
        if (context == null) return;
        SecureStorage.clearApiKey(context.getApplicationContext());
        refresh(context);
    }

    public void clearConfig(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        AiSettingsRepository.resetDefaults(app);
        SecureStorage.clearApiKey(app);
        refresh(app);
    }

    public void attachPreferenceListener(Context context) {
        Context app = context.getApplicationContext();
        SharedPreferences sp = app.getSharedPreferences("ai_settings", Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener((p, key) -> refresh(app));
        refresh(app);
    }

    /** 自动化流水线：Base URL + API Key + Model 齐全即可，不依赖 UI 缓存或 consent 勾选。 */
    public boolean isReadyForAutomation(Context context) {
        return AiRuntimeConfig.resolve(context).ready;
    }

    /** 实际发起 AI 请求前：需要用户同意隐私条款 */
    public boolean isReadyForRequest(Context context) {
        AiSettings s = AiSettingsRepository.load(context);
        return isReadyForAutomation(context) && s.aiConsent;
    }

    private static String buildMissingLabel(boolean urlOk, boolean hasKey, boolean modelOk) {
        if (!urlOk) return "未配置：Base URL";
        if (!hasKey) return "未配置：API Key";
        if (!modelOk) return "未配置：模型";
        return "未配置";
    }
}
