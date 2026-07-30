package com.zzy.ksongfloat.ui.components;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.widget.Button;

import com.zzy.ksongfloat.Rounded;
import com.zzy.ksongfloat.feedback.HapticFeedbackManager;

public class IosPrimaryButton extends Button {
    private boolean loading;
    private String normalText = "";

    public IosPrimaryButton(Context c, String text, int color) {
        super(c);
        normalText = text;
        setText(text);
        setAllCaps(false);
        setTextSize(16);
        setTextColor(Color.WHITE);
        setMinHeight(dp(52));
        setBackground(Rounded.bg(color, dp(14), 0, 0));
        setOnTouchListener((v, e) -> {
            if (!isEnabled()) return false;
            if (e.getAction() == MotionEvent.ACTION_DOWN) animateTo(0.97f, 0.82f);
            else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) animateTo(1f, 1f);
            return false;
        });
    }

    @Override public boolean performClick() {
        HapticFeedbackManager.tick(this);
        return super.performClick();
    }

    public void setLoading(boolean loading, String text) {
        this.loading = loading;
        setEnabled(!loading);
        setText(loading ? text : normalText);
        setAlpha(loading ? 0.72f : 1f);
    }

    public boolean isLoading() { return loading; }

    private void animateTo(float scale, float alpha) {
        AnimatorSet set = new AnimatorSet();
        set.setDuration(120);
        set.playTogether(ObjectAnimator.ofFloat(this, "scaleX", scale), ObjectAnimator.ofFloat(this, "scaleY", scale), ObjectAnimator.ofFloat(this, "alpha", alpha));
        set.start();
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
