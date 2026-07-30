package com.zzy.ksongfloat.automation;

import android.content.Context;
import android.content.SharedPreferences;

public class AutomationSettingsRepository {
    private static final String PREF = "automation_settings";
    private static volatile Listener listener;

    public interface Listener {
        void onSettingsChanged(AutomationSettings settings);
    }

    public static void setListener(Listener l) {
        listener = l;
    }

    public static AutomationSettings load(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        AutomationSettings s = new AutomationSettings();
        s.enableFollow = false;
        s.enableComment = p.getBoolean("enableComment", true);
        s.enablePrivateMessage = p.getBoolean("enablePrivateMessage", true);
        s.enableCommentDraft = p.getBoolean("enableCommentDraft", true);
        s.enablePrivateMessageDraft = p.getBoolean("enablePrivateMessageDraft", true);
        s.autoSendComment = p.getBoolean("autoSendComment", true);
        s.autoSendPrivateMessage = p.getBoolean("autoSendPrivateMessage", true);
        s.autoSend = p.getBoolean("autoSend", true);
        s.testMode = p.getBoolean("testMode", false);
        s.pauseOnLeaveKaraoke = p.getBoolean("pauseOnLeave", true);
        s.autoResumeOnReturn = false;
        s.analyzeBeforeSwipe = p.getBoolean("analyzeBeforeSwipe", true);
        s.delayMinMs = p.getInt("delayMinMs", 2000);
        s.delayMaxMs = p.getInt("delayMaxMs", 5000);
        s.maxUsersPerSession = p.getInt("maxUsersPerSession", 10);
        s.maxUsersPerTask = p.getInt("maxUsersPerTask", 20);
        s.consecutiveFailStop = p.getInt("consecutiveFailStop", 3);
        s.scrollAttemptsBeforeSkip = p.getInt("scrollAttemptsBeforeSkip", 3);
        s.maxTaskDurationMinutes = p.getInt("maxTaskDurationMinutes", 120);
        s.duplicateFilterMinutes = p.getInt("duplicateFilterMinutes", 60);
        return s;
    }

    public static void save(Context c, AutomationSettings s) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putBoolean("enableComment", s.enableComment)
                .putBoolean("enablePrivateMessage", s.enablePrivateMessage)
                .putBoolean("enableCommentDraft", s.enableCommentDraft)
                .putBoolean("enablePrivateMessageDraft", s.enablePrivateMessageDraft)
                .putBoolean("autoSendComment", s.autoSendComment)
                .putBoolean("autoSendPrivateMessage", s.autoSendPrivateMessage)
                .putBoolean("autoSend", s.autoSend)
                .putBoolean("testMode", s.testMode)
                .putBoolean("pauseOnLeave", s.pauseOnLeaveKaraoke)
                .putBoolean("autoResume", false)
                .putBoolean("analyzeBeforeSwipe", s.analyzeBeforeSwipe)
                .putInt("delayMinMs", s.delayMinMs)
                .putInt("delayMaxMs", s.delayMaxMs)
                .putInt("maxUsersPerSession", s.maxUsersPerSession)
                .putInt("maxUsersPerTask", s.maxUsersPerTask)
                .putInt("consecutiveFailStop", s.consecutiveFailStop)
                .putInt("scrollAttemptsBeforeSkip", s.scrollAttemptsBeforeSkip)
                .putInt("maxTaskDurationMinutes", s.maxTaskDurationMinutes)
                .putInt("duplicateFilterMinutes", s.duplicateFilterMinutes)
                .apply();
        Listener l = listener;
        if (l != null) l.onSettingsChanged(s);
    }
}
