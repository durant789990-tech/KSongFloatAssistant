package com.zzy.ksongfloat.automation;

public class StateTimeoutHandler {
    public static final long DEFAULT_TIMEOUT_MS = 5000L;

    private AutomationPhase phase = AutomationPhase.IDLE;
    private long enteredAt = System.currentTimeMillis();

    public void enter(AutomationPhase p) {
        if (p != phase) {
            phase = p;
            enteredAt = System.currentTimeMillis();
        }
    }

    public boolean isTimedOut(long timeoutMs) {
        if (phase == AutomationPhase.IDLE
                || phase == AutomationPhase.PAUSED
                || phase == AutomationPhase.STOPPED
                || phase == AutomationPhase.WAITING_NEARBY_LIST
                || phase == AutomationPhase.SCROLLING_LIST
                || phase == AutomationPhase.REQUESTING_AI) {
            return false;
        }
        return System.currentTimeMillis() - enteredAt >= timeoutMs;
    }

    public long elapsedMs() {
        return System.currentTimeMillis() - enteredAt;
    }

    public AutomationPhase getPhase() {
        return phase;
    }

    public void reset() {
        phase = AutomationPhase.IDLE;
        enteredAt = System.currentTimeMillis();
    }
}
