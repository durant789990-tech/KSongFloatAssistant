package com.zzy.ksongfloat.ui.components;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IosNavigationBar extends LinearLayout {
    public Button left;
    public Button right;
    public TextView title;

    public IosNavigationBar(Activity a, String titleText, String rightText) {
        super(a);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(0, dp(4), 0, dp(12));

        left = textButton(a, "‹ 返回");
        title = new TextView(a);
        title.setText(titleText);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(IosTheme.primaryText(a));
        title.setGravity(Gravity.CENTER);
        right = textButton(a, rightText == null ? "" : rightText);

        addView(left, new LinearLayout.LayoutParams(dp(88), dp(44)));
        addView(title, new LinearLayout.LayoutParams(0, dp(44), 1));
        addView(right, new LinearLayout.LayoutParams(dp(88), dp(44)));
    }

    private Button textButton(Activity a, String s) {
        Button b = new Button(a);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(IosTheme.BRAND);
        b.setBackgroundColor(0x00000000);
        return b;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
