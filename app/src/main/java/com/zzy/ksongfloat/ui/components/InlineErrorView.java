package com.zzy.ksongfloat.ui.components;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

public class InlineErrorView extends TextView {
    public InlineErrorView(Context c) {
        super(c);
        setTextSize(13);
        setTextColor(Color.rgb(210, 55, 55));
        setVisibility(GONE);
    }

    public void show(String msg) {
        setText(msg == null ? "" : msg);
        setVisibility(msg == null || msg.length() == 0 ? GONE : VISIBLE);
    }
}
