package com.zzy.ksongfloat.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.zzy.ksongfloat.DebugState;
import com.zzy.ksongfloat.automation.AutomationOrchestrator;
import com.zzy.ksongfloat.automation.PageCacheManager;
import com.zzy.ksongfloat.capture.PageTextCollector;
import com.zzy.ksongfloat.capture.PageTextResult;
import com.zzy.ksongfloat.classifier.PageClassificationResult;
import com.zzy.ksongfloat.classifier.PageDetector;
import com.zzy.ksongfloat.config.TargetAppConfig;
import com.zzy.ksongfloat.floating.FloatingWindowService;
import com.zzy.ksongfloat.runtime.AssistantStateCoordinator;
import com.zzy.ksongfloat.runtime.ForegroundAppDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class KSongAccessibilityService extends AccessibilityService {
    public static final String KSONG_PACKAGE_HINT_1 = "com.tencent.karaoke";
    public static final String KSONG_PACKAGE_HINT_2 = "karaoke";

    private static volatile String foregroundPackage = "";
    private static volatile String pageSummary = "无障碍服务未连接";
    private static volatile boolean connected = false;
    private static volatile long pageRevision = 0;
    private static volatile long lastWindowChangeAt = 0;
    private static volatile PageClassificationResult latestClassification;
    private static volatile PageTextResult latestPageText;
    private static volatile long latestClassificationAt = 0;

    private final AccessibilityTextExtractor extractor = new AccessibilityTextExtractor();
    private final PageDetector pageDetector = new PageDetector();
    private final AccessibilityEventThrottler throttler = new AccessibilityEventThrottler();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "a11y-worker");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final AtomicLong pendingRevision = new AtomicLong(0);

    private static volatile KSongAccessibilityService instance;
    private volatile String pendingClassName = "";

    @Override
    protected void onServiceConnected() {
        instance = this;
        connected = true;
        pageSummary = "无障碍服务已连接，请打开目标 K 歌页面";
        AccessibilityStateRepository.connected();
        ensureGestureCapability();
        throttler.forceNext();
        scheduleRefresh("service_connected", true);
        AssistantStateCoordinator.recompute(this, FloatingWindowService.isRunning());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int type = event.getEventType();
        if (!throttler.shouldProcess(type)) return;

        String pkg = event.getPackageName() == null ? "" : event.getPackageName().toString();
        pendingClassName = event.getClassName() == null ? "" : event.getClassName().toString();
        if (!pkg.equals(getPackageName())) {
            foregroundPackage = pkg;
            ForegroundAppDetector.onAccessibilityEvent(pkg, pendingClassName, type, event.getEventTime(), event.getWindowId());
        } else {
            ForegroundAppDetector.onAccessibilityEvent(foregroundPackage, pendingClassName, type, event.getEventTime(), event.getWindowId());
        }
        AccessibilityStateRepository.event(foregroundPackage);

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastWindowChangeAt = System.currentTimeMillis();
            pageRevision++;
            latestClassificationAt = 0;
            latestClassification = null;
            latestPageText = null;
            PageCacheManager.get().invalidate("a11y_window_changed");
            if (AutomationOrchestrator.get().isRunning()) {
                AutomationOrchestrator.get().notifyPageChanged(pageRevision);
            }
        }
        scheduleRefresh(pendingClassName, type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void scheduleRefresh(String className, boolean force) {
        if (force) throttler.forceNext();
        pendingRevision.incrementAndGet();
        if (!processing.compareAndSet(false, true)) return;
        final long rev = pendingRevision.get();
        worker.execute(() -> {
            try {
                processWindow(className, rev);
            } finally {
                processing.set(false);
                if (pendingRevision.get() != rev) scheduleRefresh(pendingClassName, false);
            }
        });
    }

    private void processWindow(String className, long rev) {
        AccessibilityNodeInfo root = null;
        try {
            root = obtainRootSnapshot();
            boolean ksong = TargetAppConfig.matches(this, foregroundPackage);
            AccessibilitySnapshot snapshot = extractor.extract(foregroundPackage, className, root, ksong);
            AccessibilitySnapshotRepository.get().update(snapshot);
            if (!ksong) {
                pageSummary = foregroundPackage + "\n不是目标 K 歌页面";
                postRecompute();
                return;
            }
            PageTextResult page = new PageTextCollector().collect(snapshot, null, 0, true);
            PageClassificationResult cls = pageDetector.detect(page);
            latestPageText = page;
            latestClassification = cls;
            latestClassificationAt = System.currentTimeMillis();
            DebugState.update(page, cls, true);
            pageSummary = foregroundPackage + "\n" + join(snapshot.allVisibleTexts, 220);
            postRecompute();
        } catch (Exception e) {
            pageSummary = "无障碍读取失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            recycle(root);
        }
    }

    private AccessibilityNodeInfo obtainRootSnapshot() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) return AccessibilityNodeInfo.obtain(root);
        if (Build.VERSION.SDK_INT >= 21) {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo window : windows) {
                    if (window == null) continue;
                    try {
                        if (window.isActive() || window.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                            AccessibilityNodeInfo r = window.getRoot();
                            if (r != null) return AccessibilityNodeInfo.obtain(r);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }

    private void postRecompute() {
        mainHandler.post(() -> AssistantStateCoordinator.recompute(this, FloatingWindowService.isRunning()));
    }

    private void ensureGestureCapability() {
        try {
            AccessibilityServiceInfo info = getServiceInfo();
            if (info == null) return;
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            setServiceInfo(info);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterrupt() {
        connected = false;
        pageSummary = "无障碍服务已中断";
        AccessibilityStateRepository.interrupted("无障碍服务已被系统中断");
        AssistantStateCoordinator.recompute(this, FloatingWindowService.isRunning());
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        connected = false;
        worker.shutdownNow();
        AccessibilityStateRepository.destroyed();
        AccessibilitySnapshotRepository.get().clear();
        AssistantStateCoordinator.recompute(this, FloatingWindowService.isRunning());
        super.onDestroy();
    }

    public static boolean isConnected() {
        return connected;
    }

    public static String getForegroundPackage() {
        return foregroundPackage == null ? "" : foregroundPackage;
    }

    public static boolean isKSongForeground() {
        return isKSongPackage(getForegroundPackage());
    }

    public static boolean isKSongPackage(String p) {
        String s = p == null ? "" : p.toLowerCase();
        return s.contains(KSONG_PACKAGE_HINT_1) || s.contains(KSONG_PACKAGE_HINT_2);
    }

    public static String getPageSummary() {
        return pageSummary == null || pageSummary.length() == 0 ? "暂无信息" : pageSummary;
    }

    public static long getPageRevision() {
        return pageRevision;
    }

    public static long getLastWindowChangeAt() {
        return lastWindowChangeAt;
    }

    public static PageClassificationResult getLatestClassification() {
        return latestClassification;
    }

    public static PageTextResult getLatestPageText() {
        return latestPageText;
    }

    public static long getLatestClassificationAt() {
        return latestClassificationAt;
    }

    public static String canFillText(android.content.Context c, TextFillRequest r) {
        KSongAccessibilityService s = instance;
        if (s == null) return "无障碍服务未连接";
        return new TextInputController(s).canFill(c, r);
    }

    public static boolean fillText(TextFillRequest r) {
        KSongAccessibilityService s = instance;
        return s != null && new TextInputController(s).fill(r);
    }

    public static KSongAccessibilityService getInstance() {
        return instance;
    }

    public AccessibilityNodeInfo getRootInActiveWindowSafe() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) return root;
        return obtainRootSnapshot();
    }

    public boolean fillFocusedEditable(String text) {
        if (!isKSongForeground()) return false;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo focused = null;
        try {
            focused = root == null ? null : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focused == null || !focused.isEditable()) return false;
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        } finally {
            recycle(focused);
            recycle(root);
        }
    }

    private static void recycle(AccessibilityNodeInfo n) {
        try {
            if (n != null) n.recycle();
        } catch (Exception ignored) {
        }
    }

    private static String join(List<String> texts, int maxLen) {
        StringBuilder sb = new StringBuilder();
        if (texts != null) for (String t : texts) {
            if (sb.length() + t.length() + 3 > maxLen) break;
            if (sb.length() > 0) sb.append(" | ");
            sb.append(t);
        }
        return sb.toString();
    }
}
