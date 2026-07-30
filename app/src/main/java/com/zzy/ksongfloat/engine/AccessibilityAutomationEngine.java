package com.zzy.ksongfloat.engine;

import android.accessibilityservice.AccessibilityService;

import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;
import com.zzy.ksongfloat.automation.GestureController;

public class AccessibilityAutomationEngine implements AutomationEngine {
    private final AccessibilityService service;

    public AccessibilityAutomationEngine(AccessibilityService service) {
        this.service = service;
    }

    @Override
    public boolean isAvailable() {
        return KSongAccessibilityService.isConnected() && KSongAccessibilityService.getInstance() != null;
    }

    @Override
    public EngineType getType() {
        return EngineType.ACCESSIBILITY;
    }

    @Override
    public String statusLabel() {
        return isAvailable() ? "无障碍引擎" : "无障碍未连接";
    }

    @Override
    public ActionResult swipeUp() {
        if (!isAvailable()) return ActionResult.SERVICE_UNAVAILABLE;
        return new GestureController(service).performSwipeUp() ? ActionResult.SUCCESS : ActionResult.FAILED;
    }

    @Override
    public ActionResult swipeDown() {
        if (!isAvailable()) return ActionResult.SERVICE_UNAVAILABLE;
        return new GestureController(service).performSwipeDown() ? ActionResult.SUCCESS : ActionResult.FAILED;
    }

    @Override
    public ActionResult tap(int x, int y) {
        if (!isAvailable()) return ActionResult.SERVICE_UNAVAILABLE;
        return new GestureController(service).performTap(x, y) ? ActionResult.SUCCESS : ActionResult.FAILED;
    }

    @Override
    public ActionResult back() {
        if (!isAvailable()) return ActionResult.SERVICE_UNAVAILABLE;
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                ? ActionResult.SUCCESS : ActionResult.FAILED;
    }

    @Override
    public ActionResult dismissKeyboard() {
        if (!isAvailable()) return ActionResult.SERVICE_UNAVAILABLE;
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                ? ActionResult.SUCCESS : ActionResult.FAILED;
    }

    @Override
    public void cancelPendingActions() {
        // GestureController uses dispatchGesture callbacks; no queue yet
    }
}
