package com.zzy.ksongfloat.automation;

import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;

import com.zzy.ksongfloat.accessibility.AccessibilitySnapshot;
import com.zzy.ksongfloat.accessibility.AccessibilityTextExtractor;
import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;
import com.zzy.ksongfloat.ai.AiConfigRepository;
import com.zzy.ksongfloat.capture.PageTextCollector;
import com.zzy.ksongfloat.capture.PageTextResult;
import com.zzy.ksongfloat.classifier.PageClassificationResult;
import com.zzy.ksongfloat.classifier.PageDetector;
import com.zzy.ksongfloat.classifier.PageType;
import com.zzy.ksongfloat.runtime.ForegroundAppDetector;
import com.zzy.ksongfloat.runtime.ForegroundAppResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 同城用户列表自动化流水线。
 */
public class AutomationOrchestrator {
    public interface PhaseListener {
        void onPhase(AutomationPhase phase, String detail);
    }

    private enum PipePhase {
        PRECHECK, ON_CITY_LIST, OPEN_USER, ON_USER_PAGE,
        FILL_COMMENT, FILL_MESSAGE, RETURN_LIST, SWIPE_LIST, DONE
    }

    private static volatile AutomationOrchestrator instance;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private final PageDetector pageDetector = new PageDetector();
    private volatile AutomationPhase phase = AutomationPhase.IDLE;
    private volatile PipePhase pipePhase = PipePhase.PRECHECK;
    private volatile String detail = "";
    private volatile PhaseListener listener;
    private Thread worker;
    private Context appContext;
    private AutomationSession session;
    private AutomationSettings settings;
    private UserTaskQueue taskQueue = new UserTaskQueue();
    private ProcessedUserRepository processedRepo;
    private UserTask activeTask;
    private Map<String, UserCardDetector.Candidate> cardIndex = new HashMap<>();
    private int commentDone;
    private int messageDone;

    public static AutomationOrchestrator get() {
        if (instance == null) synchronized (AutomationOrchestrator.class) {
            if (instance == null) instance = new AutomationOrchestrator();
        }
        return instance;
    }

    public void setListener(PhaseListener l) { listener = l; }

    public void notifyPageChanged(long revision) {
        PageCacheManager.get().invalidate("window_changed");
    }

    public AutomationPhase getPhase() { return phase; }
    public String getDetail() { return detail; }
    public boolean isRunning() { return running.get(); }
    public boolean isPaused() { return false; }
    public int getProcessedUsers() {
        return session == null ? 0 : session.processedUserCount;
    }

    public UserTaskQueue getTaskQueue() { return taskQueue; }
    public AutomationSession getSession() { return session; }

    public void start(Context context) {
        if (running.get()) AutomationSessionManager.get().emergencyStop("重新开始前先停止旧任务");
        appContext = context.getApplicationContext();
        settings = AutomationSettingsRepository.load(appContext);
        stopFlag.set(false);
        taskQueue.clear();
        activeTask = null;
        cardIndex.clear();
        commentDone = 0;
        messageDone = 0;
        pipePhase = PipePhase.PRECHECK;
        processedRepo = new ProcessedUserRepository(settings.duplicateFilterMinutes);
        AutomationLog.clear();
        PageCacheManager.get().invalidate("session_start");
        session = AutomationSessionManager.get().beginSession(appContext);
        running.set(true);
        AutomationRuntime.onStart();
        AutomationRuntime.setCurrentEngine("无障碍");
        setPhase(AutomationPhase.PRECHECK, "创建新会话");
        worker = new Thread(this::runLoop, "city-pipeline");
        AutomationSessionManager.get().bindWorker(worker);
        worker.start();
    }

    public void pause() { AutomationSessionManager.get().emergencyStop("用户暂停"); }
    public void pauseWithReason(String reason) { AutomationSessionManager.get().emergencyStop(reason); }
    public void resume() { AutomationLog.warn("请重新点击开始"); }
    public void stop() { AutomationSessionManager.get().emergencyStop("用户停止"); }

    public void stopWorkerOnly() {
        stopFlag.set(true);
        running.set(false);
        if (worker != null) worker.interrupt();
        worker = null;
        session = null;
        taskQueue.clear();
        setPhase(AutomationPhase.STOPPED, "已停止");
    }

