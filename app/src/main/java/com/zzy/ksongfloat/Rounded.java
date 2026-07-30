package com.zzy.ksongfloat;

import android.graphics.drawable.GradientDrawable;

public class Rounded {
    public static GradientDrawable bg(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        if (strokeWidth > 0) g.setStroke(strokeWidth, strokeColor);
        return g;
    }
}