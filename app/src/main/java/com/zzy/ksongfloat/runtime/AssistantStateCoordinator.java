package com.zzy.ksongfloat.runtime;

import android.content.Context;

import com.zzy.ksongfloat.DebugState;
import com.zzy.ksongfloat.accessibility.AccessibilityConnectionState;
import com.zzy.ksongfloat.accessibility.AccessibilityStateDetector;
import com.zzy.ksongfloat.ai.AiConfigRepository;
import com.zzy.ksongfloat.ai.AiDebugState;
import com.zzy.ksongfloat.classifier.PageClassificationResult;
import com.zzy.ksongfloat.classifier.PageType;
import com.zzy.ksongfloat.config.TargetAppConfig;

public class AssistantStateCoordinator {
    public static void serviceStarted(Context context) { recompute(context, true, null, null); }
    public static void serviceStopped(Context context) { recompute(context, false, AssistantRuntimeState.STOPPED, null); }
    public static void error(Context context, String message) { recompute(context, true, AssistantRuntimeState.ERROR, message); }
    public static void recompute(Context context, boolean serviceRunning) { recompute(context, serviceRunning, null, null); }

    public static void recompute(Context context, boolean serviceRunning, AssistantRuntimeState forcedState, String error) {
        if (context == null) return;
        AccessibilityConnectionState acc = AccessibilityStateDetector.detect(context);
        ForegroundAppResolver.Result fr = ForegroundAppResolver.resolve(context);
        String current = fr.packageName.isEmpty() ? fr.underlyingPackage : fr.packageName;
        if (current.isEmpty()) current = ForegroundAppResolver.displayPackage(context);
        String target = TargetAppConfig.getTargetPackage(context);
        boolean targetDetected = fr.presence == ForegroundAppResolver.AppPresence.TARGET_APP
                || fr.presence == ForegroundAppResolver.AppPresence.ASSISTANT_OVERLAY;
        PageClassificationResult cls = DebugState.cls();
        PageType type = cls == null ? PageType.UNKNOWN : cls.pageType;
        double confidence = cls == null ? 0 : cls.confidence;
        String nickname = cls == null ? "" : cls.detectedNickname;
        String songs = cls == null ? "" : cls.detectedSongTitles.toString();
        boolean aiConfigured = AiConfigRepository.get().isConfigured();
        AssistantRuntimeState state = forcedState;
        if (state == null) {
            if (!serviceRunning) state = AssistantRuntimeState.STOPPED;
            else if (acc != AccessibilityConnectionState.CONNECTED) state = AssistantRuntimeState.WAITING_FOR_ACCESSIBILITY;
            else if (!targetDetected) state = AssistantRuntimeState.WAITING_FOR_TARGET_APP;
            else if (type != PageType.UNKNOWN && confidence >= 0.6) state = AssistantRuntimeState.READY_TO_ANALYZE;
            else state = AssistantRuntimeState.TARGET_APP_DETECTED;
        }
        AssistantStateRepository.update(new AssistantRuntimeSnapshot(state, acc, serviceRunning, current, target, targetDetected, type, confidence, "", nickname, songs, aiConfigured, AiDebugState.lastState, "", DebugState.lastAnalyzedAt(), error == null ? "" : "ERROR", error == null ? "" : error, System.currentTimeMillis()));
    }
}
