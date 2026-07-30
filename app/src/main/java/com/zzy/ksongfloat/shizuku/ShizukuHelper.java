package com.zzy.ksongfloat.shizuku;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import com.zzy.ksongfloat.automation.AutomationLog;
import com.zzy.ksongfloat.automation.ShellTextEscaper;
import com.zzy.ksongfloat.engine.ActionResult;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import rikka.shizuku.Shizuku;

/**
 * Shizuku Shell 增强：文本注入与按键。未连接时自动降级到无障碍策略。
 */
public final class ShizukuHelper {
    private static volatile boolean initialized;
    private static volatile boolean permissionGranted;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ShizukuHelper() {
    }

    public static void init(Context context) {
        if (initialized) return;
        initialized = true;
        try {
            Shizuku.addBinderReceivedListenerSticky(() -> refreshPermission());
            Shizuku.addBinderDeadListener(() -> permissionGranted = false);
            Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                permissionGranted = grantResult == PackageManager.PERMISSION_GRANTED;
            });
            refreshPermission();
        } catch (Throwable t) {
            AutomationLog.warn("Shizuku 初始化失败：" + t.getMessage());
        }
    }

    private static void refreshPermission() {
        try {
            if (Shizuku.pingBinder()) {
                permissionGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
            } else {
                permissionGranted = false;
            }
        } catch (Throwable t) {
            permissionGranted = false;
        }
    }

    public static boolean ping() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasPermission() {
        return permissionGranted;
    }

    public static boolean isReady() {
        return ping() && hasPermission();
    }

    public static void requestPermission(int requestCode) {
        if (!ping()) return;
        MAIN.post(() -> {
            try {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(requestCode);
                } else {
                    permissionGranted = true;
                }
            } catch (Throwable ignored) {
            }
        });
    }

    public static String statusLabel(Context context) {
        if (!ping()) return "Shizuku 未运行";
        if (!hasPermission()) return "Shizuku 已连接，未授权";
        return "Shizuku 已就绪";
    }

    public static boolean shizukuExec(String command) {
        if (!isReady() || command == null || command.isEmpty()) return false;
        Process process = null;
        try {
            process = newProcess(new String[]{"sh", "-c", command}, null, null);
            if (process == null) return false;
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Throwable t) {
            AutomationLog.warn("Shizuku exec 失败：" + t.getMessage());
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }

    private static Process newProcess(String[] cmd, String[] env, String dir) throws Exception {
        Method m = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
        m.setAccessible(true);
        Object result = m.invoke(null, cmd, env, dir);
        return (Process) result;
    }

    public static boolean inputText(String text) {
        return shizukuExec(ShellTextEscaper.buildInputTextCommand(text));
    }

    public static boolean keyEnter() {
        return shizukuExec("input keyevent 66");
    }

    public static ActionResult testSwipeUp(Context context) {
        if (!isReady()) return ActionResult.SERVICE_UNAVAILABLE;
        return shizukuExec("input swipe 540 1800 540 500 350") ? ActionResult.SUCCESS : ActionResult.FAILED;
    }

    public static ActionResult testTap(Context context, int x, int y) {
        if (!isReady()) return ActionResult.SERVICE_UNAVAILABLE;
        return shizukuExec("input tap " + x + " " + y) ? ActionResult.SUCCESS : ActionResult.FAILED;
    }

    public static ActionResult testBack(Context context) {
        if (!isReady()) return ActionResult.SERVICE_UNAVAILABLE;
        return shizukuExec("input keyevent 4") ? ActionResult.SUCCESS : ActionResult.FAILED;
    }

    public static void cancelPending() {
        // no-op
    }
}
