package com.zzy.ksongfloat.accessibility;

import android.view.accessibility.AccessibilityEvent;

public final class AccessibilityEventThrottler {
    private static final long MIN_SCAN_INTERVAL_MS = 900L;
    private volatile long lastScanAt;

    public boolean shouldProcess(int eventType) {
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (now - lastScanAt < MIN_SCAN_INTERVAL_MS) return false;
        }
        lastScanAt = now;
        return true;
    }

    public void forceNext() {
        lastScanAt = 0;
    }
}
