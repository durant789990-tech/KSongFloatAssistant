package com.zzy.ksongfloat.automation;

import android.content.Context;

import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;
import com.zzy.ksongfloat.ai.AiConfigRepository;
import com.zzy.ksongfloat.engine.AutomationEngineSelector;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 强行自动化流水线：不校验页面/前台 App，点击开始后无限循环上滑与互动。
 */
public class AutomationOrchestrator {
    public interface PhaseListener {
        void onPhase(AutomationPhase phase, String detail);
    }

    private static volatile AutomationOrchestrator instance;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private volatile AutomationPhase phase = AutomationPhase.IDLE;
    private volatile String detail = "";
    private volatile PhaseListener listener;
    private Thread worker;
    private Context appContext;
    private AutomationSession session;
    private AutomationSettings settings;
    private int loopCount;

    public static AutomationOrchestrator get() {
        if (instance == null) synchronized (AutomationOrchestrator.class) {
            if (instance == null) instance = new AutomationOrchestrator();
        }
        return instance;
    }

    public void setListener(PhaseListener l) { listener = l; }

    public void notifyPageChanged(long revision) { /* 强行模式不依赖页面缓存 */ }

    public AutomationPhase getPhase() { return phase; }
    public String getDetail() { return detail; }
    public boolean isRunning() { return running.get(); }
    public boolean isPaused() { return false; }
    public int getProcessedUsers() { return loopCount; }
    public UserTaskQueue getTaskQueue() { return new UserTaskQueue(); }
    public AutomationSession getSession() { return session; }

    /** 用户点击开始后立即启动，不做前台/页面拦截。 */
    public void start(Context context) {
        if (running.get()) stop();
        appContext = context.getApplicationContext();
        settings = AutomationSettingsRepository.load(appContext);
        stopFlag.set(false);
        loopCount = 0;
        AutomationLog.clear();
        session = AutomationSessionManager.get().beginSession(appContext);
        running.set(true);
        AutomationRuntime.onStart();
        AutomationRuntime.setCurrentEngine("无障碍·强行模式");
        AutomationRuntime.setFloatMessage("强行流水线已启动");
        setPhase(AutomationPhase.EXECUTING, "无限循环模式");
        AutomationSessionManager.get().markRunning(session);
        worker = new Thread(this::forceLoop, "force-auto-loop");
        AutomationSessionManager.get().bindWorker(worker);
        worker.start();
    }

    public void pause() { stop(); }
    public void pauseWithReason(String reason) { stop(); }
    public void resume() { if (appContext != null) start(appContext); }

    public void stop() {
        stopFlag.set(true);
        AutomationSessionManager.get().emergencyStop("用户停止");
        running.set(false);
        setPhase(AutomationPhase.STOPPED, "已停止");
    }

    public void stopWorkerOnly() {
        stopFlag.set(true);
        running.set(false);
        worker = null;
        session = null;
        setPhase(AutomationPhase.STOPPED, "已停止");
    }

    public String analyzeCurrentPage(Context ctx) {
        return "强行模式：已跳过页面校验，直接点击「开始」即可运行";
    }

    private void forceLoop() {
        final String sid = session == null ? "" : session.sessionId;
        try {
            while (!stopFlag.get() && AutomationSessionManager.get().isValid(sid)) {
                loopCount++;
                session.processedUserCount = loopCount;

                // Step 1: 强制上滑
                setPhase(AutomationPhase.SCROLLING_LIST, "上滑翻页 #" + loopCount);
                KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
                if (svc != null) {
                    new GestureController(svc).performSwipeUp();
                } else {
                    AutomationEngineSelector.swipeUp(appContext, this);
                }
                RandomDelayHelper.sleepScaled(1500, 1.0, stopFlag);

                if (stopFlag.get()) break;

                // Step 2: 深度查找与互动（找不到则静默跳过）
                setPhase(AutomationPhase.SCANNING, "扫描互动节点 #" + loopCount);
                interactCurrentScreen(svc);

                // Step 3: 随机等待 2~5 秒
                setPhase(AutomationPhase.WAITING_NEARBY_LIST, "随机等待");
                RandomDelayHelper.delay(settings, stopFlag);
                AutomationRuntime.setProcessedCount(loopCount);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AutomationLog.warn("流水线异常（继续下一轮）：" + e.getMessage());
        } finally {
            running.set(false);
        }
    }

    private void interactCurrentScreen(KSongAccessibilityService svc) {
        if (svc == null) {
            AutomationLog.info("无障碍未连接，跳过本屏互动");
            return;
        }
        NodeActionController actions = new NodeActionController(svc);

        if (actions.clickByTexts("关注", "+ 关注", "加关注")) {
            AutomationLog.info("已点击关注");
            session.actionCount++;
        }

        boolean openedComment = actions.clickByTexts("评论", "说点什么", "写评论", "发表评论");
        if (openedComment) {
            AutomationLog.info("已打开评论入口");
        }

        String draft = buildDraft();
        if (draft.isEmpty()) {
            if (actions.clickByTexts("私信", "发消息")) {
                AutomationLog.info("已打开私信入口");
            }
            draft = buildDraft();
        }

        if (!draft.isEmpty()) {
            boolean autoSend = !settings.testMode && (settings.autoSendComment || settings.autoSend);
            NodeActionController.FillResult fill = actions.fillInputAndSend(draft, autoSend);
            if (fill.filled) {
                setPhase(AutomationPhase.FILLING_COMMENT,
                        autoSend ? "已填写并尝试发送" : "已填写草稿（测试模式）");
                AutomationLog.info("填写结果 method=" + fill.method + " sent=" + fill.sent);
            }
        } else if (actions.clickByTexts("说点什么", "输入", "评论")) {
            actions.setText("很好听！");
        }
    }

    private String buildDraft() {
        if (!AiConfigRepository.get().isConfigured() || session == null) return "";
        try {
            AiContentGenerator.Draft d = AiContentGenerator.generateComment(
                    appContext, session, "当前屏幕公开内容", "歌友");
            return d.text == null ? "" : d.text.trim();
        } catch (Exception e) {
            AutomationLog.warn("AI 草稿跳过：" + e.getMessage());
            return "";
        }
    }

    private void setPhase(AutomationPhase p, String d) {
        phase = p;
        detail = d == null ? "" : d;
        AutomationRuntime.setLastAction(p.name() + (detail.isEmpty() ? "" : ": " + detail));
        PhaseListener l = listener;
        if (l != null) l.onPhase(p, detail);
        if (session != null) {
            session.lastAction = AutomationRuntime.getLastAction();
            AutomationStateRepository.get().update(session, AutomationSessionManager.get().stateVersion(),
                    session.state, detail);
        }
    }
}
