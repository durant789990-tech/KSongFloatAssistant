package com.zzy.ksongfloat.automation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.zzy.ksongfloat.ai.AiClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 会话生命周期与 emergencyStop 唯一入口。
 */
public final class AutomationSessionManager {
    public static class StateEvent {
        public final String sessionId;
        public final long stateVersion;
        public final long timestamp;
        public final AutomationSession.State state;
        public final String message;

        StateEvent(String sessionId, long stateVersion, AutomationSession.State state, String message) {
            this.sessionId = sessionId == null ? "" : sessionId;
            this.stateVersion = stateVersion;
            this.timestamp = System.currentTimeMillis();
            this.state = state;
            this.message = message == null ? "" : message;
        }
    }

    private static final AutomationSessionManager INSTANCE = new AutomationSessionManager();
    private final AtomicLong stateVersion = new AtomicLong(0);
    private final MutableLiveData<StateEvent> live = new MutableLiveData<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile AutomationSession current;
    private volatile AiClient aiClient;
    private volatile Future<?> aiFuture;
    private volatile ExecutorService executor;
    private volatile Thread workerThread;

    public static AutomationSessionManager get() {
        return INSTANCE;
    }

    public LiveData<StateEvent> observe() {
        return live;
    }

    public AutomationSession current() {
        return current;
    }

    public long nextStateVersion() {
        return stateVersion.incrementAndGet();
    }

    public long stateVersion() {
        return stateVersion.get();
    }

    /** App 冷启动时必须 STOPPED，不恢复历史任务。 */
    public void resetOnAppStart() {
        current = null;
        stateVersion.incrementAndGet();
        publish(null, AutomationSession.State.STOPPED, "应用已启动，自动化未运行");
    }

    public AutomationSession beginSession(Context context) {
        emergencyStopInternal("新会话替换旧会话", false);
        AutomationSession session = AutomationSession.createNew();
        session.state = AutomationSession.State.PRECHECK;
        current = session;
        aiClient = new AiClient();
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "auto-session-exec");
            t.setDaemon(true);
            return t;
        });
        stateVersion.incrementAndGet();
        publish(session, AutomationSession.State.PRECHECK, "正在预检查");
        AutomationLog.info("SESSION_START id=" + session.sessionId);
        return session;
    }

    public boolean isActive(String sessionId) {
        AutomationSession s = current;
        return s != null
                && !s.cancelled
                && s.state == AutomationSession.State.RUNNING
                && s.sessionId.equals(sessionId);
    }

    public boolean isValid(String sessionId) {
        AutomationSession s = current;
        return s != null && !s.cancelled && s.sessionId.equals(sessionId)
                && s.state != AutomationSession.State.STOPPED
                && s.state != AutomationSession.State.ERROR;
    }

    public void markRunning(AutomationSession session) {
        if (session == null) return;
        session.state = AutomationSession.State.RUNNING;
        publish(session, AutomationSession.State.RUNNING, "运行中");
    }

    public void bindWorker(Thread t) {
        workerThread = t;
    }

    public void bindAiFuture(Future<?> f) {
        aiFuture = f;
    }

    public AiClient aiClient() {
        return aiClient;
    }

    public void emergencyStop(String reason) {
        emergencyStopInternal(reason, true);
    }

    private void emergencyStopInternal(String reason, boolean publish) {
        AutomationSession s = current;
        if (s != null) {
            s.cancelled = true;
            s.state = AutomationSession.State.STOPPED;
        }
        mainHandler.removeCallbacksAndMessages(null);
        try {
            if (aiFuture != null) aiFuture.cancel(true);
        } catch (Exception ignored) {
        }
        try {
            if (aiClient != null) aiClient.cancel();
        } catch (Exception ignored) {
        }
        try {
            if (executor != null) executor.shutdownNow();
        } catch (Exception ignored) {
        }
        executor = null;
        aiFuture = null;
        if (workerThread != null) workerThread.interrupt();
        workerThread = null;
        AutomationOrchestrator.get().stopWorkerOnly();
        AutomationRuntime.onStop();
        PageCacheManager.get().invalidate("emergencyStop");
        if (publish) {
            stateVersion.incrementAndGet();
            publish(s, AutomationSession.State.STOPPED, reason == null ? "已停止" : reason);
            AutomationLog.warn("EMERGENCY_STOP " + reason);
        }
    }

    public boolean shouldAcceptEvent(String sessionId, long version, long maxAgeMs) {
        AutomationSession s = current;
        if (s == null || sessionId == null || !sessionId.equals(s.sessionId)) return false;
        if (version < stateVersion.get()) return false;
        return System.currentTimeMillis() - s.startTime <= Math.max(maxAgeMs, 600_000L);
    }

    public void publishToastSafe(Context ctx, String sessionId, long version, String msg) {
        if (!shouldAcceptEvent(sessionId, version, 30_000L)) return;
        mainHandler.post(() -> {
            if (!shouldAcceptEvent(sessionId, version, 30_000L)) return;
            android.widget.Toast.makeText(ctx.getApplicationContext(), msg, android.widget.Toast.LENGTH_LONG).show();
        });
    }

    private void publish(AutomationSession session, AutomationSession.State state, String msg) {
        live.postValue(new StateEvent(session == null ? "" : session.sessionId, stateVersion.get(), state, msg));
        AutomationStateRepository.get().update(session, stateVersion.get(), state, msg);
    }
}
