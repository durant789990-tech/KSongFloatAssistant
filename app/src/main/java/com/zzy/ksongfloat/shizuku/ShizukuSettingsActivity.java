package com.zzy.ksongfloat.shizuku;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.zzy.ksongfloat.ui.theme.AppTheme;
import com.zzy.ksongfloat.util.UiKit;

/** 已禁用入口，保留类避免旧链接崩溃。 */
public class ShizukuSettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = UiKit.root(this);
        root.addView(UiKit.title(this, "Shizuku 已禁用"));
        root.addView(UiKit.caption(this, "当前版本自动化仅使用无障碍服务，不再使用 Shizuku Shell 手势。"));
        root.addView(UiKit.caption(this, "状态：" + ShizukuHelper.statusLabel(this)));
    }
}
