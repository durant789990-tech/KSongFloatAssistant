package com.zzy.ksongfloat.feedback;

import android.app.Activity;
import android.graphics.Color;
import android.widget.Toast;

public class AppFeedbackManager {
    public static void success(Activity a, String msg) { Toast.makeText(a, msg, Toast.LENGTH_SHORT).show(); }
    public static void warning(Activity a, String msg) { Toast.makeText(a, msg, Toast.LENGTH_LONG).show(); }
    public static void error(Activity a, String msg) { Toast.makeText(a, msg, Toast.LENGTH_LONG).show(); }
}
