package com.zzy.ksongfloat;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.zzy.ksongfloat.accessibility.AccessibilityStateDetector;
import com.zzy.ksongfloat.accessibility.AccessibilityStateRepository;
import com.zzy.ksongfloat.accessibility.AccessibilityConnectionState;
import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;
import com.zzy.ksongfloat.ai.AiConfigRepository;
import com.zzy.ksongfloat.automation.AutomationLog;
import com.zzy.ksongfloat.automation.AutomationOrchestrator;
import com.zzy.ksongfloat.automation.AutomationRuntime;
import com.zzy.ksongfloat.automation.AutomationSettingsRepository;
import com.zzy.ksongfloat.floating.FloatingWindowService;
import com.zzy.ksongfloat.guide.SettingsHubActivity;
import com.zzy.ksongfloat.location.LocationStateRepository;
import com.zzy.ksongfloat.logs.LogViewerActivity;
import com.zzy.ksongfloat.navigation.AppNavigator;
import com.zzy.ksongfloat.ui.MainViewModel;
import com.zzy.ksongfloat.ui.theme.AppTheme;
import com.zzy.ksongfloat.util.PermissionUtils;
import com.zzy.ksongfloat.util.SettingsNavigator;
import com.zzy.ksongfloat.util.UiKit;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private LinearLayout root;
    private MainViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(MainViewModel.class);
        AutomationRuntime.setChangeListener(vm::refresh);
        AiConfigRepository.get().attachPreferenceListener(this);
        buildUi();
        vm.observeDashboard().observe(this, s -> buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        AccessibilityStateDetector.detect(this);
        AiConfigRepository.get().refresh(this);
        LocationStateRepository.get().refreshPermission(this);
        vm.refresh();
    }

    private void buildUi() {
        root = UiKit.root(this);
        root.removeAllViews();
        addHeader();
        addRunCard();
        addConditionCard();
        addConfigSummaryCard();
        addRecentLogsCard();
        addBottomActions();
    }

    private void addHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(UiKit.title(this, "K歌助手"));
        try {
            left.addView(UiKit.caption(this, "v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (Exception e) {
            left.addView(UiKit.caption(this, "v0.1.0"));
        }
        Button settings = UiKit.textButton(this, "设置");
        UiKit.attachClick(settings, v -> AppNavigator.open(this, SettingsHubActivity.class, false));
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(settings);
        root.addView(row);
    }

    private void addRunCard() {
        MainViewModel.DashboardState d = vm.observeDashboard().getValue();
        if (d == null) {
            vm.refresh();
            d = vm.observeDashboard().getValue();
        }
        if (d == null) return;
        final MainViewModel.DashboardState state = d;
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.text(this, "运行状态", AppTheme.BODY_SP, AppTheme.TEXT_PRIMARY, true));
        String statusText = labelStatus(state.status);
        card.addView(UiKit.caption(this, "当前状态：" + statusText));
        card.addView(UiKit.caption(this, "前台应用：" + empty(state.foregroundPackage, "未知")));
        card.addView(UiKit.caption(this, "识别页面：" + state.page));
        card.addView(UiKit.caption(this, "当前引擎：" + state.engine));
        card.addView(UiKit.caption(this, "AI：" + (state.ai.configured ? "已配置" : "未配置")));
        card.addView(UiKit.caption(this, "定位：" + state.locationStatus));
        card.addView(UiKit.caption(this, "队列待处理：" + state.queuePending));
        if (!state.currentUserName.isEmpty()) {
            card.addView(UiKit.caption(this, "当前用户：" + state.currentUserName));
        }
        card.addView(UiKit.caption(this, "最近动作：" + state.lastAction));
        card.addView(UiKit.caption(this, "已处理：" + state.processed + " · 连续失败：" + state.consecutiveFail));
        if (state.message != null && !state.message.isEmpty()) {
            card.addView(UiKit.text(this, state.message, AppTheme.CAPTION_SP, AppTheme.WARNING, false));
        }
        Button primary = primaryButton(state);
        UiKit.attachClick(primary, v -> onPrimaryAction(state));
        card.addView(primary);
        Button floatBtn = UiKit.button(this, "启动悬浮助手", AppTheme.BRAND_DARK);
        UiKit.attachClick(floatBtn, v -> startFloatingAssistant());
        card.addView(floatBtn);
        root.addView(card);
    }

    private Button primaryButton(MainViewModel.DashboardState d) {
        if (d.running && !d.paused) return UiKit.button(this, "停止自动化", AppTheme.DANGER);
        if (d.paused) return UiKit.button(this, "继续运行", AppTheme.BRAND);
        if (d.status == AutomationRuntime.UiStatus.ERROR) return UiKit.button(this, "查看错误", AppTheme.DANGER);
        return UiKit.button(this, "开始自动化", AppTheme.BRAND);
    }

    private void onPrimaryAction(MainViewModel.DashboardState d) {
        if (d.running && !d.paused) stopAutomation();
        else if (d.paused) {
            AutomationOrchestrator.get().resume();
            vm.refresh();
        } else if (d.status == AutomationRuntime.UiStatus.ERROR) {
            toast(d.message.isEmpty() ? AutomationRuntime.getPauseReason() : d.message);
        } else startAutomationChecked();
    }

    private void addConditionCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.text(this, "运行条件", AppTheme.BODY_SP, AppTheme.TEXT_PRIMARY, true));
        addConditionRow(card, "无障碍服务", AccessibilityStateRepository.state() == AccessibilityConnectionState.CONNECTED, "去开启", () -> SettingsNavigator.openAccessibility(this));
        addConditionRow(card, "悬浮窗权限", PermissionUtils.canDrawOverlays(this), "去开启", () -> SettingsNavigator.openOverlay(this));
        addConditionRow(card, "通知权限", PermissionUtils.isNotificationEnabled(this), "去授权", () -> {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 3301);
            else SettingsNavigator.openNotification(this);
        });
        boolean aiOk = AiConfigRepository.get().isConfigured();
        addConditionRow(card, "AI 接口", aiOk, aiOk ? null : "去设置", () -> AppNavigator.open(this, com.zzy.ksongfloat.ai.AiSettingsActivity.class, false));
        addConditionRow(card, "虚拟定位", vm.observeDashboard().getValue() != null
                && vm.observeDashboard().getValue().locationStatus.contains("模拟"), "去设置",
                () -> AppNavigator.open(this, com.zzy.ksongfloat.location.LocationSettingsActivity.class, false));
        root.addView(card);
    }

    private void addConditionRow(LinearLayout card, String name, boolean ok, String action, Runnable open) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView tv = UiKit.text(this, name, AppTheme.BODY_SP, ok ? AppTheme.SUCCESS : AppTheme.WARNING, false);
        row.addView(tv, new LinearLayout.LayoutParams(0, -2, 1));
        if (ok) row.addView(UiKit.text(this, "正常", AppTheme.CAPTION_SP, AppTheme.SUCCESS, true));
        else if (action != null) {
            Button b = UiKit.textButton(this, action);
            UiKit.attachClick(b, v -> open.run());
            row.addView(b);
        }
        card.addView(row);
    }

    private void addConfigSummaryCard() {
        var s = AutomationSettingsRepository.load(this);
        var ai = AiConfigRepository.get().getCurrentState();
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.text(this, "自动化配置", AppTheme.BODY_SP, AppTheme.TEXT_PRIMARY, true));
        card.addView(UiKit.caption(this, "延迟：" + s.delayMinSec() + " ~ " + s.delayMaxSec() + " 秒"));
        card.addView(UiKit.caption(this, "单轮/任务上限：" + s.maxUsersPerSession + " / " + s.maxUsersPerTask));
        card.addView(UiKit.caption(this, "已启用：" + s.enabledActionsSummary()));
        card.addView(UiKit.caption(this, "测试模式：" + (s.testMode ? "开启" : "关闭")));
        card.addView(UiKit.caption(this, "AI 模型：" + (ai.model.isEmpty() ? "未配置" : ai.model)));
        Button edit = UiKit.button(this, "修改自动化设置", AppTheme.BRAND);
        UiKit.attachClick(edit, v -> AppNavigator.open(this, com.zzy.ksongfloat.automation.AutomationSettingsActivity.class, false));
        card.addView(edit);
        root.addView(card);
    }

    private void addRecentLogsCard() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.text(this, "最近日志", AppTheme.BODY_SP, AppTheme.TEXT_PRIMARY, true));
        List<String> logs = AutomationLog.recent(3);
        if (logs.isEmpty()) card.addView(UiKit.caption(this, "暂无日志"));
        else for (String line : logs) card.addView(UiKit.caption(this, line));
        Button all = UiKit.textButton(this, "查看全部日志");
        UiKit.attachClick(all, v -> AppNavigator.open(this, LogViewerActivity.class, false));
        card.addView(all);
        root.addView(card);
    }

    private void addBottomActions() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button settings = UiKit.button(this, "设置", AppTheme.BRAND_DARK);
        Button help = UiKit.button(this, "帮助与诊断", AppTheme.BRAND_DARK);
        UiKit.attachClick(settings, v -> AppNavigator.open(this, SettingsHubActivity.class, false));
        UiKit.attachClick(help, v -> AppNavigator.open(this, com.zzy.ksongfloat.diagnostics.DiagnosticsActivity.class, false));
        row.addView(settings, new LinearLayout.LayoutParams(0, UiKit.dp(this, AppTheme.BUTTON_HEIGHT_DP), 1));
        row.addView(help, new LinearLayout.LayoutParams(0, UiKit.dp(this, AppTheme.BUTTON_HEIGHT_DP), 1));
        root.addView(row);
    }

    private void startAutomationChecked() {
        if (!PermissionUtils.canDrawOverlays(this)) {
            toast("请先开启悬浮窗权限");
            SettingsNavigator.openOverlay(this);
            return;
        }
        startFloatingAssistant();
        AutomationOrchestrator.get().start(getApplicationContext());
        toast("强行自动化已启动");
        vm.refresh();
    }

    private void startFloatingAssistant() {
        if (!PermissionUtils.canDrawOverlays(this)) {
            SettingsNavigator.openOverlay(this);
            return;
        }
        Intent i = new Intent(this, FloatingWindowService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    private void stopAutomation() {
        AutomationOrchestrator.get().stop();
        vm.refresh();
    }

    private static String labelStatus(AutomationRuntime.UiStatus st) {
        if (st == AutomationRuntime.UiStatus.RUNNING) return "运行中";
        if (st == AutomationRuntime.UiStatus.PAUSED) return "已暂停";
        if (st == AutomationRuntime.UiStatus.ERROR) return "错误";
        return "未运行";
    }

    private static String empty(String s, String fallback) {
        return s == null || s.isEmpty() ? fallback : s;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
