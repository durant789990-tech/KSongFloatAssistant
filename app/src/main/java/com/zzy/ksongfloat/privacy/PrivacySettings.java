package com.zzy.ksongfloat.privacy;

import android.content.Context;

public class PrivacySettings {
    private static final String P = "privacy_settings";
    public boolean allowOcrText = true;
    public boolean allowAccessibilityText = true;
    public boolean autoDeleteScreenshots = true;
    public boolean saveAnalysisHistory = true;
    public boolean autoHideFloatingWindow = true;

    public static PrivacySettings load(Context c) {
        PrivacySettings s = new PrivacySettings();
        android.content.SharedPreferences sp = c.getSharedPreferences(P, Context.MODE_PRIVATE);
        s.allowOcrText = sp.getBoolean("allowOcrText", true);
        s.allowAccessibilityText = sp.getBoolean("allowAccessibilityText", true);
        s.autoDeleteScreenshots = sp.getBoolean("autoDeleteScreenshots", true);
        s.saveAnalysisHistory = sp.getBoolean("saveAnalysisHistory", true);
        s.autoHideFloatingWindow = sp.getBoolean("autoHideFloatingWindow", true);
        return s;
    }

    public static void save(Context c, PrivacySettings s) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
                .putBoolean("allowOcrText", s.allowOcrText)
                .putBoolean("allowAccessibilityText", s.allowAccessibilityText)
                .putBoolean("autoDeleteScreenshots", s.autoDeleteScreenshots)
                .putBoolean("saveAnalysisHistory", s.saveAnalysisHistory)
                .putBoolean("autoHideFloatingWindow", s.autoHideFloatingWindow)
                .apply();
    }
}
