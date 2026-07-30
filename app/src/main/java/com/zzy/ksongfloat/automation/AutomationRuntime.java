package com.zzy.ksongfloat.automation;

import com.zzy.ksongfloat.classifier.PageType;

public final class AutomationRuntime {
    public enum UiStatus { IDLE, RUNNING, PAUSED, ERROR }

    private static volatile UiStatus status = UiStatus.IDLE;
    private static volatile String lastAction = "暂无";
    private static volatile String pauseReason = "";
    private static volatile String currentPage = "UNKNOWN";
    private static volatile int processedCount = 0;
    private static volatile int consecutiveUnknown = 0;
    private static volatile String floatMessage = "";

    private static volatile int consecutiveFail = 0;
    private static volatile String foregroundPackage = "";
    private static volatile String currentEngine = "无障碍";
    private static volatile Listener changeListener;

    public interface Listener {
        void onRuntimeChanged();
    }

    public static void setChangeListener(Listener l) {
        changeListener = l;
    }

    private static void notifyChanged() {
        Listener l = changeListener;
        if (l != null) l.onRuntimeChanged();
    }

    public static UiStatus getStatus() { return status; }
    public static String getLastAction() { return lastAction; }
    public static String getPauseReason() { return pauseReason; }
    public static String getCurrentPage() { return currentPage; }
    public static int getProcessedCount() { return processedCount; }
    public static int getConsecutiveUnknown() { return consecutiveUnknown; }
    public static int getConsecutiveFail() { return consecutiveFail; }
    public static String getFloatMessage() { return floatMessage; }
    public static String getForegroundPackage() { return foregroundPackage; }
    public static String getCurrentEngine() { return currentEngine; }

    public static void onStart() {
        status = UiStatus.RUNNING;
        pauseReason = "";
        floatMessage = "";
        processedCount = 0;
        consecutiveUnknown = 0;
        consecutiveFail = 0;
        lastAction = "任务启动";
        notifyChanged();
    }

    public static void onStop() {
        status = UiStatus.IDLE;
        lastAction = "任务停止";
        floatMessage = "";
        notifyChanged();
    }

    public static void onPause(String reason) {
        status = UiStatus.PAUSED;
        pauseReason = reason == null ? "" : reason;
        floatMessage = reason;
        lastAction = "暂停：" + pauseReason;
        notifyChanged();
    }

    public static void onError(String reason) {
        status = UiStatus.ERROR;
        pauseReason = reason == null ? "" : reason;
        floatMessage = reason;
        lastAction = "错误：" + pauseReason;
        notifyChanged();
    }

    public static void onResumeRun() {
        status = UiStatus.RUNNING;
        pauseReason = "";
        floatMessage = "";
        notifyChanged();
    }

    public static void setLastAction(String action) {
        lastAction = action == null ? "" : action;
        notifyChanged();
    }

    public static void setCurrentPage(PageType type) {
        currentPage = type == null ? "UNKNOWN" : type.name();
        notifyChanged();
    }

    public static void setProcessedCount(int count) {
        processedCount = count;
        notifyChanged();
    }

    public static void setConsecutiveFail(int count) {
        consecutiveFail = count;
        notifyChanged();
    }

    public static void incrementUnknown() {
        consecutiveUnknown++;
        notifyChanged();
    }

    public static void resetUnknown() {
        consecutiveUnknown = 0;
        notifyChanged();
    }

    public static void setFloatMessage(String msg) {
        floatMessage = msg == null ? "" : msg;
        notifyChanged();
    }

    public static void setForegroundPackage(String pkg) {
        foregroundPackage = pkg == null ? "" : pkg;
        notifyChanged();
    }

    public static void setCurrentEngine(String engine) {
        currentEngine = engine == null ? "无障碍" : engine;
        notifyChanged();
    }
}
