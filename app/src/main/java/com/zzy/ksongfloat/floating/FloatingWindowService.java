package com.zzy.ksongfloat.floating;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.widget.*;

import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.Future;

import com.zzy.ksongfloat.*;
import com.zzy.ksongfloat.accessibility.*;
import com.zzy.ksongfloat.ai.*;
import com.zzy.ksongfloat.ai.model.*;
import com.zzy.ksongfloat.ai.AiConfigRepository;
import com.zzy.ksongfloat.automation.AutomationLog;
import com.zzy.ksongfloat.automation.AutomationOrchestrator;
import com.zzy.ksongfloat.automation.AutomationPhase;
import com.zzy.ksongfloat.automation.AutomationRuntime;
import com.zzy.ksongfloat.capture.*;
import com.zzy.ksongfloat.classifier.*;
import com.zzy.ksongfloat.config.TargetAppConfig;
import com.zzy.ksongfloat.runtime.ForegroundAppResolver;
import com.zzy.ksongfloat.history.*;
import com.zzy.ksongfloat.privacy.PrivacySettings;
import com.zzy.ksongfloat.runtime.*;
import com.zzy.ksongfloat.security.SecureStorage;
import com.zzy.ksongfloat.util.PermissionUtils;

public class FloatingWindowService extends Service {
    public static final String ACTION_STOP = "com.zzy.ksongfloat.action.STOP_FLOATING";
    private static volatile boolean running;
    private WindowManager wm;
    private View bubble, panel, confirmView;
    private WindowManager.LayoutParams bubbleParams, panelParams;
    private LinearLayout content;
    private TextView status;
    private TextView logView;
    private TextView bubbleText;
    private AssistantStateObserver stateObserver;
    private volatile boolean busy = false;
    private long generation = 0, lastRegenAt = 0;
    private final OcrEngine ocr = new OcrEngine();
    private Future<OcrResult> ocrFuture;
    private final AiClient aiClient = new AiClient();
    private PageTextResult lastText;
    private PageClassificationResult lastCls;
    private AiAnalysisResult lastAi;
    private final List<String> previous = new ArrayList<>();

    public void onCreate() {
        super.onCreate();
        running = true;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        AssistantStateCoordinator.serviceStarted(this);
        stateObserver = snapshot -> {
            updateBubble(snapshot);
            updateNotification(snapshot);
            updatePanelStatus(snapshot);
        };
        AssistantStateRepository.observe(stateObserver);
        startFg();
        showBubble();
    }

