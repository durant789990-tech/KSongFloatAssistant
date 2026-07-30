package com.zzy.ksongfloat.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

public class SettingsNavigator {
    public static void openOverlay(Activity a) {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + a.getPackageName()));
            a.startActivity(i);
        } catch (Exception e) { showMiuiGuide(a, "悬浮窗权限", "设置 > 应用 > 权限管理 > 显示悬浮窗 > K歌悬浮助手 > 允许"); }
    }

    public static void openAccessibility(Activity a) {
        try { a.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        catch (Exception e) { showMiuiGuide(a, "无障碍服务", "设置 > 无障碍 > 已下载的应用 > K歌悬浮助手 > 开启"); }
    }

    public static void openNotification(Activity a) {
        try {
            Intent i = new Intent();
            if (Build.VERSION.SDK_INT >= 26) {
                i.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                i.putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName());
            } else {
                i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(Uri.parse("package:" + a.getPackageName()));
            }
            a.startActivity(i);
        } catch (Exception e) { showMiuiGuide(a, "通知权限", "设置 > 通知与控制中心 > 应用通知 > K歌悬浮助手 > 允许通知"); }
    }

    public static void openBattery(Activity a) {
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + a.getPackageName()));
            a.startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.getPackageName()));
                a.startActivity(i);
            } catch (Exception ignored) { showMiuiGuide(a, "电池优化", "设置 > 电池 > 应用省电管理 > K歌悬浮助手 > 无限制"); }
        }
    }

    public static void openMiuiBackgroundGuide(Activity a) {
        showMiuiGuide(a, "小米后台权限", "建议打开：自启动、后台弹出界面、显示悬浮窗，并把省电策略设置为无限制。");
    }

    public static void showMiuiGuide(Context c, String title, String text) {
        Toast.makeText(c, title + "：" + text, Toast.LENGTH_LONG).show();
    }
}
