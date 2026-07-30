package com.zzy.ksongfloat.ui.components;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

import com.zzy.ksongfloat.Rounded;

public class StatusBanner extends TextView {
    public static final int INFO = 0, SUCCESS = 1, WARNING = 2, ERROR = 3;

    public StatusBanner(Context c) {
        super(c);
        setTextSize(14);
        setPadding(dp(14), dp(12), dp(14), dp(12));
        setLineSpacing(0, 1.15f);
        setState(INFO, "准备就绪");
    }

    public void setState(int state, String text) {
        setText(text);
        int bg, fg, stroke;
        if (state == SUCCESS) { bg = Color.rgb(229, 255, 239); fg = Color.rgb(20, 100, 55); stroke = Color.rgb(115, 220, 155); }
        else if (state == WARNING) { bg = Color.rgb(255, 248, 230); fg = Color.rgb(150, 85, 0); stroke = Color.rgb(245, 203, 92); }
        else if (state == ERROR) { bg = Color.rgb(255, 235, 235); fg = Color.rgb(175, 45, 45); stroke = Color.rgb(255, 120, 120); }
        else { bg = IosTheme.card(getContext()); fg = IosTheme.secondaryText(getContext()); stroke = IosTheme.separator(getContext()); }
        setTextColor(fg);
        setBackground(Rounded.bg(bg, dp(14), stroke, 1));
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