    public int onStartCommand(Intent i, int f, int id) {
        if (i != null && ACTION_STOP.equals(i.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startFg();
        ensureBubbleVisible();
        return START_NOT_STICKY;
    }

    /** 有悬浮窗权限时强制显示悬浮球。 */
    private void ensureBubbleVisible() {
        if (!PermissionUtils.canDrawOverlays(this)) {
            toast("请先开启悬浮窗权限");
            return;
        }
        if (bubble == null) showBubble();
        else try {
            wm.updateViewLayout(bubble, bubbleParams);
        } catch (Exception e) {
            try { wm.addView(bubble, bubbleParams); } catch (Exception ignored) { showBubble(); }
        }
    }

    public IBinder onBind(Intent i) { return null; }

    private void startFg() {
        String ch = "ksong_float_service";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(ch, "K歌悬浮助手", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(c);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, ch) : new Notification.Builder(this);
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPi = PendingIntent.getActivity(this, 2002, open, PendingIntent.FLAG_UPDATE_CURRENT | piImmutable());
        Intent stop = new Intent(this, FloatingWindowService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 2003, stop, PendingIntent.FLAG_UPDATE_CURRENT | piImmutable());
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("K歌悬浮助手正在运行")
                .setContentText(notificationText(AssistantStateRepository.get()))
                .setContentIntent(openPi)
                .addAction(android.R.drawable.ic_menu_view, "打开", openPi)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopPi)
                .setOngoing(true);
        startForeground(2001, b.build());
    }

    private void showBubble() {
        TextView v = new TextView(this);
        bubbleText = v;
        v.setText("K助");
        v.setTextColor(Color.WHITE);
        v.setTextSize(13);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        v.setBackground(Rounded.bg(Color.rgb(100, 86, 230), dp(28), Color.WHITE, 1));
        bubble = v;
        bubbleParams = params(dp(56), dp(56));
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        int sw = getResources().getDisplayMetrics().widthPixels;
        bubbleParams.x = FloatingPositionRepository.x(this, sw - dp(68));
        bubbleParams.y = FloatingPositionRepository.y(this, dp(360));
        limitPosition(bubbleParams);
        v.setOnTouchListener(new FloatingTouchController(v, bubbleParams, new FloatingTouchController.Callback() {
            public void onClick() { togglePanel(); }
            public void onDrag(int x, int y) { limitPosition(bubbleParams); update(bubble, bubbleParams); }
            public void onDragEnd(int x, int y) { snapBubbleToEdge(); FloatingPositionRepository.save(FloatingWindowService.this, bubbleParams.x, bubbleParams.y); }
            public void onLongPress() { showStopConfirm(); }
        }));
        v.setOnClickListener(x -> {});
        try { wm.addView(bubble, bubbleParams); } catch (Exception e) { toast("悬浮球显示失败：" + safe(e)); stopSelf(); }
        updateBubble(AssistantStateRepository.get());
    }

    private void togglePanel() { if (panel == null) showPanel(); else hidePanel(); }

    private void showPanel() {
        ScrollView sv = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(10), dp(12), dp(12));
        content.setBackground(Rounded.bg(Color.argb(238, 20, 26, 48), dp(18), Color.rgb(86, 101, 160), 1));
        sv.addView(content);

        LinearLayout dragBar = new LinearLayout(this);
        dragBar.setOrientation(LinearLayout.HORIZONTAL);
        dragBar.setGravity(Gravity.CENTER_VERTICAL);
        dragBar.setPadding(0, 0, 0, dp(6));
        TextView title = label("K歌助手", 16, Color.WHITE, true);
        TextView collapse = label(" 收起 ", 12, Color.rgb(190, 205, 255), true);
        collapse.setOnClickListener(v -> hidePanel());
        dragBar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        dragBar.addView(collapse);
        content.addView(dragBar);

        status = label(floatStatusText(), 12, Color.rgb(210, 220, 245), false);
        status.setPadding(0, dp(4), 0, dp(8));
        content.addView(status);

        logView = label("", 10, Color.rgb(170, 190, 230), false);
        logView.setMaxLines(3);
        content.addView(logView);

        content.addView(row(btn("开始", v -> startAutomation()), btn("暂停/继续", v -> toggleAutomationPause())));
        content.addView(row(btn("停止", v -> stopAutomation()), btn("分析当前页面", v -> analyzeCurrentPage())));
        content.addView(btn("打开应用", v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }));

        panel = sv;
        panelParams = params(dp(300), WindowManager.LayoutParams.WRAP_CONTENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = dp(18);
        panelParams.y = dp(70);
        drag(dragBar, panelParams);
        try { wm.addView(panel, panelParams); } catch (Exception e) { panel = null; toast("悬浮面板展开失败：" + safe(e)); }
        AutomationLog.setListener(line -> appendLog(line));
        AutomationOrchestrator.get().setListener((phase, detail) -> refreshFloatUi());
        refreshFloatUi();
    }

    private String floatStatusText() {
        AutomationRuntime.UiStatus st = AutomationRuntime.getStatus();
        String statusLabel = st == AutomationRuntime.UiStatus.RUNNING ? "运行中"
                : st == AutomationRuntime.UiStatus.PAUSED ? "已暂停"
                : st == AutomationRuntime.UiStatus.ERROR ? "错误" : "未运行";
        String msg = AutomationRuntime.getFloatMessage();
        String ai = AiConfigRepository.get().isConfigured() ? "已配置" : "未配置";
        AutomationOrchestrator orch = AutomationOrchestrator.get();
        int queue = orch.getTaskQueue() == null ? 0 : orch.getTaskQueue().pendingCount();
        String user = orch.getSession() == null ? "" : orch.getSession().currentUserName;
        return "状态：" + statusLabel
                + "\n页面：" + AutomationRuntime.getCurrentPage()
                + "\n前台：" + ForegroundAppResolver.displayPackage(this)
                + "\n引擎：" + AutomationRuntime.getCurrentEngine()
                + "\nAI：" + ai
                + "\n队列：" + queue
                + (user.isEmpty() ? "" : "\n当前用户：" + user)
                + "\n已处理：" + AutomationRuntime.getProcessedCount()
                + "\n最近动作：" + AutomationRuntime.getLastAction()
                + (msg.isEmpty() ? "" : "\n" + msg);
    }

