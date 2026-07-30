package com.zzy.ksongfloat.automation;

public class AutomationSettings {
    public boolean enableFollow = false;
    public boolean enableComment = false;
    public boolean enablePrivateMessage = false;
    public boolean enableCommentDraft = true;
    public boolean enablePrivateMessageDraft = true;
    public boolean autoSendComment = false;
    public boolean autoSendPrivateMessage = false;
    public boolean autoSend = false;
    public boolean testMode = true;
    public boolean pauseOnLeaveKaraoke = true;
    public boolean autoResumeOnReturn = false;
    public boolean analyzeBeforeSwipe = true;
    public int delayMinMs = 2000;
    public int delayMaxMs = 5000;
    public int maxUsersPerSession = 10;
    public int maxUsersPerTask = 20;
    public int consecutiveFailStop = 3;
    public int scrollAttemptsBeforeSkip = 3;
    public int maxTaskDurationMinutes = 120;
    public int duplicateFilterMinutes = 60;

    public int delayMinSec() { return Math.max(1, delayMinMs / 1000); }
    public int delayMaxSec() { return Math.max(delayMinSec(), delayMaxMs / 1000); }

    public void setDelaySeconds(int minSec, int maxSec) {
        delayMinMs = Math.max(1000, minSec * 1000);
        delayMaxMs = Math.max(delayMinMs, maxSec * 1000);
    }

    public String enabledActionsSummary() {
        StringBuilder sb = new StringBuilder();
        if (enableCommentDraft) sb.append("评论草稿 ");
        if (enablePrivateMessageDraft) sb.append("私信草稿 ");
        if (autoSendComment) sb.append("自动发评论 ");
        if (autoSendPrivateMessage) sb.append("自动发私信 ");
        if (testMode) sb.append("[测试模式]");
        return sb.length() == 0 ? "仅扫描" : sb.toString().trim();
    }
}
