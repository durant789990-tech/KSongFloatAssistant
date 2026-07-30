package com.zzy.ksongfloat.guide;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zzy.ksongfloat.ai.AiSettingsActivity;
import com.zzy.ksongfloat.navigation.AppNavigator;
import com.zzy.ksongfloat.ui.components.IosNavigationBar;
import com.zzy.ksongfloat.util.PermissionUtils;
import com.zzy.ksongfloat.util.SettingsNavigator;
import com.zzy.ksongfloat.util.UiKit;
import com.zzy.ksongfloat.xiaomi.XiaomiPermissionHelper;

public class PermissionWizardActivity extends Activity {
    private static final String PREF = "permission_wizard";
    private static final String KEY_XIAOMI_MANUAL = "xiaomi_manual_ok";
    private int step = 0;
    private LinearLayout root;
    private final String[] names = {"悬浮窗权限", "无障碍服务", "通知权限", "忽略电池优化", "小米后台运行说明", "AI 接口配置"};

    @Override protected void onCreate(Bundle b) { super.onCreate(b); render(); }
    @Override protected void onResume() { super.onResume(); render(); }

    private void render() {
        root = UiKit.root(this);
        IosNavigationBar nav = new IosNavigationBar(this, "权限向导", "");
        UiKit.attachClick(nav.left, v -> AppNavigator.finish(this));
        root.addView(nav);
        root.addView(UiKit.text(this, "第 " + (step + 1) + " 步 / 6：" + names[step], 16, Color.rgb(20,26,44), true));
        UiKit.addGap(root, this, 8);
        root.addView(UiKit.text(this, desc(), 14, Color.rgb(70,78,100), false));
        UiKit.addGap(root, this, 8);
        TextView status = UiKit.text(this, "当前状态：" + statusText(), 15, ok() ? Color.rgb(20,100,55) : Color.rgb(165,82,0), true);
        root.addView(status);

        Button setting = UiKit.button(this, step == 5 ? "打开 AI 设置" : "去设置", Color.rgb(96,112,170));
        UiKit.attachClick(setting, v -> openSetting());
        root.addView(setting);

        if (step == 4) {
            Button manual = UiKit.button(this, "手动确认已开启", Color.rgb(35, 130, 92));
            UiKit.attachClick(manual, v -> {
                getSharedPreferences(PREF, MODE_PRIVATE).edit().putBoolean(KEY_XIAOMI_MANUAL, true).apply();
                render();
            });
            root.addView(manual);
        }

        Button skip = UiKit.button(this, "暂时跳过", Color.rgb(92,101,125));
        UiKit.attachClick(skip, v -> next());
        root.addView(skip);

        Button next = UiKit.button(this, step == 5 ? "完成" : "下一步", Color.rgb(108,92,231));
        UiKit.attachClick(next, v -> next());
        root.addView(next);
        setContentView(root);
    }

    private String desc() {
        switch (step) {
            case 0: return "用途：显示悬浮球和分析面板。必要权限。";
            case 1: return "用途：读取当前页面公开文字，并在你确认后填入输入框。必要权限。";
            case 2: return "用途：前台服务通知。Android 13 及以上为必要权限。";
            case 3: return "用途：降低系统杀后台概率。可选权限。";
            case 4: return XiaomiPermissionHelper.guideText()
                    + "\n\nHyperOS/MIUI 无法自动检测自启动与后台弹出界面，请手动开启后点「手动确认已开启」。";
            default: return "用途：配置 OpenAI 兼容接口。AI 分析需要；本地页面识别可不配置。";
        }
    }

    private boolean ok() {
        switch (step) {
            case 0: return PermissionUtils.canDrawOverlays(this);
            case 1: return PermissionUtils.isAccessibilityEnabled(this);
            case 2: return PermissionUtils.isNotificationEnabled(this);
            case 3: return PermissionUtils.isBatteryIgnoring(this);
            case 4: return getSharedPreferences(PREF, MODE_PRIVATE).getBoolean(KEY_XIAOMI_MANUAL, false);
            default: return com.zzy.ksongfloat.security.SecureStorage.hasApiKey(this)
                    && com.zzy.ksongfloat.ai.AiSettingsRepository.load(this).isUsable();
        }
    }

    private String statusText() {
        if (step == 4) {
            return ok() ? "已手动确认" : "请按说明开启后手动确认";
        }
        return ok() ? "已开启" : "未开启";
    }

    private void openSetting() {
        switch (step) {
            case 0: SettingsNavigator.openOverlay(this); break;
            case 1: SettingsNavigator.openAccessibility(this); break;
            case 2:
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 3301);
                } else {
                    SettingsNavigator.openNotification(this);
                }
                break;
            case 3: SettingsNavigator.openBattery(this); break;
            case 4: XiaomiPermissionHelper.openAutoStart(this); break;
            default: AppNavigator.open(this, AiSettingsActivity.class, false); break;
        }
    }

    private void next() {
        if (step < 5) { step++; render(); }
        else {
            getSharedPreferences(PREF, MODE_PRIVATE).edit().putBoolean("wizard_seen", true).apply();
            finish();
        }
    }
}
