package com.zzy.ksongfloat.logs;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zzy.ksongfloat.automation.AutomationLog;
import com.zzy.ksongfloat.ui.theme.AppTheme;
import com.zzy.ksongfloat.util.UiKit;

public class LogViewerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = UiKit.root(this);
        root.addView(UiKit.title(this, "全部日志"));
        TextView tv = UiKit.text(this, "", AppTheme.CAPTION_SP, AppTheme.TEXT_SECONDARY, false);
        StringBuilder sb = new StringBuilder();
        for (String line : AutomationLog.snapshot()) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        tv.setText(sb.length() == 0 ? "暂无日志" : sb.toString());
        root.addView(tv);
    }
}
