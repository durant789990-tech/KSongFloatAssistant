package com.zzy.ksongfloat.config;

import android.content.Context;

public class TargetAppConfig {
    public static final String DEFAULT_PACKAGE = "com.tencent.karaoke";
    private static final String PREF = "target_app";
    private static final String KEY_PACKAGE = "package";

    public static String getTargetPackage(Context context) {
        if (context == null) return DEFAULT_PACKAGE;
        String saved = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_PACKAGE, DEFAULT_PACKAGE);
        String normalized = normalizePackageName(saved);
        return normalized.length() == 0 ? DEFAULT_PACKAGE : normalized;
    }

    public static void saveTargetPackage(Context context, String packageName) {
        if (context == null) return;
        String normalized = normalizePackageName(packageName);
        if (normalized.length() == 0) return;
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PACKAGE, normalized)
                .apply();
    }

    public static void resetTargetPackage(Context context) {
        if (context == null) return;
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PACKAGE, DEFAULT_PACKAGE)
                .apply();
    }

    public static boolean matches(Context context, String packageName) {
        if (packageName == null) return false;
        String current = normalizePackageName(packageName);
        if (current.length() == 0) return false;
        String target = getTargetPackage(context);
        if (current.equals(target)) return true;

        // 默认目标保持对官方包名及同系调试包的兼容；用户保存自定义包名时优先按精确包名判断。
        return DEFAULT_PACKAGE.equals(target) && current.toLowerCase().contains("karaoke");
    }

    public static String normalizePackageName(String packageName) {
        if (packageName == null) return "";
        String value = packageName.trim();
        if (value.startsWith("package:")) value = value.substring("package:".length()).trim();
        int slash = value.indexOf('/');
        if (slash >= 0) value = value.substring(0, slash).trim();
        return value;
    }
}
