package com.zzy.ksongfloat.floating;

import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

public class FloatingTouchController implements View.OnTouchListener {
    public interface Callback {
        void onClick();
        void onDrag(int x, int y);
        void onDragEnd(int x, int y);
        void onLongPress();
    }

    public enum Gesture { CLICK, DRAG, LONG_PRESS }

    private final WindowManager.LayoutParams params;
    private final Callback callback;
    private final int touchSlop;
    private final int longPressTimeout;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private float downRawX, downRawY;
    private int startX, startY;
    private long downAt;
    private boolean dragging;
    private boolean longPressed;
    private final Runnable longPressRunnable = new Runnable() {
        @Override public void run() {
            if (!dragging && !longPressed) {
                longPressed = true;
                if (callback != null) callback.onLongPress();
            }
        }
    };

    public FloatingTouchController(View view, WindowManager.LayoutParams params, Callback callback) {
        this.params = params;
        this.callback = callback;
        this.touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        this.longPressTimeout = ViewConfiguration.getLongPressTimeout();
    }

    @Override public boolean onTouch(View v, MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = e.getRawX(); downRawY = e.getRawY();
                startX = params.x; startY = params.y;
                downAt = System.currentTimeMillis();
                dragging = false; longPressed = false;
                handler.postDelayed(longPressRunnable, longPressTimeout);
                return true;
            case MotionEvent.ACTION_MOVE:
                int dx = (int)(e.getRawX() - downRawX);
                int dy = (int)(e.getRawY() - downRawY);
                if (!dragging && distance(dx, dy) > touchSlop) {
                    dragging = true;
                    handler.removeCallbacks(longPressRunnable);
                }
                if (dragging) {
                    params.x = startX + dx;
                    params.y = startY + dy;
                    if (callback != null) callback.onDrag(params.x, params.y);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                if (longPressed) return true;
                if (dragging) {
                    if (callback != null) callback.onDragEnd(params.x, params.y);
                    return true;
                }
                if (classify(e.getRawX() - downRawX, e.getRawY() - downRawY, System.currentTimeMillis() - downAt, touchSlop, longPressTimeout) == Gesture.CLICK) {
                    v.performClick();
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    if (callback != null) callback.onClick();
                }
                return true;
            default:
                return true;
        }
    }

    public static Gesture classify(float dx, float dy, long durationMs, int slop, int longPressMs) {
        if (distance(dx, dy) > slop) return Gesture.DRAG;
        if (durationMs >= longPressMs) return Gesture.LONG_PRESS;
        return Gesture.CLICK;
    }

    private static double distance(float dx, float dy) { return Math.sqrt(dx * dx + dy * dy); }
}
