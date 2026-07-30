package com.zzy.ksongfloat.engine;

import android.view.accessibility.AccessibilityNodeInfo;

public interface AutomationEngine {
    boolean isAvailable();
    EngineType getType();
    String statusLabel();
    ActionResult swipeUp();
    ActionResult swipeDown();
    ActionResult tap(int x, int y);
    ActionResult back();
    ActionResult dismissKeyboard();
    void cancelPendingActions();
}
