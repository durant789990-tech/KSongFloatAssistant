package com.zzy.ksongfloat.runtime;

public class ForegroundAppDetector {
    private static volatile String currentPackageName = "";
    private static volatile String currentClassName = "";
    private static volatile int lastEventType;
    private static volatile long lastEventTime;
    private static volatile int lastWindowId = -1;

    public static void onAccessibilityEvent(String packageName, String className, int eventType, long eventTime, int windowId) {
        currentPackageName = packageName == null ? "" : packageName;
        currentClassName = className == null ? "" : className;
        lastEventType = eventType;
        lastEventTime = eventTime > 0 ? eventTime : System.currentTimeMillis();
        lastWindowId = windowId;
    }

    public static String currentPackageName() { return currentPackageName == null ? "" : currentPackageName; }
    public static String currentClassName() { return currentClassName == null ? "" : currentClassName; }
    public static int lastEventType() { return lastEventType; }
    public static long lastEventTime() { return lastEventTime; }
    public static int lastWindowId() { return lastWindowId; }
}
