package com.zzy.ksongfloat.shizuku;

import android.content.Context;

import com.zzy.ksongfloat.engine.ActionResult;

/** 已禁用：自动化仅使用无障碍，不再依赖 Shizuku。 */
public final class ShizukuHelper {
    private ShizukuHelper() {}

    public static void init(Context context) {
        // disabled
    }

    public static boolean ping() {
        return false;
    }

    public static boolean hasPermission() {
        return false;
    }

    public static void requestPermission(int requestCode) {
        // disabled
    }

    public static String statusLabel(Context context) {
        return "已禁用";
    }

    public static ActionResult testSwipeUp(Context context) {
        return ActionResult.SERVICE_UNAVAILABLE;
    }

    public static ActionResult testTap(Context context, int x, int y) {
        return ActionResult.SERVICE_UNAVAILABLE;
    }

    public static ActionResult testBack(Context context) {
        return ActionResult.SERVICE_UNAVAILABLE;
    }

    public static void cancelPending() {
        // disabled
    }
}
