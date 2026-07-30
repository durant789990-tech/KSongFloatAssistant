package com.zzy.ksongfloat.engine;

import android.content.Context;

import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;
import com.zzy.ksongfloat.automation.AutomationOrchestrator;

/** 自动化仅使用无障碍引擎，强行模式不做前台 App 拦截。 */
public final class AutomationEngineSelector {
    private static volatile AutomationEngine active;

    public static AutomationEngine getEngine(Context context) {
        AccessibilityAutomationEngine acc = new AccessibilityAutomationEngine(KSongAccessibilityService.getInstance());
        active = acc;
        return acc;
    }

    public static String currentEngineLabel(Context context) {
        return "无障碍";
    }

    public static ActionResult swipeUp(Context context, AutomationOrchestrator orchestrator) {
        return getEngine(context).swipeUp();
    }
}