    /** 仅分析当前页面（悬浮窗按钮）。 */
    public String analyzeCurrentPage(Context ctx) {
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        if (svc == null) return "无障碍未连接";
        AccessibilityNodeInfo root = svc.getRootInActiveWindowSafe();
        if (root == null) return "无法读取窗口";
        int sh = ctx.getResources().getDisplayMetrics().heightPixels;
        int sw = ctx.getResources().getDisplayMetrics().widthPixels;
        java.util.List<UserCardDetector.Candidate> cards = UserCardDetector.findCandidates(root, sh, sw);
        PageTextResult page = scanPageText(root);
        CityUserListDetector.Result r = CityUserListDetector.detect(page, root, cards);
        NodeFinder.recycle(root);
        return r.pageType.name() + " conf=" + r.confidence + " cards=" + r.cardCount + " " + r.reason;
    }

    private void runLoop() {
        final String sid = session.sessionId;
        try {
            AutomationSessionManager.get().markRunning(session);
            if (!precheck(sid)) return;

            while (!stopFlag.get() && AutomationSessionManager.get().isActive(sid)
                    && session.processedUserCount < settings.maxUsersPerTask
                    && !isTaskTimedOut()) {

                if (!ensureTargetApp(sid)) {
                    RandomDelayHelper.delay(settings, stopFlag, 0.5);
                    continue;
                }
                if (checkSensitiveAndStop()) return;

                switch (pipePhase) {
                    case ON_CITY_LIST:
                    case PRECHECK:
                        if (!ensureCityList(sid)) break;
                        if (!buildQueue(sid)) break;
                        pipePhase = PipePhase.OPEN_USER;
                        break;
                    case OPEN_USER:
                        if (!openNextUser(sid)) {
                            pipePhase = PipePhase.SWIPE_LIST;
                        } else {
                            pipePhase = PipePhase.ON_USER_PAGE;
                        }
                        break;
                    case ON_USER_PAGE:
                        if (!processUserPage(sid)) {
                            pipePhase = PipePhase.RETURN_LIST;
                        }
                        break;
                    case FILL_COMMENT:
                    case FILL_MESSAGE:
                        pipePhase = PipePhase.RETURN_LIST;
                        break;
                    case RETURN_LIST:
                        if (returnToCityList(sid)) {
                            markUserDone();
                            pipePhase = PipePhase.ON_CITY_LIST;
                        } else {
                            session.continuousFailureCount++;
                            if (session.continuousFailureCount >= settings.consecutiveFailStop) {
                                AutomationSessionManager.get().emergencyStop("返回列表失败，任务已暂停");
                                return;
                            }
                            pipePhase = PipePhase.ON_CITY_LIST;
                        }
                        break;
                    case SWIPE_LIST:
                        if (swipeListPage(sid)) {
                            pipePhase = PipePhase.ON_CITY_LIST;
                        } else {
                            AutomationSessionManager.get().emergencyStop("列表暂无更多用户");
                            return;
                        }
                        break;
                    default:
                        pipePhase = PipePhase.ON_CITY_LIST;
                        break;
                }
                RandomDelayHelper.delay(settings, stopFlag, 0.3);
            }
            if (!stopFlag.get()) setPhase(AutomationPhase.STOPPED, "任务完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AutomationSessionManager.get().emergencyStop(e.getMessage() == null ? "异常" : e.getMessage());
        } finally {
            running.set(false);
        }
    }

    private boolean precheck(String sid) {
        if (!AiConfigRepository.get().isConfigured()) {
            AutomationSessionManager.get().emergencyStop("请先配置 AI 接口");
            return false;
        }
        ForegroundAppResolver.Result fr = ForegroundAppResolver.resolve(appContext);
        if (fr.presence == ForegroundAppResolver.AppPresence.OTHER_APP) {
            AutomationSessionManager.get().emergencyStop("当前不在全民K歌");
            return false;
        }
        session.currentPackage = fr.packageName.isEmpty() ? fr.underlyingPackage : fr.packageName;
        AutomationRuntime.setForegroundPackage(com.zzy.ksongfloat.runtime.ForegroundAppResolver.displayPackage(appContext));
        pipePhase = PipePhase.ON_CITY_LIST;
        return AutomationSessionManager.get().isValid(sid);
    }

    private boolean ensureTargetApp(String sid) {
        ForegroundAppResolver.Result fr = ForegroundAppResolver.resolve(appContext);
        if (fr.presence == ForegroundAppResolver.AppPresence.OTHER_APP) {
            AutomationSessionManager.get().emergencyStop("已离开全民K歌：" + fr.packageName);
            return false;
        }
        if (fr.presence == ForegroundAppResolver.AppPresence.UNKNOWN_TRANSIENT) {
            session.unknownStreak++;
            setPhase(AutomationPhase.RECOVERING, "等待窗口 " + session.unknownStreak + "/3");
            return session.unknownStreak < 3;
        }
        session.unknownStreak = 0;
        return true;
    }

    private boolean checkSensitiveAndStop() {
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        if (svc == null) return false;
        AccessibilityNodeInfo root = svc.getRootInActiveWindowSafe();
        if (root == null) return false;
        PageTextResult page = scanPageText(root);
        SensitivePageGuard.Result s = SensitivePageGuard.inspect(page, root);
        NodeFinder.recycle(root);
        if (s.sensitive) {
            AutomationSessionManager.get().emergencyStop("检测到账号或设置页面，任务已停止");
            return true;
        }
        return false;
    }

    private boolean ensureCityList(String sid) throws InterruptedException {
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        AccessibilityNodeInfo root = obtainRoot(sid, svc);
        if (root == null) return false;
        int sh = appContext.getResources().getDisplayMetrics().heightPixels;
        int sw = appContext.getResources().getDisplayMetrics().widthPixels;
        java.util.List<UserCardDetector.Candidate> cards = UserCardDetector.findCandidates(root, sh, sw);
        PageTextResult page = scanPageText(root);
        CityUserListDetector.Result r = CityUserListDetector.detect(page, root, cards);
        session.currentPage = r.pageType.name();
        session.currentWindowId = com.zzy.ksongfloat.runtime.ForegroundAppDetector.lastWindowId();
        AutomationRuntime.setCurrentPage(r.pageType);
        NodeFinder.recycle(root);
        if (!r.cityList) {
            AutomationSessionManager.get().emergencyStop("请先进入同城用户列表后再开始");
            return false;
        }
        setPhase(AutomationPhase.SCANNING, "同城列表 · 可见用户 " + r.cardCount);
        return true;
    }

    private boolean buildQueue(String sid) {
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        AccessibilityNodeInfo root = svc == null ? null : svc.getRootInActiveWindowSafe();
        if (root == null) return false;
        int sh = appContext.getResources().getDisplayMetrics().heightPixels;
        int sw = appContext.getResources().getDisplayMetrics().widthPixels;
        java.util.List<UserCardDetector.Candidate> cards = UserCardDetector.findCandidates(root, sh, sw);
        int wid = com.zzy.ksongfloat.runtime.ForegroundAppDetector.lastWindowId();
        taskQueue.buildFromCards(appContext, cards, processedRepo, wid);
        cardIndex.clear();
        for (UserCardDetector.Candidate c : cards) cardIndex.put(c.nodeKey, c);
        session.queueSize = taskQueue.size();
        NodeFinder.recycle(root);
        AutomationLog.info("SCAN cards=" + cards.size() + " queue=" + taskQueue.pendingCount());
        return taskQueue.hasPending();
    }

    private boolean openNextUser(String sid) throws InterruptedException {
        activeTask = taskQueue.nextPending();
        if (activeTask == null) return false;
        activeTask.status = UserTask.Status.OPENING;
        session.currentUserName = activeTask.displayName;
        session.currentUserIndex = activeTask.queueIndex;
        setPhase(AutomationPhase.OPENING_USER, "打开用户：" + activeTask.displayName);

        UserCardDetector.Candidate card = cardIndex.get(activeTask.cardNodeKey);
        if (card == null || UserCardDetector.isExcludedNavLabel(activeTask.displayName)) {
            activeTask.status = UserTask.Status.FAILED;
            activeTask.failureReason = "卡片无效或属于导航";
            session.failedUserCount++;
            return true;
        }
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        long rev = KSongAccessibilityService.getPageRevision();
        boolean ok = new NodeActionController(svc).click(new NodeFinder.Match(card.node, "user_card", card.label));
        PageCacheManager.get().invalidate("open_user");
        if (!ok) {
            activeTask.status = UserTask.Status.FAILED;
            activeTask.failureReason = "点击失败";
            session.failedUserCount++;
            AutomationLog.warn("OPEN_USER_FAILED " + activeTask.displayName);
            return true;
        }
        PageChangeWaiter.waitForChange(rev);
        RandomDelayHelper.delay(settings, stopFlag, 0.6);
        PageType pt = currentPageType();
        if (pt != PageType.USER_PROFILE && pt != PageType.WORK_DETAIL) {
            activeTask.status = UserTask.Status.FAILED;
            activeTask.failureReason = "未进入用户页：" + pt;
            session.failedUserCount++;
            AutomationLog.warn("OPEN_USER_VERIFY_FAIL page=" + pt);
            pipePhase = PipePhase.RETURN_LIST;
            return true;
        }
        activeTask.status = UserTask.Status.ANALYZING;
        AutomationLog.info("OPEN_USER_OK " + activeTask.displayName + " page=" + pt);
        return true;
    }

    private boolean processUserPage(String sid) throws InterruptedException {
        if (activeTask == null) return false;
        setPhase(AutomationPhase.REQUESTING_AI, "分析用户：" + activeTask.displayName);
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        AccessibilityNodeInfo root = svc.getRootInActiveWindowSafe();
        PageTextResult page = scanPageText(root);
        String summary = page == null ? "" : page.mergedText;
        NodeFinder.recycle(root);

        if (settings.enableCommentDraft) {
            AiContentGenerator.Draft draft = AiContentGenerator.generateComment(
                    appContext, session, summary, activeTask.displayName);
            if (draft.text.isEmpty()) {
                AutomationLog.warn("COMMENT_DRAFT_EMPTY " + draft.reason);
            } else {
                activeTask.commentDraft = draft.text;
                if (tryFillComment(svc, draft.text)) {
                    AutomationLog.info("COMMENT_FILLED testMode=" + settings.testMode);
                    commentDone++;
                }
            }
        }
        if (settings.enablePrivateMessageDraft) {
            if (openPrivateMessageEntry(svc)) {
                AiContentGenerator.Draft draft = AiContentGenerator.generatePrivateMessage(
                        appContext, session, summary, activeTask.displayName);
                if (!draft.text.isEmpty() && tryFillMessage(svc, draft.text)) {
                    AutomationLog.info("PM_FILLED testMode=" + settings.testMode);
                    messageDone++;
                }
            }
        }
        activeTask.status = UserTask.Status.COMPLETED;
        return false;
    }

    private boolean tryFillComment(KSongAccessibilityService svc, String text) throws InterruptedException {
        long rev = KSongAccessibilityService.getPageRevision();
        NodeActionController actions = new NodeActionController(svc);
        if (currentPageType() == PageType.USER_PROFILE || currentPageType() == PageType.WORK_DETAIL) {
            actions.clickByTexts("评论", "说点什么");
            PageChangeWaiter.waitForChange(rev, 3000);
        }
        NodeActionController.FillResult r = actions.fillInputAndSend(text, false);
        setPhase(AutomationPhase.FILLING_COMMENT, settings.testMode ? "评论草稿已填写，未发送" : "评论已填写");
        return r.filled;
    }

    private boolean openPrivateMessageEntry(KSongAccessibilityService svc) throws InterruptedException {
        long rev = KSongAccessibilityService.getPageRevision();
        boolean ok = new NodeActionController(svc).clickByTexts("私信", "发消息");
        if (!ok) return false;
        PageChangeWaiter.waitForChange(rev, 3000);
        return currentPageType() == PageType.PRIVATE_MESSAGE
                || currentPageType() == PageType.PRIVATE_MESSAGE_INPUT;
    }

    private boolean tryFillMessage(KSongAccessibilityService svc, String text) {
        NodeActionController.FillResult r = new NodeActionController(svc).fillInputAndSend(text, false);
        setPhase(AutomationPhase.FILLING_PM, settings.testMode ? "私信草稿已填写，未发送" : "私信已填写");
        return r.filled;
    }

    private boolean returnToCityList(String sid) throws InterruptedException {
        setPhase(AutomationPhase.GOING_BACK, "返回同城列表");
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        long rev = KSongAccessibilityService.getPageRevision();
        new NodeActionController(svc).back();
        PageChangeWaiter.waitForChange(rev, 5000);
        PageCacheManager.get().invalidate("back");
        RandomDelayHelper.delay(settings, stopFlag, 0.5);
        CityUserListDetector.Result r = detectCityList();
        if (r.cityList) {
            AutomationLog.info("RETURN_LIST_OK");
            session.continuousFailureCount = 0;
            return true;
        }
        rev = KSongAccessibilityService.getPageRevision();
        new NodeActionController(svc).back();
        PageChangeWaiter.waitForChange(rev, 3000);
        r = detectCityList();
        AutomationLog.warn("RETURN_LIST_RETRY ok=" + r.cityList);
        return r.cityList;
    }

    private void markUserDone() {
        if (activeTask == null) return;
        if (activeTask.status != UserTask.Status.FAILED) {
            activeTask.status = UserTask.Status.COMPLETED;
            processedRepo.markProcessed(appContext, activeTask.userKey);
            session.processedUserCount++;
        }
        taskQueue.advanceAfterComplete();
        activeTask = null;
        session.queueSize = taskQueue.pendingCount();
        AutomationRuntime.setProcessedCount(session.processedUserCount);
    }

    private boolean swipeListPage(String sid) throws InterruptedException {
        if (!detectCityList().cityList) {
            AutomationSessionManager.get().emergencyStop("不在同城列表，无法滑动");
            return false;
        }
        setPhase(AutomationPhase.SCROLLING_LIST, "当前屏已处理完，加载下一批");
        long rev = KSongAccessibilityService.getPageRevision();
        com.zzy.ksongfloat.engine.ActionResult r = com.zzy.ksongfloat.engine.AutomationEngineSelector.swipeUp(appContext, this);
        AutomationLog.info("SWIPE_LIST result=" + r.name());
        PageChangeWaiter.waitForChange(rev, 5000);
        PageCacheManager.get().invalidate("swipe");
        RandomDelayHelper.delay(settings, stopFlag, 0.8);
        int before = taskQueue.size();
        buildQueue(sid);
        if (taskQueue.hasPending()) {
            session.emptySwipeStreak = 0;
            return true;
        }
        session.emptySwipeStreak++;
        AutomationLog.warn("SWIPE_NO_NEW_USERS streak=" + session.emptySwipeStreak);
        return session.emptySwipeStreak < 3;
    }

    private CityUserListDetector.Result detectCityList() {
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        AccessibilityNodeInfo root = svc == null ? null : svc.getRootInActiveWindowSafe();
        if (root == null) return new CityUserListDetector.Result(false, PageType.UNKNOWN, 0, "no root", 0);
        int sh = appContext.getResources().getDisplayMetrics().heightPixels;
        int sw = appContext.getResources().getDisplayMetrics().widthPixels;
        java.util.List<UserCardDetector.Candidate> cards = UserCardDetector.findCandidates(root, sh, sw);
        PageTextResult page = scanPageText(root);
        CityUserListDetector.Result r = CityUserListDetector.detect(page, root, cards);
        NodeFinder.recycle(root);
        return r;
    }

    private PageType currentPageType() {
        KSongAccessibilityService svc = KSongAccessibilityService.getInstance();
        AccessibilityNodeInfo root = svc == null ? null : svc.getRootInActiveWindowSafe();
        if (root == null) return PageType.UNKNOWN;
        PageTextResult page = scanPageText(root);
        PageClassificationResult cls = pageDetector.detect(page);
        NodeFinder.recycle(root);
        if (cls.pageType == PageType.UNKNOWN) {
            CityUserListDetector.Result r = detectCityList();
            return r.pageType;
        }
        return cls.pageType;
    }

    private PageTextResult scanPageText(AccessibilityNodeInfo root) {
        if (root == null) return null;
        AccessibilitySnapshot snap = new AccessibilityTextExtractor().extract(
                ForegroundAppResolver.TARGET_PKG, "", root, true);
        return new PageTextCollector().collect(snap, null, 0, true);
    }

    private AccessibilityNodeInfo obtainRoot(String sid, KSongAccessibilityService svc) throws InterruptedException {
        if (svc == null) return null;
        for (int i = 0; i < 3; i++) {
            AccessibilityNodeInfo root = svc.getRootInActiveWindowSafe();
            if (root != null) return root;
            Thread.sleep(500);
            if (!AutomationSessionManager.get().isActive(sid)) return null;
        }
        return null;
    }

    private boolean isTaskTimedOut() {
        if (settings.maxTaskDurationMinutes <= 0) return false;
        return System.currentTimeMillis() - session.startTime > settings.maxTaskDurationMinutes * 60_000L;
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
