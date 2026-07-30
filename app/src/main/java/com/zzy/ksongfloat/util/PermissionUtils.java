package com.zzy.ksongfloat.util;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import com.zzy.ksongfloat.accessibility.AccessibilityStateDetector;

public class PermissionUtils {
    public static boolean canDrawOverlays(Context c) {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c);
    }

    public static boolean isNotificationEnabled(Context c) {
        if (Build.VERSION.SDK_INT < 24) return true;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.areNotificationsEnabled();
    }

    public static boolean isBatteryIgnoring(Context c) {
        if (Build.VERSION.SDK_INT < 23) return true;
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
    }

    public static boolean isAccessibilityEnabled(Context c) {
        return AccessibilityStateDetector.isSystemEnabled(c);
    }

    public static boolean isCaptureAuthorized(Context c) {
        return c.getSharedPreferences("runtime_state", Context.MODE_PRIVATE).getBoolean("capture_authorized", false);
    }

    public static void setCaptureAuthorized(Context c, boolean value) {
        c.getSharedPreferences("runtime_state", Context.MODE_PRIVATE).edit().putBoolean("capture_authorized", value).apply();
    }

    public static boolean hasRequiredForFullRun(Context c) {
        return canDrawOverlays(c) && isAccessibilityEnabled(c) && isNotificationEnabled(c);
    }

    public static String missingRequiredText(Context c) {
        StringBuilder sb = new StringBuilder();
        if (!canDrawOverlays(c)) sb.append("悬浮窗、");
        if (!isAccessibilityEnabled(c)) sb.append("无障碍服务、");
        if (!isNotificationEnabled(c)) sb.append("通知权限、");
        if (sb.length() == 0) return "";
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
