package com.zzy.ksongfloat.ui.components;

import android.content.Context;
import android.widget.LinearLayout;

import com.zzy.ksongfloat.Rounded;

public class IosCardView extends LinearLayout {
    public IosCardView(Context c) {
        super(c);
        setOrientation(VERTICAL);
        int p = dp(16);
        setPadding(p, p, p, p);
        setBackground(Rounded.bg(IosTheme.card(c), dp(16), IosTheme.separator(c), 1));
        setElevation(dp(1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        setLayoutParams(lp);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
