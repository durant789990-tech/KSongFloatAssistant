package com.zzy.ksongfloat.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

public class AccessibilityStateDetector {
    public static ComponentName serviceComponent(Context context) {
        return new ComponentName(context, KSongAccessibilityService.class);
    }

    public static boolean isSystemEnabled(Context context) {
        if (context == null) return false;
        ComponentName expected = serviceComponent(context);
        String flat = expected.flattenToString();
        String shortFlat = expected.flattenToShortString();

        try {
            AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am != null) {
                List<AccessibilityServiceInfo> list = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
                if (list != null) {
                    for (AccessibilityServiceInfo info : list) {
                        String id = info == null ? null : info.getId();
                        if (matches(id, flat, shortFlat)) return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            String enabled = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabled != null) {
                TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
                splitter.setString(enabled);
                while (splitter.hasNext()) {
                    if (matches(splitter.next(), flat, shortFlat)) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static AccessibilityConnectionState detect(Context context) {
        boolean enabled = isSystemEnabled(context);
        if (!enabled) {
            AccessibilityStateRepository.setState(AccessibilityConnectionState.DISABLED);
            return AccessibilityConnectionState.DISABLED;
        }
        AccessibilityConnectionState runtime = AccessibilityStateRepository.state();
        if (KSongAccessibilityService.isConnected()) return AccessibilityConnectionState.CONNECTED;
        if (runtime == AccessibilityConnectionState.INTERRUPTED) return AccessibilityConnectionState.INTERRUPTED;
        AccessibilityStateRepository.setState(AccessibilityConnectionState.ENABLED_NOT_CONNECTED);
        return AccessibilityConnectionState.ENABLED_NOT_CONNECTED;
    }

    public static boolean matches(String actual, String flat, String shortFlat) {
        if (actual == null) return false;
        String value = actual.trim();
        if (value.equals(flat) || value.equals(shortFlat)) return true;
        String expectedPkg = "";
        String expectedCls = "";
        int slash = flat.indexOf('/');
        if (slash > 0) {
            expectedPkg = flat.substring(0, slash);
            expectedCls = flat.substring(slash + 1);
        }
        int actualSlash = value.indexOf('/');
        if (actualSlash <= 0) return false;
        String actualPkg = value.substring(0, actualSlash);
        String actualCls = value.substring(actualSlash + 1);
        if (actualCls.startsWith(".")) actualCls = actualPkg + actualCls;
        return actualPkg.equals(expectedPkg) && actualCls.equals(expectedCls);
    }
}
