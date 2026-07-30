package com.zzy.ksongfloat.runtime;

import com.zzy.ksongfloat.accessibility.AccessibilityConnectionState;
import com.zzy.ksongfloat.classifier.PageType;

public class AssistantRuntimeSnapshot {
    public final AssistantRuntimeState assistantState;
    public final AccessibilityConnectionState accessibilityState;
    public final boolean serviceRunning;
    public final String currentPackageName;
    public final String targetPackageName;
    public final boolean targetAppDetected;
    public final PageType pageType;
    public final double pageConfidence;
    public final String pageTitle;
    public final String detectedNickname;
    public final String detectedSongTitles;
    public final boolean aiConfigured;
    public final String aiRequestState;
    public final String ocrState;
    public final long lastAnalysisTime;
    public final String lastErrorCode;
    public final String lastErrorMessage;
    public final long updatedAt;

    public AssistantRuntimeSnapshot(AssistantRuntimeState assistantState, AccessibilityConnectionState accessibilityState, boolean serviceRunning, String currentPackageName, String targetPackageName, boolean targetAppDetected, PageType pageType, double pageConfidence, String pageTitle, String detectedNickname, String detectedSongTitles, boolean aiConfigured, String aiRequestState, String ocrState, long lastAnalysisTime, String lastErrorCode, String lastErrorMessage, long updatedAt) {
        this.assistantState = assistantState == null ? AssistantRuntimeState.STOPPED : assistantState;
        this.accessibilityState = accessibilityState == null ? AccessibilityConnectionState.UNKNOWN : accessibilityState;
        this.serviceRunning = serviceRunning;
        this.currentPackageName = n(currentPackageName);
        this.targetPackageName = n(targetPackageName);
        this.targetAppDetected = targetAppDetected;
        this.pageType = pageType == null ? PageType.UNKNOWN : pageType;
        this.pageConfidence = pageConfidence;
        this.pageTitle = n(pageTitle);
        this.detectedNickname = n(detectedNickname);
        this.detectedSongTitles = n(detectedSongTitles);
        this.aiConfigured = aiConfigured;
        this.aiRequestState = n(aiRequestState);
        this.ocrState = n(ocrState);
        this.lastAnalysisTime = lastAnalysisTime;
        this.lastErrorCode = n(lastErrorCode);
        this.lastErrorMessage = n(lastErrorMessage);
        this.updatedAt = updatedAt;
    }

    public static AssistantRuntimeSnapshot initial() {
        return new AssistantRuntimeSnapshot(AssistantRuntimeState.STOPPED, AccessibilityConnectionState.UNKNOWN, false, "", "", false, PageType.UNKNOWN, 0, "", "", "", false, "", "", 0, "", "", System.currentTimeMillis());
    }

    public AssistantRuntimeSnapshot withState(AssistantRuntimeState state) {
        return new AssistantRuntimeSnapshot(state, accessibilityState, serviceRunning, currentPackageName, targetPackageName, targetAppDetected, pageType, pageConfidence, pageTitle, detectedNickname, detectedSongTitles, aiConfigured, aiRequestState, ocrState, lastAnalysisTime, lastErrorCode, lastErrorMessage, System.currentTimeMillis());
    }

    private static String n(String s) { return s == null ? "" : s; }
}
