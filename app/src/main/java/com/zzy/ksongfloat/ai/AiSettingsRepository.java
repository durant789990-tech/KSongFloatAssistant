package com.zzy.ksongfloat.ai;

import android.content.Context;
import android.content.SharedPreferences;

public class AiSettingsRepository {
    private static final String P = "ai_settings";

    public static AiSettings load(Context c) {
        SharedPreferences sp = c.getSharedPreferences(P, Context.MODE_PRIVATE);
        AiSettings s = new AiSettings();
        s.baseUrl = sp.getString("baseUrl", "");
        s.model = sp.getString("model", "");
        s.timeoutSeconds = sp.getInt("timeout", 60);
        s.temperature = Double.longBitsToDouble(sp.getLong("temp", Double.doubleToLongBits(0.7)));
        s.maxTokens = sp.getInt("maxTokens", 1200);
        s.strictJson = sp.getBoolean("strict", false);
        s.allowOcrText = sp.getBoolean("allowOcr", true);
        s.allowAccessibilityText = sp.getBoolean("allowAcc", true);
        s.userStyle = sp.getString("style", "自然、礼貌、简短");
        s.customPrompt = sp.getString("customPrompt", "");
        s.aiConsent = sp.getBoolean("consent", false);
        return s;
    }

    public static void save(Context c, AiSettings s) {
        if (s.userStyle != null && s.userStyle.length() > 120) s.userStyle = s.userStyle.substring(0, 120);
        if (s.customPrompt != null && s.customPrompt.length() > 500) s.customPrompt = s.customPrompt.substring(0, 500);
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
                .putString("baseUrl", n(s.baseUrl))
                .putString("model", n(s.model))
                .putInt("timeout", Math.max(5, Math.min(180, s.timeoutSeconds)))
                .putLong("temp", Double.doubleToLongBits(Math.max(0, Math.min(2, s.temperature))))
                .putInt("maxTokens", Math.max(100, Math.min(4000, s.maxTokens)))
                .putBoolean("strict", s.strictJson)
                .putBoolean("allowOcr", s.allowOcrText)
                .putBoolean("allowAcc", s.allowAccessibilityText)
                .putString("style", n(s.userStyle))
                .putString("customPrompt", n(s.customPrompt))
                .putBoolean("consent", s.aiConsent)
                .apply();
    }

    public static void resetDefaults(Context c) {
        AiSettings s = new AiSettings();
        save(c, s);
    }

    public static void setConsent(Context c, boolean v) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putBoolean("consent", v).apply();
    }

    private static String n(String x) { return x == null ? "" : x; }
}
