package com.zzy.ksongfloat.diagnostics;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;

import com.zzy.ksongfloat.DebugState;
import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;
import com.zzy.ksongfloat.accessibility.AccessibilityStateDetector;
import com.zzy.ksongfloat.accessibility.AccessibilityStateRepository;
import com.zzy.ksongfloat.ai.AiDebugState;
import com.zzy.ksongfloat.ai.AiSettingsRepository;
import com.zzy.ksongfloat.classifier.PageClassificationResult;
import com.zzy.ksongfloat.config.TargetAppConfig;
import com.zzy.ksongfloat.navigation.AppNavigator;
import com.zzy.ksongfloat.runtime.AssistantRuntimeSnapshot;
import com.zzy.ksongfloat.runtime.AssistantStateCoordinator;
import com.zzy.ksongfloat.runtime.AssistantStateRepository;
import com.zzy.ksongfloat.runtime.ForegroundAppDetector;
import com.zzy.ksongfloat.security.SecureStorage;
import com.zzy.ksongfloat.ui.components.IosNavigationBar;
import com.zzy.ksongfloat.util.PermissionUtils;
import com.zzy.ksongfloat.util.UiKit;

public class DiagnosticsActivity extends Activity {
    TextView out;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = UiKit.root(this);
        IosNavigationBar nav = new IosNavigationBar(this, "问题诊断", "");
        UiKit.attachClick(nav.left, v -> AppNavigator.finish(this));
        root.addView(nav);
        Button refresh = UiKit.button(this, "重新检测", Color.rgb(108,92,231));
        UiKit.attachClick(refresh, v -> show());
        root.addView(refresh);
        Button copy = UiKit.button(this, "复制诊断摘要", Color.rgb(96,112,170));
        UiKit.attachClick(copy, v -> copy());
        root.addView(copy);
        Button clear = UiKit.button(this, "清除错误记录", Color.rgb(92,101,125));
        UiKit.attachClick(clear, v -> { AiDebugState.clear(); DebugState.clear(); show(); toast("已清除"); });
        root.addView(clear);
        out = UiKit.text(this, "", 13, Color.rgb(35,42,62), false);
        out.setTextIsSelectable(true);
        root.addView(out);
        show();
    }

    private void show() { out.setText(summary()); }

    private String summary() {
        AssistantStateCoordinator.recompute(this, com.zzy.ksongfloat.floating.FloatingWindowService.isRunning());
        AssistantRuntimeSnapshot rt = AssistantStateRepository.get();
        PageClassificationResult cls = DebugState.cls();
        return "Android 版本：" + android.os.Build.VERSION.RELEASE
                + "\n手机厂商：" + android.os.Build.MANUFACTURER
                + "\n设备型号：" + android.os.Build.MODEL
                + "\nApp 版本：0.1.0"
                + "\n悬浮窗权限：" + yn(PermissionUtils.canDrawOverlays(this))
                + "\n无障碍系统授权状态：" + yn(AccessibilityStateDetector.isSystemEnabled(this))
                + "\n无障碍真实连接状态：" + AccessibilityStateRepository.displayText()
                + "\n通知权限：" + yn(PermissionUtils.isNotificationEnabled(this))
                + "\n电池优化状态：" + (PermissionUtils.isBatteryIgnoring(this) ? "已忽略" : "未忽略")
                + "\n前台服务状态：" + (rt.serviceRunning ? "运行中" : "未运行")
                + "\n当前前台包名：" + rt.currentPackageName
                + "\n目标包名：" + TargetAppConfig.getTargetPackage(this)
                + "\n是否匹配：" + yn(rt.targetAppDetected)
                + "\n当前页面类型：" + rt.pageType
                + "\n页面置信度：" + rt.pageConfidence
                + "\nAI 是否配置：" + (AiSettingsRepository.load(this).isUsable() && SecureStorage.hasApiKey(this) ? "已配置" : "未配置")
                + "\n最近请求状态：" + AiDebugState.lastState
                + "\n最近错误摘要：" + safe(AiDebugState.lastError, 120)
                + "\n最近 HTTP：" + AiDebugState.lastHttp
                + "\n最近 OCR 状态：" + (DebugState.page() == null ? "暂无" : (DebugState.page().ocrAvailable ? "可用" : "不可用"))
                + "\n最近页面分类：" + (cls == null ? "暂无" : cls.pageType + " / " + cls.confidence)
                + "\n最近无障碍事件时间：" + AccessibilityStateRepository.lastEventAt()
                + "\n最近窗口事件类型：" + ForegroundAppDetector.lastEventType();
    }

    private void copy() {
        ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("diagnostics", summary()));
        toast("诊断摘要已复制，不包含 API Key、完整页面文字或凭证");
    }

    private String yn(boolean b) { return b ? "已开启" : "未开启"; }
    private String safe(String s, int n) { if (s == null) return ""; return s.length() > n ? s.substring(0, n) + "..." : s; }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
