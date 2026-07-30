package com.zzy.ksongfloat.automation;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * 自动化状态唯一发布源，带 sessionId 与 stateVersion。
 */
public final class AutomationStateRepository {
    public static class Snapshot {
        public final String sessionId;
        public final long stateVersion;
        public final AutomationSession.State state;
        public final String message;
        public final String foregroundPackage;
        public final String pageType;
        public final String lastAction;
        public final String lastAiPlan;
        public final long updatedAt;

        Snapshot(String sessionId, long stateVersion, AutomationSession.State state, String message,
                 String foregroundPackage, String pageType, String lastAction, String lastAiPlan) {
            this.sessionId = sessionId == null ? "" : sessionId;
            this.stateVersion = stateVersion;
            this.state = state == null ? AutomationSession.State.IDLE : state;
            this.message = message == null ? "" : message;
            this.foregroundPackage = foregroundPackage == null ? "" : foregroundPackage;
            this.pageType = pageType == null ? "" : pageType;
            this.lastAction = lastAction == null ? "" : lastAction;
            this.lastAiPlan = lastAiPlan == null ? "" : lastAiPlan;
            this.updatedAt = System.currentTimeMillis();
        }
    }

    private static final AutomationStateRepository INSTANCE = new AutomationStateRepository();
    private final MutableLiveData<Snapshot> live = new MutableLiveData<>(empty());
    private volatile Snapshot cached = empty();

    public static AutomationStateRepository get() {
        return INSTANCE;
    }

    public LiveData<Snapshot> observe() {
        return live;
    }

    public Snapshot currentSnapshot() {
        return cached;
    }

    public void update(AutomationSession session, long version, AutomationSession.State state, String message) {
        String sid = session == null ? "" : session.sessionId;
        String pkg = session == null ? "" : session.currentPackage;
        String page = session == null ? "" : session.currentPage;
        String action = session == null ? "" : session.lastAction;
        String plan = session == null ? "" : session.lastAiPlan;
        cached = new Snapshot(sid, version, state, message, pkg, page, action, plan);
        live.postValue(cached);
    }

    private static Snapshot empty() {
        return new Snapshot("", 0, AutomationSession.State.STOPPED, "未运行", "", "", "", "");
    }
}
