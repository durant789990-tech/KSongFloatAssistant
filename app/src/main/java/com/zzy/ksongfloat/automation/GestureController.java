package com.zzy.ksongfloat.automation;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GestureController {
    private static final Random RND = new Random();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private final AccessibilityService service;

    public GestureController(AccessibilityService service) {
        this.service = service;
    }

    /**
     * 从屏幕 50%/80% 滑到 50%/20%，持续 300~500ms，用于列表向下滚动。
     */
    public boolean performSwipeUp() {
        if (Build.VERSION.SDK_INT < 24) {
            AutomationLog.warn("系统版本过低，无法执行手势");
            return false;
        }
        DisplayMetrics dm = service.getResources().getDisplayMetrics();
        float x = dm.widthPixels * 0.5f;
        float startY = dm.heightPixels * 0.80f;
        float endY = dm.heightPixels * 0.20f;
        long duration = 300 + RND.nextInt(201);
        return dispatchOnMainThread(x, startY, x, endY, duration, false);
    }

    public boolean swipeUp() {
        return performSwipeUp();
    }

    public boolean performTap(int x, int y) {
        if (Build.VERSION.SDK_INT < 24) return false;
        return dispatchOnMainThread(x, y, x, y, 50, false);
    }

    public boolean performSwipeDown() {
        if (Build.VERSION.SDK_INT < 24) return false;
        DisplayMetrics dm = service.getResources().getDisplayMetrics();
        float x = dm.widthPixels * 0.5f;
        float startY = dm.heightPixels * 0.25f;
        float endY = dm.heightPixels * 0.75f;
        long duration = 320 + RND.nextInt(180);
        return dispatchOnMainThread(x, startY, x, endY, duration, false);
    }

    public boolean swipeDown() {
        return performSwipeDown();
    }

    private boolean dispatchOnMainThread(float x1, float y1, float x2, float y2, long durationMs, boolean curved) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean(false);
        AtomicBoolean dispatched = new AtomicBoolean(false);
        MAIN.post(() -> {
            try {
                Path path = new Path();
                path.moveTo(x1, y1);
                if (curved) {
                    float cx = (x1 + x2) / 2f + jitter(24);
                    float cy = (y1 + y2) / 2f + jitter(16);
                    path.quadTo(cx, cy, x2, y2);
                } else {
                    path.lineTo(x2, y2);
                }
                GestureDescription.StrokeDescription stroke =
                        new GestureDescription.StrokeDescription(path, 0, Math.max(280, durationMs));
                GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
                boolean sent = service.dispatchGesture(gesture, new AccessibilityService.GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        ok.set(true);
                        latch.countDown();
                    }

                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        AutomationLog.warn("手势被取消");
                        latch.countDown();
                    }
                }, null);
                dispatched.set(sent);
                if (!sent) latch.countDown();
            } catch (Exception e) {
                AutomationLog.error("手势派发异常：" + e.getMessage());
                latch.countDown();
            }
        });
        try {
            latch.await(Math.max(1200, durationMs + 800), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (!dispatched.get()) {
            AutomationLog.warn("dispatchGesture 返回 false，请确认无障碍已开启手势权限");
        }
        return ok.get();
    }

    private float jitter(int spread) {
        return RND.nextInt(Math.max(1, spread * 2 + 1)) - spread;
    }
}