    private void analyzeCurrentPage() {
        String result = AutomationOrchestrator.get().analyzeCurrentPage(this);
        toast(result);
        refreshFloatUi();
    }

    private void refreshFloatUi() {
        if (status == null) return;
        status.post(() -> status.setText(floatStatusText()));
    }

    private void startAutomation() {
        ensureBubbleVisible();
        if (panel == null) showPanel();
        AutomationOrchestrator.get().start(this);
        AutomationRuntime.setFloatMessage("强行流水线运行中");
        refreshFloatUi();
    }

    private void toggleAutomationPause() {
        AutomationOrchestrator o = AutomationOrchestrator.get();
        if (!o.isRunning()) { toast("自动引流未运行"); return; }
        if (o.isPaused()) o.resume(); else o.pause();
    }

    private void stopAutomation() {
        AutomationOrchestrator.get().stop();
        setStatus("自动引流已停止");
    }

    private void appendLog(String line) {
        if (logView == null) return;
        logView.post(() -> {
            String cur = logView.getText() == null ? "" : logView.getText().toString();
            String next = (cur + "\n" + line).trim();
            String[] parts = next.split("\n");
            if (parts.length > 3) {
                StringBuilder sb = new StringBuilder();
                for (int i = parts.length - 3; i < parts.length; i++) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(parts[i]);
                }
                next = sb.toString();
            }
            logView.setText(next);
        });
    }

    private void analyze() {
        if (busy) { toast("正在处理，请稍等"); return; }
        if (!PermissionUtils.canDrawOverlays(this)) { setStatus("缺少悬浮窗权限"); return; }
        if (!PermissionUtils.isAccessibilityEnabled(this)) { setStatus("请先开启无障碍服务"); return; }
        if (!TargetAppConfig.matches(this, KSongAccessibilityService.getForegroundPackage())) { setStatus("当前不在目标 K 歌页面"); return; }
        AccessibilitySnapshot snap = AccessibilitySnapshotRepository.get().latest();
        if (snap == null || !snap.ksongForeground) { setStatus("还没有捕获到目标页面文本，请切到目标 App 后重试"); return; }
        busy = true;
        AssistantStateRepository.update(AssistantStateRepository.get().withState(AssistantRuntimeState.CAPTURING));
        long gid = ++generation;
        setStatus("正在采集页面文本\n准备截图 OCR...");
        ScreenCaptureManager.get().requestSingleCapture(this, new ScreenCaptureManager.CaptureCallback() {
            public void onStatus(ScreenCaptureManager.Status s, String m) { if (gid == generation) setStatus(m); }
            public void onError(ScreenCaptureManager.Status s, String m) { if (gid == generation) { busy = false; setStatus(m); } }
            public void onSuccess(CapturedImage img) {
                new Thread(() -> {
                    boolean deleted = false;
                    try {
                        AssistantStateRepository.update(AssistantStateRepository.get().withState(AssistantRuntimeState.OCR_PROCESSING));
                        setStatus("正在进行 OCR 识别...");
                        ocrFuture = ocr.recognizeAsync(img.getBitmap());
                        OcrResult or = ocrFuture.get();
                        img.release(true);
                        deleted = true;
                        PageTextResult ptr = new PageTextCollector().collect(AccessibilitySnapshotRepository.get().latest(), or, img.capturedAt, true);
                        if (!ptr.accessibilityAvailable && !ptr.ocrAvailable) { setStatus("没有识别到可用文本，请换个页面重试"); return; }
                        PageClassificationResult cls = new PageClassifier().classify(ptr);
                        lastText = ptr;
                        lastCls = cls;
                        DebugState.update(ptr, cls, deleted);
                        AssistantStateCoordinator.recompute(FloatingWindowService.this, true, AssistantRuntimeState.AI_ANALYZING, null);
                        setStatus("页面识别完成，正在请求 AI 分析...");
                        requestAi(gid, AiAnalysisRequest.RequestType.PAGE_ANALYSIS, false);
                    } catch (Exception e) {
                        setStatus("分析失败：" + safe(e));
                    } finally {
                        busy = false;
                        try { img.release(true); } catch (Exception ignored) {}
                    }
                }, "ai-analysis-flow").start();
            }
        });
    }

    private void requestSuggestion(boolean privateMsg, boolean regen) {
        if (lastText == null || lastCls == null) { toast("请先分析当前页面"); return; }
        if (regen && System.currentTimeMillis() - lastRegenAt < 3000) { toast("换一句太快了，请稍后再试"); return; }
        if (!TargetAppConfig.matches(this, KSongAccessibilityService.getForegroundPackage())) { toast("当前不在目标页面"); return; }
        lastRegenAt = System.currentTimeMillis();
        busy = true;
        long gid = ++generation;
        requestAi(gid, privateMsg ? (regen ? AiAnalysisRequest.RequestType.REGENERATE_PRIVATE_MESSAGE : AiAnalysisRequest.RequestType.PRIVATE_MESSAGE_SUGGESTION) : (regen ? AiAnalysisRequest.RequestType.REGENERATE_COMMENT : AiAnalysisRequest.RequestType.COMMENT_SUGGESTION), regen);
    }

    private void requestAi(long gid, AiAnalysisRequest.RequestType type, boolean regen) {
        try {
            AiSettings s = AiSettingsRepository.load(this);
            if (!s.aiConsent) { busy = false; showConsentThen(type); return; }
            if (!s.isUsable()) { busy = false; setStatus("AI 设置未完成，请填写 Base URL、模型名并勾选同意"); return; }
            String key = SecureStorage.loadApiKey(this);
            if (key.isEmpty()) { busy = false; setStatus("缺少 API Key，请先到 AI 设置页保存"); return; }
            BaseUrlNormalizer.Result nr = BaseUrlNormalizer.normalize(s.baseUrl);
            if (!nr.ok) { busy = false; setStatus(nr.error); return; }
            AiAnalysisRequest ar = buildAnalysisRequest(s, type);
            AiPromptBuilder pb = new AiPromptBuilder();
            JSONObject body = pb.buildChatRequest(s, AiSystemPrompt.TEXT, pb.buildUserMessage(ar), false);
            setStatus("正在请求 AI...");
            AssistantStateCoordinator.recompute(FloatingWindowService.this, true, AssistantRuntimeState.AI_ANALYZING, null);
            aiClient.chatAsync(new AiRequest(nr.url, key, body.toString(), s.timeoutSeconds), new AiClient.Callback() {
                public void onState(AiRequestState st, String msg) { if (gid == generation) setStatus(msg); }
                public void onResult(AiCallResult cr) { if (gid != generation) return; busy = false; handleAiResult(cr, ar, s, gid); }
            });
        } catch (Exception e) {
            busy = false;
            setStatus("AI 请求失败：" + safe(e));
        }
    }

    private AiAnalysisRequest buildAnalysisRequest(AiSettings s, AiAnalysisRequest.RequestType type) {
        AiAnalysisRequest r = new AiAnalysisRequest();
        r.requestType = type;
        r.pageType = lastCls == null ? "UNKNOWN" : lastCls.pageType.name();
        r.pageConfidence = lastCls == null ? 0 : lastCls.confidence;
        r.packageName = lastText == null ? "" : lastText.packageName;
        r.windowTitle = lastText == null ? "" : lastText.windowTitle;
        String a = s.allowAccessibilityText && lastText != null ? lastText.accessibilityText : "";
        String o = s.allowOcrText && lastText != null ? lastText.ocrText : "";
        r.visibleText = (a + "\n" + o).trim();
        r.resourceIds = lastText == null ? new ArrayList<>() : lastText.resourceIds;
        r.contentDescriptions = lastText == null ? new ArrayList<>() : lastText.contentDescriptions;
        r.detectedNickname = lastCls == null ? "" : lastCls.detectedNickname;
        r.detectedSongTitles = lastCls == null ? new ArrayList<>() : lastCls.detectedSongTitles;
        r.userStyle = s.userStyle;
        r.customPrompt = s.customPrompt;
        r.previousSuggestions.addAll(previous);
        return r;
    }

    private void handleAiResult(AiCallResult cr, AiAnalysisRequest req, AiSettings s, long gid) {
        if (!cr.success) {
            AiDebugState.lastState = "FAILED";
            AiDebugState.lastError = cr.error.message;
            AiDebugState.lastHttp = cr.error.httpCode;
            setStatus("AI 失败：" + cr.error.message + "\n建议：" + cr.error.suggestion);
            return;
        }
        try {
            setStatus("正在解析 AI 返回...");
            AiAnalysisResult r;
            try {
                r = new AiResponseParser().parse(cr.response.content);
            } catch (Exception first) {
                String repair = new AiJsonRepairer().buildRepairUserMessage(cr.response.content, first.getMessage());
                JSONObject body = new AiPromptBuilder().buildChatRequest(s, AiSystemPrompt.TEXT, repair, false);
                AiCallResult rr = new AiClient().chat(new AiRequest(BaseUrlNormalizer.normalize(s.baseUrl).url, SecureStorage.loadApiKey(this), body.toString(), s.timeoutSeconds), false);
                if (!rr.success) throw first;
                r = new AiResponseParser().parse(rr.response.content);
            }
            new AiSuggestionSafetyFilter().filter(r);
            lastAi = r;
            AiDebugState.lastState = "SUCCESS";
            AiDebugState.lastHttp = cr.response.httpCode;
            AiDebugState.lastElapsedMs = cr.response.elapsedMs;
            AiDebugState.lastJsonOk = true;
            AiDebugState.lastSuggestionCount = r.commentSuggestions.size() + r.privateMessageSuggestions.size();
            int risks = 0;
            for (SuggestionItem it : r.commentSuggestions) if (it.blocked) risks++;
            for (SuggestionItem it : r.privateMessageSuggestions) if (it.blocked) risks++;
            AiDebugState.lastRiskCount = risks;
            saveHistory(r);
            AssistantStateCoordinator.recompute(this, true, AssistantRuntimeState.RESULT_READY, null);
            showAiCards(r);
        } catch (Exception e) {
            AiDebugState.lastState = "FAILED";
            AiDebugState.lastJsonOk = false;
            AiDebugState.lastError = safe(e);
            setStatus("AI 返回解析失败：" + safe(e));
        }
    }

    private void showAiCards(AiAnalysisResult r) {
        if (content == null) return;
        content.post(() -> {
            while (content.getChildCount() > 9) content.removeViewAt(9);
            setStatus("分析完成\n页面类型：" + (lastCls == null ? "UNKNOWN" : lastCls.pageType)
                    + "\n页面置信度：" + (lastCls == null ? 0 : lastCls.confidence)
                    + "\nAI 置信度：" + r.confidence
                    + "\n昵称：" + r.nickname
                    + "\n音乐偏好：" + r.musicPreferences
                    + "\n聊天角度：" + r.conversationAngles
                    + "\n风险：" + riskText(r));
            addSection("评论建议", r.commentSuggestions, "COMMENT");
            addSection("私信建议", r.privateMessageSuggestions, "PRIVATE_MESSAGE");
        });
    }

    private void addSection(String title, List<SuggestionItem> list, String type) {
        content.addView(label(title, 14, Color.WHITE, true));
        for (SuggestionItem it : list) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(8), dp(8), dp(8), dp(8));
            card.setBackground(Rounded.bg(Color.rgb(10, 14, 30), dp(12), Color.rgb(70, 82, 130), 1));
            TextView txt = label(it.text + "\n理由：" + it.reason + (it.blocked ? "\n风险：" + it.riskReason : ""), 12, Color.WHITE, false);
            card.addView(txt);
            LinearLayout ops = new LinearLayout(this);
            ops.setOrientation(LinearLayout.HORIZONTAL);
            ops.addView(btn("复制", v -> copyOne(it.text)), new LinearLayout.LayoutParams(0, dp(38), 1));
            if (!it.blocked) ops.addView(btn("填入", v -> confirmFill(it, type)), new LinearLayout.LayoutParams(0, dp(38), 1));
            ops.addView(btn("换一句", v -> { previous.add(it.text); requestSuggestion(type.equals("PRIVATE_MESSAGE"), true); }), new LinearLayout.LayoutParams(0, dp(38), 1));
            card.addView(ops);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, dp(6), 0, dp(8));
            content.addView(card, lp);
        }
    }

    private void confirmFill(SuggestionItem it, String type) {
        PageType pt = lastCls == null ? PageType.UNKNOWN : lastCls.pageType;
        TextFillRequest req = new TextFillRequest(it.text, type, pt, false);
        String why = KSongAccessibilityService.canFillText(this, req);
        if (!why.isEmpty()) { toast(why); return; }
        showConfirm(req);
    }

    private void showConfirm(TextFillRequest req) {
        if (confirmView != null) try { wm.removeView(confirmView); } catch (Exception ignored) {}
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(14), dp(14), dp(14), dp(14));
        v.setBackground(Rounded.bg(Color.rgb(30, 34, 55), dp(14), Color.WHITE, 1));
        v.addView(label("确认填入当前 K 歌输入框？\n\n“" + req.text + "”\n\n不会点击发送按钮，发送需你手动确认。", 13, Color.WHITE, false));
        v.addView(row(btn("取消", x -> { try { wm.removeView(confirmView); } catch (Exception ignored) {} confirmView = null; }),
                btn("确认填入", x -> {
                    String why = KSongAccessibilityService.canFillText(this, req);
                    if (!why.isEmpty()) toast(why);
                    else if (KSongAccessibilityService.fillText(req)) toast("已填入输入框，请手动确认发送");
                    else toast("填入失败，请确认焦点在 K 歌输入框");
                    try { wm.removeView(confirmView); } catch (Exception ignored) {}
                    confirmView = null;
                })));
        confirmView = v;
        WindowManager.LayoutParams lp = params(dp(320), WindowManager.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        wm.addView(confirmView, lp);
    }

    private void showConsentThen(AiAnalysisRequest.RequestType t) {
        setStatus("需要先完成 AI 设置并勾选同意。应用只发送页面文本摘要，不上传原始截图或 API Key。");
        openActivity(com.zzy.ksongfloat.ai.AiSettingsActivity.class);
    }

    private void copyOne(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("suggestion", text));
        toast("已复制到剪贴板");
    }

    private void copyText() { if (lastAi != null && !lastAi.commentSuggestions.isEmpty()) copyOne(lastAi.commentSuggestions.get(0).text); else toast("暂无建议"); }
    private void makeComment() { requestSuggestion(false, false); }
    private void makeMessage() { requestSuggestion(true, false); }
    private void changeOne() { requestSuggestion(false, true); }
    private void confirmFill() { if (lastAi != null && !lastAi.commentSuggestions.isEmpty()) confirmFill(lastAi.commentSuggestions.get(0), "COMMENT"); else toast("暂无建议"); }

    private String riskText(AiAnalysisResult r) {
        StringBuilder sb = new StringBuilder();
        for (AiRiskFlag f : r.riskFlags) { if (sb.length() > 0) sb.append("; "); sb.append(f.message); }
        return sb.length() == 0 ? "无" : sb.toString();
    }

    private void cancel() {
        generation++;
        busy = false;
        try { ScreenCaptureManager.get().cancel(); } catch (Exception ignored) {}
        try { if (ocrFuture != null) ocrFuture.cancel(true); } catch (Exception ignored) {}
        try { aiClient.cancel(); } catch (Exception ignored) {}
        setStatus("已取消当前任务");
        AssistantStateCoordinator.recompute(this, true);
    }

    private void setStatus(String s) { if (status != null) status.post(() -> status.setText(s)); }
    private LinearLayout row(View a, View b) { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setPadding(0, dp(6), 0, 0); r.addView(a, new LinearLayout.LayoutParams(0, dp(42), 1)); r.addView(b, new LinearLayout.LayoutParams(0, dp(42), 1)); return r; }
    private Button btn(String s, View.OnClickListener l) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(12); b.setTextColor(Color.WHITE); b.setBackground(Rounded.bg(Color.rgb(62, 76, 132), dp(10), 0, 0)); b.setOnClickListener(l); return b; }
    private TextView label(String s, int sp, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); t.setLineSpacing(0, 1.15f); return t; }
    private void openActivity(Class<?> c) { Intent i = new Intent(this, c); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
    private String safe(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
    private WindowManager.LayoutParams params(int w, int h) { int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE; return new WindowManager.LayoutParams(w, h, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT); }
    private void hidePanel() { if (panel != null) try { wm.removeView(panel); } catch (Exception ignored) {} panel = null; content = null; status = null; logView = null; AutomationLog.setListener(null); }
    private void update(View v, WindowManager.LayoutParams p) { try { wm.updateViewLayout(v, p); } catch (Exception ignored) {} }
    private void drag(View v, WindowManager.LayoutParams p) {
        final int[] sx = new int[1], sy = new int[1];
        final float[] dx = new float[1], dy = new float[1];
        final int slop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
        final boolean[] dragging = {false};
        v.setOnTouchListener((x, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sx[0] = p.x; sy[0] = p.y;
                    dx[0] = e.getRawX(); dy[0] = e.getRawY();
                    dragging[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int mx = (int) (e.getRawX() - dx[0]);
                    int my = (int) (e.getRawY() - dy[0]);
                    if (!dragging[0] && (Math.abs(mx) > slop || Math.abs(my) > slop)) dragging[0] = true;
                    if (dragging[0]) {
                        p.x = sx[0] + mx;
                        p.y = sy[0] + my;
                        update(x, p);
                        return true;
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return dragging[0];
            }
            return false;
        });
    }
    private void dragSnap(View v, WindowManager.LayoutParams p) { drag(v, p); }

    public void onDestroy() {
        running = false;
        if (stateObserver != null) AssistantStateRepository.remove(stateObserver);
        AssistantStateCoordinator.serviceStopped(this);
        cancel();
        AutomationOrchestrator.get().stop();
        hidePanel();
        if (confirmView != null) try { wm.removeView(confirmView); } catch (Exception ignored) {}
        if (bubble != null) try { wm.removeView(bubble); } catch (Exception ignored) {}
        try { ocr.shutdown(); } catch (Exception ignored) {}
        super.onDestroy();
    }

    private void saveHistory(AiAnalysisResult r) {
        try {
            if (!PrivacySettings.load(this).saveAnalysisHistory) return;
            InteractionRecord rec = new InteractionRecord();
            rec.nickname = r.nickname == null || r.nickname.length() == 0 ? (lastCls == null ? "" : lastCls.detectedNickname) : r.nickname;
            rec.visibleBio = r.profileSummary == null ? "" : r.profileSummary;
            rec.visibleSongs = lastCls == null ? "[]" : new org.json.JSONArray(lastCls.detectedSongTitles).toString();
            rec.pageType = lastCls == null ? "UNKNOWN" : lastCls.pageType.name();
            rec.lastAnalyzedAt = System.currentTimeMillis();
            rec.generatedComments = suggestionsToJson(r.commentSuggestions);
            rec.generatedMessages = suggestionsToJson(r.privateMessageSuggestions);
            rec.riskFlags = risksToJson(r.riskFlags);
            rec.interactionStatus = InteractionStatus.ANALYZED;
            String publicId = lastText == null ? "" : (lastText.packageName + "|" + lastText.windowTitle);
            rec.profileFingerprint = ProfileFingerprint.create(rec.nickname, rec.visibleBio, rec.visibleSongs, publicId);
            HistoryRepository.upsert(this, rec, new HistoryRepository.Callback<InteractionRecord>() {
                public void onResult(InteractionRecord value) {}
                public void onError(Exception e) { AiDebugState.lastError = "历史保存失败：" + e.getMessage(); }
            });
        } catch (Exception e) {
            AiDebugState.lastError = "历史保存失败：" + e.getMessage();
        }
    }

    private String suggestionsToJson(List<SuggestionItem> list) throws Exception {
        org.json.JSONArray arr = new org.json.JSONArray();
        if (list != null) for (SuggestionItem it : list) arr.put(it.text);
        return arr.toString();
    }

    private String risksToJson(List<AiRiskFlag> list) throws Exception {
        org.json.JSONArray arr = new org.json.JSONArray();
        if (list != null) for (AiRiskFlag f : list) arr.put(f.message);
        return arr.toString();
    }

    public static boolean isRunning() {
        return running;
    }

    private int piImmutable() { return Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0; }

    private void updateNotification(AssistantRuntimeSnapshot s) {
        if (s == null) return;
        try {
            Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "ksong_float_service") : new Notification.Builder(this);
            Intent open = new Intent(this, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent openPi = PendingIntent.getActivity(this, 2002, open, PendingIntent.FLAG_UPDATE_CURRENT | piImmutable());
            Intent stop = new Intent(this, FloatingWindowService.class);
            stop.setAction(ACTION_STOP);
            PendingIntent stopPi = PendingIntent.getService(this, 2003, stop, PendingIntent.FLAG_UPDATE_CURRENT | piImmutable());
            b.setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("K歌悬浮助手正在运行")
                    .setContentText(notificationText(s))
                    .setContentIntent(openPi)
                    .addAction(android.R.drawable.ic_menu_view, "打开", openPi)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopPi)
                    .setOngoing(true);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(2001, b.build());
        } catch (Exception ignored) {}
    }

    private String notificationText(AssistantRuntimeSnapshot s) {
        if (s == null) return "等待打开全民 K 歌";
        switch (s.assistantState) {
            case WAITING_FOR_ACCESSIBILITY: return "等待无障碍服务连接";
            case WAITING_FOR_TARGET_APP: return "等待打开全民 K 歌";
            case TARGET_APP_DETECTED: return "已检测全民 K 歌 · 页面识别中";
            case READY_TO_ANALYZE: return "当前页面：" + s.pageType + " · 等待分析";
            case CAPTURING: return "正在获取当前页面截图";
            case OCR_PROCESSING: return "正在识别当前页面文字";
            case AI_ANALYZING: return "正在生成评论和私信建议";
            case RESULT_READY: return "分析完成 · 点击查看建议";
            case ERROR: return "运行异常 · 点击查看详情";
            default: return "当前状态：" + s.assistantState;
        }
    }

    private void updateBubble(AssistantRuntimeSnapshot s) {
        if (bubbleText == null || s == null) return;
        bubbleText.post(() -> {
            int color;
            switch (s.assistantState) {
                case WAITING_FOR_ACCESSIBILITY:
                case WAITING_FOR_TARGET_APP: color = Color.rgb(120, 126, 140); break;
                case TARGET_APP_DETECTED: color = Color.rgb(40, 120, 220); break;
                case READY_TO_ANALYZE: color = Color.rgb(108, 92, 231); break;
                case AI_ANALYZING:
                case CAPTURING:
                case OCR_PROCESSING: color = Color.rgb(140, 80, 220); break;
                case RESULT_READY: color = Color.rgb(35, 150, 92); break;
                case ERROR: color = Color.rgb(210, 70, 70); break;
                default: color = Color.rgb(100, 86, 230); break;
            }
            bubbleText.setBackground(Rounded.bg(color, dp(28), Color.WHITE, 1));
        });
    }

    private void updatePanelStatus(AssistantRuntimeSnapshot s) {
        if (status != null && s != null) status.post(() -> status.setText(panelStatusText(s)));
    }

    private String panelStatusText(AssistantRuntimeSnapshot s) {
        if (s == null) return "等待状态更新";
        String current = s.currentPackageName.length() == 0 ? "暂未捕获" : s.currentPackageName;
        return "当前应用：" + current
                + "\n目标应用：" + s.targetPackageName
                + "\n当前页面：" + s.pageType + " / " + s.pageConfidence
                + "\n无障碍：" + s.accessibilityState
                + "\nAI：" + (s.aiConfigured ? "已配置" : "未配置")
                + "\n助手状态：" + s.assistantState;
    }

    private void snapBubbleToEdge() {
        int sw = getResources().getDisplayMetrics().widthPixels;
        bubbleParams.x = bubbleParams.x + dp(28) < sw / 2 ? dp(8) : Math.max(dp(8), sw - dp(64));
        limitPosition(bubbleParams);
        update(bubble, bubbleParams);
    }

    private void limitPosition(WindowManager.LayoutParams p) {
        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;
        p.x = Math.max(0, Math.min(p.x, sw - dp(56)));
        p.y = Math.max(dp(24), Math.min(p.y, sh - dp(96)));
    }

    private void showStopConfirm() {
        if (confirmView != null) try { wm.removeView(confirmView); } catch (Exception ignored) {}
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(14), dp(14), dp(14), dp(14));
        v.setBackground(Rounded.bg(Color.rgb(30, 34, 55), dp(14), Color.WHITE, 1));
        v.addView(label("停止 K歌悬浮助手？\n会取消当前分析任务，不会发送任何评论或私信。", 13, Color.WHITE, false));
        v.addView(row(btn("取消", x -> { try { wm.removeView(confirmView); } catch (Exception ignored) {} confirmView = null; }),
                btn("停止助手", x -> stopSelf())));
        confirmView = v;
        WindowManager.LayoutParams lp = params(dp(320), WindowManager.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        try { wm.addView(confirmView, lp); } catch (Exception e) { toast("停止确认面板显示失败：" + safe(e)); }
    }
}

