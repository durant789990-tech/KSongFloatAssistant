package com.zzy.ksongfloat.ui.components;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

public class IosTheme {
    public static final int BRAND = Color.rgb(98, 86, 238);
    public static final int DANGER = Color.rgb(255, 59, 48);

    public static boolean dark(Context c) {
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public static int bg(Context c) { return dark(c) ? Color.rgb(0,0,0) : Color.rgb(242,242,247); }
    public static int card(Context c) { return dark(c) ? Color.rgb(28,28,30) : Color.WHITE; }
    public static int primaryText(Context c) { return dark(c) ? Color.WHITE : Color.rgb(18,24,38); }
    public static int secondaryText(Context c) { return dark(c) ? Color.rgb(174,174,178) : Color.rgb(102,112,133); }
    public static int separator(Context c) { return dark(c) ? Color.rgb(55,55,58) : Color.rgb(226,231,242); }
}
