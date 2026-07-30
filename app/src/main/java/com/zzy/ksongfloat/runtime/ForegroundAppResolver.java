package com.zzy.ksongfloat.runtime;

import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;
import com.zzy.ksongfloat.config.TargetAppConfig;

import java.util.List;

/**
 * 前台包名解析。忽略助手自身悬浮窗/Service 事件，避免误判 com.zzy.ksongfloat。
 */
public final class ForegroundAppResolver {
    public static final String TARGET_PKG = "com.tencent.karaoke";
    private static final long TARGET_CACHE_MS = 2000L;

    public enum AppPresence {
        TARGET_APP,
        OTHER_APP,
        UNKNOWN_TRANSIENT,
        ASSISTANT_OVERLAY,
        SYSTEM_UI,
        ACCESSIBILITY_UNAVAILABLE
    }

    public static class Result {
        public final AppPresence presence;
        public final String packageName;
        public final String source;
        public final String underlyingPackage;

        Result(AppPresence presence, String packageName, String source, String underlying) {
            this.presence = presence;
            this.packageName = packageName == null ? "" : packageName;
            this.source = source == null ? "" : source;
            this.underlyingPackage = underlying == null ? "" : underlying;
        }
    }

    private static volatile String cachedTargetPkg = "";
    private static volatile long cachedTargetAt = 0L;

    public static Result resolve(Context ctx) {
        if (ctx == null) return new Result(AppPresence.ACCESSIBILITY_UNAVAILABLE, "", "no_ctx", "");
        if (!KSongAccessibilityService.isConnected()) {
            return new Result(AppPresence.ACCESSIBILITY_UNAVAILABLE, "", "no_a11y", "");
        }
        String assistantPkg = ctx.getPackageName();
        WindowPick pick = pickFromWindows(assistantPkg);
        if (pick != null) {
            if (pick.assistantOverlay && isTargetPkg(pick.underlying)) {
                cacheTarget(pick.underlying);
                return new Result(AppPresence.ASSISTANT_OVERLAY, pick.underlying, "overlay_under", pick.underlying);
            }
            if (isAssistantPkg(assistantPkg, pick.topPkg)) {
                if (isTargetPkg(pick.underlying)) {
                    cacheTarget(pick.underlying);
                    return new Result(AppPresence.ASSISTANT_OVERLAY, pick.underlying, "assistant_event_under", pick.underlying);
                }
            }
            if (isSystemUi(pick.topPkg)) {
                if (isCachedTarget()) {
                    return new Result(AppPresence.TARGET_APP, cachedTargetPkg, "system_ui_cache", cachedTargetPkg);
                }
                return new Result(AppPresence.SYSTEM_UI, pick.topPkg, "system_ui", pick.underlying);
            }
            if (isTargetPkg(pick.topPkg)) {
                cacheTarget(pick.topPkg);
                return new Result(AppPresence.TARGET_APP, pick.topPkg, pick.source, pick.topPkg);
            }
            if (!pick.topPkg.isEmpty() && !isAssistantPkg(assistantPkg, pick.topPkg)) {
                return new Result(AppPresence.OTHER_APP, pick.topPkg, pick.source, pick.underlying);
            }
        }

        String eventPkg = ForegroundAppDetector.currentPackageName();
        if (!eventPkg.isEmpty() && !isAssistantPkg(assistantPkg, eventPkg)) {
            if (isTargetPkg(eventPkg)) {
                cacheTarget(eventPkg);
                return new Result(AppPresence.TARGET_APP, eventPkg, "event", eventPkg);
            }
            if (isSystemUi(eventPkg)) {
                if (isCachedTarget()) return new Result(AppPresence.TARGET_APP, cachedTargetPkg, "event_system_cache", cachedTargetPkg);
                return new Result(AppPresence.SYSTEM_UI, eventPkg, "event_system", "");
            }
            return new Result(AppPresence.OTHER_APP, eventPkg, "event", "");
        }

        if (isCachedTarget()) {
            return new Result(AppPresence.TARGET_APP, cachedTargetPkg, "target_cache", cachedTargetPkg);
        }
        return new Result(AppPresence.UNKNOWN_TRANSIENT, "", "unknown", "");
    }

