package com.zzy.ksongfloat.floating;

import android.content.Context;

public class FloatingPositionRepository {
    private static final String PREF = "floating_position";
    public static int x(Context c, int fallback) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("x", fallback); }
    public static int y(Context c, int fallback) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("y", fallback); }
    public static void save(Context c, int x, int y) { c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("x", x).putInt("y", y).apply(); }
}
