package com.zzy.ksongfloat.automation;

import java.util.UUID;

/** 单次用户主动启动的自动化会话。 */
public final class AutomationSession {
    public enum State {
        IDLE, PRECHECK, RUNNING, PAUSED, STOPPED, ERROR
    }

    public final String sessionId;
    public final long startTime;
    public volatile State state = State.PRECHECK;
    public volatile boolean cancelled;
    public volatile String currentPackage = "";
    public volatile String currentPage = "";
    public volatile int currentWindowId = -1;
    public volatile int currentUserIndex;
    public volatile int processedUserCount;
    public volatile int failedUserCount;
    public volatile int continuousFailureCount;
    public volatile int actionCount;
    public volatile String lastAction = "";
    public volatile String lastActionResult = "";
    public volatile String lastAiPlan = "";
    public volatile String currentUserName = "";
    public volatile int queueSize;
    public volatile int unknownStreak;
    public volatile int rootEmptyRetries;
    public volatile int emptySwipeStreak;

    AutomationSession(String sessionId) {
        this.sessionId = sessionId;
        this.startTime = System.currentTimeMillis();
    }

    static AutomationSession createNew() {
        return new AutomationSession(UUID.randomUUID().toString());
    }
}
