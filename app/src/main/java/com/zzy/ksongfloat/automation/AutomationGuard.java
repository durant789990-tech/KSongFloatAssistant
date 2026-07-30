package com.zzy.ksongfloat.automation;

import android.app.KeyguardManager;
import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;

import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;
import com.zzy.ksongfloat.runtime.ForegroundAppResolver;

public final class AutomationGuard {
    public static final String TARGET_PKG = ForegroundAppResolver.TARGET_PKG;

    public enum BlockReason {
        OK,
        NOT_RUNNING,
        PAUSED,
        NO_ACCESSIBILITY,
        NO_ROOT,
        WRONG_PACKAGE,
        UNKNOWN_TRANSIENT,
        SCREEN_LOCKED,
        BLOCKED_OVERLAY
    }

    public static class CheckResult {
        public final boolean allowed;
        public final BlockReason reason;
        public final String message;
        public final String packageName;

        CheckResult(boolean allowed, BlockReason reason, String message, String packageName) {
            this.allowed = allowed;
            this.reason = reason;
            this.message = message;
            this.packageName = packageName;
        }
    }

    public static CheckResult checkAction(Context ctx, AutomationOrchestrator orchestrator) {
        if (orchestrator == null || !orchestrator.isRunning()) {
            return blocked(BlockReason.NOT_RUNNING, "自动化未运行", pkg(ctx));
        }
        if (orchestrator.isPaused()) {
            return blocked(BlockReason.PAUSED, "任务已暂停", pkg(ctx));
        }
        if (!KSongAccessibilityService.isConnected() || KSongAccessibilityService.getInstance() == null) {
            return blocked(BlockReason.NO_ACCESSIBILITY, "无障碍服务未连接", pkg(ctx));
        }
        if (isScreenLocked(ctx)) {
            return blocked(BlockReason.SCREEN_LOCKED, "屏幕已锁定", pkg(ctx));
        }

        ForegroundAppResolver.Result fr = ForegroundAppResolver.resolve(ctx);
        if (fr.presence == ForegroundAppResolver.AppPresence.OTHER_APP) {
            return blocked(BlockReason.WRONG_PACKAGE, "已离开全民K歌", fr.packageName);
        }
        if (fr.presence == ForegroundAppResolver.AppPresence.ASSISTANT_OVERLAY
                || fr.presence == ForegroundAppResolver.AppPresence.TARGET_APP) {
            // 允许：助手悬浮窗在上层时仍视为目标 App
        } else if (fr.presence == ForegroundAppResolver.AppPresence.ACCESSIBILITY_UNAVAILABLE) {
            return blocked(BlockReason.NO_ACCESSIBILITY, "无障碍服务未连接", fr.packageName);
        }
        if (fr.presence == ForegroundAppResolver.AppPresence.UNKNOWN_TRANSIENT) {
            return blocked(BlockReason.UNKNOWN_TRANSIENT, "前台应用暂无法确认，等待重试", fr.packageName);
        }

        AccessibilityNodeInfo root = KSongAccessibilityService.getInstance().getRootInActiveWindowSafe();
        if (root == null) {
            return blocked(BlockReason.NO_ROOT, "无法读取当前页面节点", fr.packageName);
        }
        root.recycle();
        if (looksBlockedOverlay(fr.packageName)) {
            return blocked(BlockReason.BLOCKED_OVERLAY, "当前可能被系统弹窗或输入法遮挡", fr.packageName);
        }
        return new CheckResult(true, BlockReason.OK, "", fr.packageName);
    }

    public static boolean canRecoverSwipe(Context ctx, AutomationOrchestrator orchestrator) {
        CheckResult base = checkAction(ctx, orchestrator);
        if (base.reason == BlockReason.UNKNOWN_TRANSIENT || base.reason == BlockReason.NO_ROOT) {
            ForegroundAppResolver.Result fr = ForegroundAppResolver.resolve(ctx);
            return fr.presence == ForegroundAppResolver.AppPresence.TARGET_APP;
        }
        if (!base.allowed) return false;
        return !looksBlockedOverlay(base.packageName);
    }

    private static boolean isScreenLocked(Context ctx) {
        KeyguardManager km = (KeyguardManager) ctx.getSystemService(Context.KEYGUARD_SERVICE);
        return km != null && km.isKeyguardLocked();
    }

    private static boolean looksBlockedOverlay(String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        String p = pkg.toLowerCase();
        return p.contains("com.android.settings")
                || p.contains("com.google.android.permissioncontroller")
                || p.contains("com.android.packageinstaller")
                || p.contains("inputmethod")
                || p.contains("systemui")
                || p.contains("com.miui.home")
                || p.contains("launcher");
    }

    private static CheckResult blocked(BlockReason reason, String msg, String pkg) {
        return new CheckResult(false, reason, msg, pkg == null ? "" : pkg);
    }

    private static String pkg(Context ctx) {
        return ForegroundAppResolver.displayPackage(ctx);
    }
}
