package com.zzy.ksongfloat.accessibility;

public class AccessibilityStateRepository {
    private static volatile AccessibilityConnectionState state = AccessibilityConnectionState.UNKNOWN;
    private static volatile long lastConnectedAt;
    private static volatile long lastEventAt;
    private static volatile long lastInterruptedAt;
    private static volatile String lastPackageName = "";
    private static volatile String lastError = "";

    public static void connected() {
        state = AccessibilityConnectionState.CONNECTED;
        lastConnectedAt = System.currentTimeMillis();
        lastError = "";
    }

    public static void event(String packageName) {
        lastEventAt = System.currentTimeMillis();
        lastPackageName = packageName == null ? "" : packageName;
        if (state != AccessibilityConnectionState.CONNECTED) state = AccessibilityConnectionState.CONNECTED;
    }

    public static void interrupted(String message) {
        state = AccessibilityConnectionState.INTERRUPTED;
        lastInterruptedAt = System.currentTimeMillis();
        lastError = message == null ? "无障碍服务已被系统中断" : message;
    }

    public static void destroyed() {
        state = AccessibilityConnectionState.ENABLED_NOT_CONNECTED;
    }

    public static void setState(AccessibilityConnectionState s) {
        state = s == null ? AccessibilityConnectionState.UNKNOWN : s;
    }

    public static AccessibilityConnectionState state() { return state; }
    public static long lastConnectedAt() { return lastConnectedAt; }
    public static long lastEventAt() { return lastEventAt; }
    public static long lastInterruptedAt() { return lastInterruptedAt; }
    public static String lastPackageName() { return lastPackageName == null ? "" : lastPackageName; }
    public static String lastError() { return lastError == null ? "" : lastError; }

    public static String displayText() {
        switch (state) {
            case DISABLED: return "无障碍服务未开启";
            case ENABLED_NOT_CONNECTED: return "已授权，但服务尚未连接";
            case CONNECTED: return "无障碍服务已连接";
            case INTERRUPTED: return "无障碍服务被系统中断，请重新开启";
            default: return "无障碍状态未知";
        }
    }
}
