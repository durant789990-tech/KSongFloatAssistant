package com.zzy.ksongfloat.feedback;

import android.view.HapticFeedbackConstants;
import android.view.View;

public class HapticFeedbackManager {
    public static void tick(View v) { if (v != null) v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); }
    public static void confirm(View v) { if (v != null) v.performHapticFeedback(HapticFeedbackConstants.CONFIRM); }
    public static void reject(View v) { if (v != null) v.performHapticFeedback(HapticFeedbackConstants.REJECT); }
    public static void danger(View v) { if (v != null) v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); }
}
