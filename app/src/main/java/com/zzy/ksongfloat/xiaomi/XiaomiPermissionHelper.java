package com.zzy.ksongfloat.xiaomi;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

public class XiaomiPermissionHelper {
    public static boolean isXiaomiLike() {
        String brand = android.os.Build.BRAND == null ? "" : android.os.Build.BRAND.toLowerCase();
        String manufacturer = android.os.Build.MANUFACTURER == null ? "" : android.os.Build.MANUFACTURER.toLowerCase();
        return brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") || manufacturer.contains("xiaomi");
    }

    public static void openAutoStart(Activity a) {
        tryStart(a, new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                "自启动管理", "设置 → 应用设置 → 授权管理 → 自启动管理 → K歌悬浮助手 → 允许");
    }

    public static void openBattery(Activity a) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.getPackageName()));
        tryStart(a, i, "省电策略", "设置 → 应用设置 → 应用管理 → K歌悬浮助手 → 省电策略 → 无限制");
    }

    public static void openAppPermission(Activity a) {
        tryStart(a, new Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", a.getPackageName()),
                "应用权限管理", "设置 → 应用设置 → 应用管理 → K歌悬浮助手 → 权限管理");
    }

    public static void openFloatingWindow(Activity a) {
        tryStart(a, new Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", a.getPackageName()),
                "悬浮窗设置", "设置 → 应用设置 → 权限管理 → 显示悬浮窗 → K歌悬浮助手 → 允许");
    }

    public static void openAccessibility(Activity a) {
        tryStart(a, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                "无障碍设置", "设置 → 更多设置 → 无障碍 → 已下载的应用 → K歌悬浮助手 → 开启");
    }

    public static String guideText() {
        return "不同 HyperOS / MIUI 版本入口可能略有不同。\n\n建议检查：\n"
                + "1. 设置 → 应用设置 → 应用管理 → K歌悬浮助手 → 省电策略 → 无限制\n"
                + "2. 设置 → 应用设置 → 授权管理 → 自启动管理 → 允许\n"
                + "3. 设置 → 应用设置 → 权限管理 → 显示悬浮窗 → 允许\n"
                + "4. 设置 → 无障碍 → 已下载的应用 → K歌悬浮助手 → 开启";
    }

    private static void tryStart(Activity a, Intent i, String title, String fallbackPath) {
        try {
            a.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(a, title + "无法自动跳转，请手动打开：\n" + fallbackPath + "\n不同 HyperOS 版本入口可能略有不同。", Toast.LENGTH_LONG).show();
        }
    }
}