    public static boolean isTargetOrCached(Context ctx) {
        AppPresence p = resolve(ctx).presence;
        return p == AppPresence.TARGET_APP || p == AppPresence.ASSISTANT_OVERLAY;
    }

    public static String displayPackage(Context ctx) {
        Result r = resolve(ctx);
        if (r.presence == AppPresence.TARGET_APP || r.presence == AppPresence.ASSISTANT_OVERLAY) {
            return r.packageName.isEmpty() ? TARGET_PKG : r.packageName;
        }
        if (r.presence == AppPresence.OTHER_APP) return r.packageName;
        if (r.presence == AppPresence.SYSTEM_UI) return "系统界面";
        if (r.presence == AppPresence.UNKNOWN_TRANSIENT) return isCachedTarget() ? cachedTargetPkg + " (缓存)" : "检测中…";
        return "无障碍未连接";
    }

    private static boolean isAssistantPkg(String assistantPkg, String pkg) {
        return pkg != null && !pkg.isEmpty() && pkg.equals(assistantPkg);
    }

    private static boolean isTargetPkg(String pkg) {
        return pkg != null && (pkg.contains(TARGET_PKG) || pkg.toLowerCase().contains("karaoke"));
    }

    private static boolean isSystemUi(String pkg) {
        if (pkg == null) return false;
        String p = pkg.toLowerCase();
        return p.contains("systemui") || p.contains("inputmethod") || p.contains("permissioncontroller");
    }

    private static void cacheTarget(String pkg) {
        cachedTargetPkg = pkg;
        cachedTargetAt = System.currentTimeMillis();
    }

    private static boolean isCachedTarget() {
        return !cachedTargetPkg.isEmpty() && System.currentTimeMillis() - cachedTargetAt <= TARGET_CACHE_MS;
    }

    private static class WindowPick {
        final String topPkg;
        final String underlying;
        final String source;
        final boolean assistantOverlay;

        WindowPick(String topPkg, String underlying, String source, boolean assistantOverlay) {
            this.topPkg = topPkg == null ? "" : topPkg;
            this.underlying = underlying == null ? "" : underlying;
            this.source = source == null ? "" : source;
            this.assistantOverlay = assistantOverlay;
        }
    }

    private static WindowPick pickFromWindows(String assistantPkg) {
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        if (svc == null || android.os.Build.VERSION.SDK_INT < 21) return null;
        try {
            List<AccessibilityWindowInfo> wins = svc.getWindows();
            if (wins == null || wins.isEmpty()) return null;
            String activePkg = "";
            String focusedPkg = "";
            String targetUnder = "";
            boolean overlay = false;
            for (AccessibilityWindowInfo w : wins) {
                if (w == null) continue;
                AccessibilityNodeInfo root = w.getRoot();
                if (root == null) continue;
                try {
                    String pkg = root.getPackageName() == null ? "" : root.getPackageName().toString();
                    if (pkg.isEmpty()) continue;
                    if (w.isActive()) activePkg = pkg;
                    if (w.isFocused()) focusedPkg = pkg;
                    if (isAssistantPkg(assistantPkg, pkg)) {
                        overlay = true;
                        continue;
                    }
                    if (isTargetPkg(pkg)) targetUnder = pkg;
                } finally {
                    root.recycle();
                }
            }
            String top = !focusedPkg.isEmpty() ? focusedPkg : activePkg;
            if (isAssistantPkg(assistantPkg, top) && !targetUnder.isEmpty()) {
                return new WindowPick(top, targetUnder, "windows", true);
            }
            if (!top.isEmpty()) {
                return new WindowPick(top, targetUnder, "windows", overlay && isTargetPkg(targetUnder));
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
