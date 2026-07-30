package com.zzy.ksongfloat.engine;

import android.content.Context;

import com.zzy.ksongfloat.shizuku.ShizukuHelper;

/**
 * Shizuku 可选增强引擎。不可用时由 Selector 降级到无障碍。
 */
public class ShizukuAutomationEngine implements AutomationEngine {
    private final Context context;

    public ShizukuAutomationEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public EngineType getType() {
        return EngineType.SHIZUKU;
    }

    @Override
    public String statusLabel() {
        return ShizukuHelper.statusLabel(context);
    }

    @Override
    public ActionResult swipeUp() {
        return ShizukuHelper.testSwipeUp(context);
    }

    @Override
    public ActionResult swipeDown() {
        return ActionResult.FAILED;
    }

    @Override
    public ActionResult tap(int x, int y) {
        return ActionResult.CANCELLED;
    }

    @Override
    public ActionResult back() {
        return ShizukuHelper.testBack(context);
    }

    @Override
    public ActionResult dismissKeyboard() {
        return back();
    }

    @Override
    public void cancelPendingActions() {
        ShizukuHelper.cancelPending();
    }
}
