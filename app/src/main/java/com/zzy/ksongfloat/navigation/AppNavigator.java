package com.zzy.ksongfloat.navigation;

import android.app.Activity;
import android.content.Intent;

import com.zzy.ksongfloat.DebugActivity;
import com.zzy.ksongfloat.MainActivity;
import com.zzy.ksongfloat.ai.AiSettingsActivity;
import com.zzy.ksongfloat.diagnostics.DiagnosticsActivity;
import com.zzy.ksongfloat.guide.UsageGuideActivity;
import com.zzy.ksongfloat.history.HistoryActivity;
import com.zzy.ksongfloat.privacy.PrivacySettingsActivity;

public class AppNavigator {
    private static long lastOpenAt;
    private static String lastTarget = "";

    public static void home(Activity a) { open(a, MainActivity.class, true); }
    public static void aiSettings(Activity a) { open(a, AiSettingsActivity.class, false); }
    public static void debug(Activity a) { open(a, DebugActivity.class, false); }
    public static void history(Activity a) { open(a, HistoryActivity.class, false); }
    public static void privacy(Activity a) { open(a, PrivacySettingsActivity.class, false); }
    public static void diagnostics(Activity a) { open(a, DiagnosticsActivity.class, false); }
    public static void usageGuide(Activity a) { open(a, UsageGuideActivity.class, false); }

    public static void open(Activity a, Class<?> cls, boolean clearTop) {
        long now = System.currentTimeMillis();
        String name = cls.getName();
        if (name.equals(lastTarget) && now - lastOpenAt < 650) return;
        lastOpenAt = now;
        lastTarget = name;
        Intent i = new Intent(a, cls);
        if (clearTop) i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        else i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        a.startActivity(i);
        a.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
    }

    public static void homeAndFinish(Activity a) {
        Intent i = new Intent(a, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        a.startActivity(i);
        a.finish();
        a.overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
    }

    public static void finish(Activity a) {
        if (!(a instanceof MainActivity) && a.isTaskRoot()) {
            Intent i = new Intent(a, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            a.startActivity(i);
        }
        a.finish();
        a.overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
    }
}
