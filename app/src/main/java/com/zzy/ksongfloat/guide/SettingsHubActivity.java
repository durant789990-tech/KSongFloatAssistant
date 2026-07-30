package com.zzy.ksongfloat.guide;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.zzy.ksongfloat.DebugActivity;
import com.zzy.ksongfloat.ai.AiSettingsActivity;
import com.zzy.ksongfloat.automation.AutomationSettingsActivity;
import com.zzy.ksongfloat.diagnostics.DiagnosticsActivity;
import com.zzy.ksongfloat.history.HistoryActivity;
import com.zzy.ksongfloat.navigation.AppNavigator;
import com.zzy.ksongfloat.privacy.PrivacySettingsActivity;
import com.zzy.ksongfloat.testcheck.DeviceTestChecklistActivity;
import com.zzy.ksongfloat.ui.theme.AppTheme;
import com.zzy.ksongfloat.util.UiKit;

public class SettingsHubActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = UiKit.root(this);
        root.addView(UiKit.title(this, "设置"));
        addLink(root, "AI 接口设置", AiSettingsActivity.class);
        addLink(root, "自动化设置", AutomationSettingsActivity.class);
        addLink(root, "权限向导", PermissionWizardActivity.class);
        addLink(root, "页面识别调试", DebugActivity.class);
        addLink(root, "互动历史", HistoryActivity.class);
        addLink(root, "使用说明", UsageGuideActivity.class);
        addLink(root, "隐私设置", PrivacySettingsActivity.class);
        addLink(root, "问题诊断", DiagnosticsActivity.class);
        addLink(root, "真机测试清单", DeviceTestChecklistActivity.class);
    }

    private void addLink(LinearLayout root, String label, Class<?> cls) {
        android.widget.Button b = UiKit.button(this, label, AppTheme.BRAND_DARK);
        UiKit.attachClick(b, v -> AppNavigator.open(this, cls, false));
        root.addView(b);
    }
}
