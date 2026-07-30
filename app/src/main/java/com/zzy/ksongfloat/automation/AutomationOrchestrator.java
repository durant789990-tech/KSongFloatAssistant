package com.zzy.ksongfloat.automation;

import android.content.Context;

import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;
import com.zzy.ksongfloat.ai.AiConfigRepository;
import com.zzy.ksongfloat.engine.AutomationEngineSelector;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 强行自动化流水线：不校验页面/前台 App，点击开始后无限循环上滑与互动。
 * 任何节点/缓存异常仅记录日志，不终止循环。
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
    private volatile Thread worker;
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
        Thread t = new Thread(this::forceLoop, "force-auto-loop");
        worker = t;
        AutomationSessionManager.get().bindWorker(t);
        t.start();
    }

    public void pause() { stop(); }
    public void pauseWithReason(String reason) { stop(); }
    public void resume() { if (appContext != null) start(appContext); }

    public void stop() {
        stopFlag.set(true);
        Thread t = worker;
        if (t != null) t.interrupt();
        AutomationSessionManager.get().cancelAiResources();
        if (session != null) {
            AutomationSessionManager.get().markUserStopped(session, "用户停止");
        }
        running.set(false);
        AutomationRuntime.onStop();
        setPhase(AutomationPhase.STOPPED, "已停止");
    }

    /** 兼容旧调用：不再终止 worker，仅同步运行标志。 */
    public void stopWorkerOnly() {
        AutomationLog.info("stopWorkerOnly 已忽略（强行模式保持运行）");
    }

    public String analyzeCurrentPage(Context ctx) {
        return "强行模式：已跳过页面校验，直接点击「开始」即可运行";
    }

    private void forceLoop() {
        final Thread self = Thread.currentThread();
        AutomationLog.info("强行流水线启动");
        while (!stopFlag.get()) {
            try {
                loopCount++;
                if (session != null) {
                    session.processedUserCount = loopCount;
                }

                setPhase(AutomationPhase.SCROLLING_LIST, "上滑翻页 #" + loopCount);
                try {
                    performForceSwipeUp();
                } catch (Exception e) {
                    AutomationLog.warn("上滑异常（继续）：" + e.getMessage());
                }

                if (stopFlag.get()) break;

                try {
                    RandomDelayHelper.sleepScaled(1500, 1.0, stopFlag);
                } catch (InterruptedException e) {
                    if (stopFlag.get()) break;
                    Thread.interrupted();
                    AutomationLog.warn("等待被中断，继续下一轮");
                    continue;
                }

                if (stopFlag.get()) break;

                setPhase(AutomationPhase.SCANNING, "扫描互动节点 #" + loopCount);
                try {
                    interactCurrentScreen(KSongAccessibilityService.getInstance());
                } catch (Exception e) {
                    AutomationLog.warn("互动异常（继续）：" + e.getMessage());
                }

                setPhase(AutomationPhase.WAITING_NEARBY_LIST, "随机等待");
                try {
                    RandomDelayHelper.delay(settings, stopFlag);
                } catch (InterruptedException e) {
                    if (stopFlag.get()) break;
                    Thread.interrupted();
                    AutomationLog.warn("随机等待被中断，继续下一轮");
                    continue;
                }
                AutomationRuntime.setProcessedCount(loopCount);
            } catch (Exception e) {
                AutomationLog.warn("流水线异常（继续下一轮）：" + e.getMessage());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    if (stopFlag.get()) break;
                    Thread.interrupted();
                }
            }
        }
        if (worker == self) {
            running.set(false);
        }
        AutomationLog.info("强行流水线退出 stopFlag=" + stopFlag.get());
    }

    private void performForceSwipeUp() {
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        if (svc != null) {
            boolean ok = new GestureController(svc).performSwipeUp();
            AutomationLog.info("dispatchGesture 上滑 result=" + ok);
            return;
        }
        AutomationEngineSelector.swipeUp(appContext, this);
    }

    private void interactCurrentScreen(KSongAccessibilityService svc) {
        if (svc == null) {
            AutomationLog.info("无障碍未连接，跳过本屏互动");
            return;
        }
        NodeActionController actions = new NodeActionController(svc);

        try {
            if (actions.clickByTexts("关注", "+ 关注", "加关注")) {
                AutomationLog.info("已点击关注");
                if (session != null) session.actionCount++;
            }
        } catch (Exception e) {
            AutomationLog.warn("关注点击失败：" + e.getMessage());
        }

        boolean openedComment = false;
        try {
            openedComment = actions.clickByTexts("评论", "说点什么", "写评论", "发表评论");
            if (openedComment) AutomationLog.info("已打开评论入口");
        } catch (Exception e) {
            AutomationLog.warn("评论入口点击失败：" + e.getMessage());
        }

        String draft = "";
        try {
            draft = buildDraft();
        } catch (Exception e) {
            AutomationLog.warn("AI 草稿失败：" + e.getMessage());
        }

        if (draft.isEmpty()) {
            try {
                if (actions.clickByTexts("私信", "发消息")) {
                    AutomationLog.info("已打开私信入口");
                }
                draft = buildDraft();
            } catch (Exception e) {
                AutomationLog.warn("私信入口失败：" + e.getMessage());
            }
        }

        if (!draft.isEmpty()) {
            try {
                // 测试模式仅禁止自动发送，不拦截点击/填字/划屏
                boolean autoSend = !settings.testMode && (settings.autoSendComment || settings.autoSend);
                NodeActionController.FillResult fill = actions.fillInputAndSend(draft, autoSend);
                if (fill.filled) {
                    setPhase(AutomationPhase.FILLING_COMMENT,
                            autoSend ? "已填写并尝试发送" : "已填写草稿（测试模式）");
                    AutomationLog.info("填写结果 method=" + fill.method + " sent=" + fill.sent);
                }
            } catch (Exception e) {
                AutomationLog.warn("填写失败：" + e.getMessage());
            }
        } else {
            try {
                if (actions.clickByTexts("说点什么", "输入", "评论")) {
                    actions.setText("很好听！");
                }
            } catch (Exception e) {
                AutomationLog.warn("默认评论失败：" + e.getMessage());
            }
        }
    }

    private String buildDraft() {
        if (!AiConfigRepository.get().isConfigured() || session == null) return "";
        AiContentGenerator.Draft d = AiContentGenerator.generateComment(
                appContext, session, "当前屏幕公开内容", "歌友");
        return d.text == null ? "" : d.text.trim();
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
