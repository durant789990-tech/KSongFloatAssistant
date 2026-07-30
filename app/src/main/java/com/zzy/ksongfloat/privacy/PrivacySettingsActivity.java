package com.zzy.ksongfloat.privacy;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;

import com.zzy.ksongfloat.ai.AiClient;
import com.zzy.ksongfloat.ai.AiSettingsRepository;
import com.zzy.ksongfloat.history.HistoryRepository;
import com.zzy.ksongfloat.navigation.AppNavigator;
import com.zzy.ksongfloat.ui.components.IosNavigationBar;
import com.zzy.ksongfloat.util.UiKit;

import java.io.File;

public class PrivacySettingsActivity extends Activity {
    CheckBox ocr, acc, deleteShot, history, hideFloat;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = UiKit.root(this);
        IosNavigationBar nav = new IosNavigationBar(this, "隐私设置", "");
        UiKit.attachClick(nav.left, v -> AppNavigator.finish(this));
        root.addView(nav);
        PrivacySettings s = PrivacySettings.load(this);
        ocr = cb("允许发送 OCR 文字摘要", s.allowOcrText);
        acc = cb("允许发送无障碍文字摘要", s.allowAccessibilityText);
        deleteShot = cb("自动删除截图缓存", s.autoDeleteScreenshots);
        history = cb("保存分析历史", s.saveAnalysisHistory);
        hideFloat = cb("分析时自动隐藏悬浮窗", s.autoHideFloatingWindow);
        root.addView(ocr); root.addView(acc); root.addView(deleteShot); root.addView(history); root.addView(hideFloat);

        Button save = UiKit.button(this, "保存隐私设置", Color.rgb(108,92,231));
        UiKit.attachClick(save, v -> save());
        root.addView(save);

        Button revoke = UiKit.button(this, "撤回 AI 数据发送同意", Color.rgb(168,72,72));
        UiKit.attachClick(revoke, v -> {
            AiSettingsRepository.setConsent(this, false);
            new AiClient().cancel();
            toast("已撤回同意，后续不会进行网络 AI 分析，本地识别仍可使用");
        });
        root.addView(revoke);

        Button clearHistory = UiKit.button(this, "清空全部历史", Color.rgb(168,72,72));
        UiKit.attachClick(clearHistory, v -> new AlertDialog.Builder(this).setTitle("确认清空历史").setMessage("不会删除 API Key。")
                .setNegativeButton("取消", null).setPositiveButton("清空", (d,w) -> HistoryRepository.clear(this, boolCb("历史已清空"))).show());
        root.addView(clearHistory);

        Button clearCache = UiKit.button(this, "清空截图缓存", Color.rgb(92,101,125));
        UiKit.attachClick(clearCache, v -> { int n = clearCacheFiles(getCacheDir()); toast("已清理缓存文件：" + n); });
        root.addView(clearCache);
    }

    private CheckBox cb(String text, boolean checked) { CheckBox c = new CheckBox(this); c.setText(text); c.setTextSize(15); c.setChecked(checked); return c; }

    private void save() {
        PrivacySettings s = new PrivacySettings();
        s.allowOcrText = ocr.isChecked();
        s.allowAccessibilityText = acc.isChecked();
        s.autoDeleteScreenshots = deleteShot.isChecked();
        s.saveAnalysisHistory = history.isChecked();
        s.autoHideFloatingWindow = hideFloat.isChecked();
        PrivacySettings.save(this, s);
        toast("隐私设置已保存");
    }

    private int clearCacheFiles(File dir) {
        int count = 0;
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isDirectory()) count += clearCacheFiles(f);
            else if (f.getName().startsWith("screen_") || f.getName().endsWith(".png")) {
                if (f.delete()) count++;
            }
        }
        return count;
    }

    private HistoryRepository.Callback<Boolean> boolCb(String ok) {
        return new HistoryRepository.Callback<Boolean>() {
            public void onResult(Boolean value) { runOnUiThread(() -> toast(ok)); }
            public void onError(Exception e) { runOnUiThread(() -> toast("操作失败：" + e.getMessage())); }
        };
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
