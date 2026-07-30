package com.zzy.ksongfloat;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.zzy.ksongfloat.accessibility.AccessibilitySnapshot;
import com.zzy.ksongfloat.accessibility.AccessibilitySnapshotRepository;
import com.zzy.ksongfloat.capture.CapturedImage;
import com.zzy.ksongfloat.capture.OcrEngine;
import com.zzy.ksongfloat.capture.OcrResult;
import com.zzy.ksongfloat.capture.PageTextCollector;
import com.zzy.ksongfloat.capture.PageTextResult;
import com.zzy.ksongfloat.capture.ScreenCaptureManager;
import com.zzy.ksongfloat.classifier.PageClassificationResult;
import com.zzy.ksongfloat.classifier.PageClassifier;
import com.zzy.ksongfloat.config.TargetAppConfig;
import com.zzy.ksongfloat.navigation.AppNavigator;
import com.zzy.ksongfloat.ui.components.IosNavigationBar;
import com.zzy.ksongfloat.util.UiKit;

public class DebugActivity extends Activity {
    private TextView out;
    private TextView targetStatus;
    private EditText targetPackage;
    private final OcrEngine ocr = new OcrEngine();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(245, 247, 252));

        IosNavigationBar nav = new IosNavigationBar(this, "页面识别调试", "");
        UiKit.attachClick(nav.left, v -> AppNavigator.finish(this));
        root.addView(nav);

        buildTargetPackagePanel(root);

        root.addView(btn("查看无障碍快照", v -> showSnapshot()));
        root.addView(btn("截图并 OCR 识别", v -> captureOcr()));
        root.addView(btn("仅用当前文本分类", v -> classifyNow()));
        root.addView(btn("清空调试状态", v -> {
            DebugState.clear();
            out.setText("已清空");
        }));

        out = tv("", 12, false);
        out.setTextIsSelectable(true);
        ScrollView sv = new ScrollView(this);
        sv.addView(out);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        showCurrent();
    }

    private void buildTargetPackagePanel(LinearLayout root) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(Rounded.bg(Color.WHITE, dp(14), Color.rgb(226, 231, 242), 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(10), 0, dp(12));
        root.addView(box, lp);

        TextView label = tv("目标 App 包名", 14, true);
        box.addView(label);

        targetStatus = tv("", 12, false);
        targetStatus.setTextColor(Color.rgb(90, 98, 120));
        box.addView(targetStatus);

        targetPackage = new EditText(this);
        targetPackage.setSingleLine(true);
        targetPackage.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        targetPackage.setHint(TargetAppConfig.DEFAULT_PACKAGE);
        targetPackage.setText(TargetAppConfig.getTargetPackage(this));
        box.addView(targetPackage, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(btn("保存包名", v -> saveTargetPackage()), new LinearLayout.LayoutParams(0, dp(42), 1));
        row.addView(btn("恢复默认", v -> resetTargetPackage()), new LinearLayout.LayoutParams(0, dp(42), 1));
        box.addView(row);
        refreshTargetStatus();
    }

    private void saveTargetPackage() {
        String normalized = TargetAppConfig.normalizePackageName(targetPackage.getText().toString());
        if (normalized.length() == 0) {
            toast("包名不能为空");
            return;
        }
        TargetAppConfig.saveTargetPackage(this, normalized);
        targetPackage.setText(TargetAppConfig.getTargetPackage(this));
        refreshTargetStatus();
        toast("目标包名已保存");
    }

    private void resetTargetPackage() {
        TargetAppConfig.resetTargetPackage(this);
        targetPackage.setText(TargetAppConfig.getTargetPackage(this));
        refreshTargetStatus();
        toast("已恢复默认包名");
    }

    private void refreshTargetStatus() {
        String foreground = com.zzy.ksongfloat.accessibility.KSongAccessibilityService.getForegroundPackage();
        boolean matched = TargetAppConfig.matches(this, foreground);
        targetStatus.setText("当前目标：" + TargetAppConfig.getTargetPackage(this)
                + "\n前台包名：" + (foreground.length() == 0 ? "暂未捕获" : foreground)
                + "\n匹配状态：" + (matched ? "匹配" : "未匹配"));
    }

    private Button btn(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        UiKit.attachClick(b, l);
        return b;
    }

    private TextView tv(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Color.rgb(30, 30, 30));
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    private void showCurrent() {
        PageTextResult p = DebugState.page();
        PageClassificationResult c = DebugState.cls();
        out.setText(format(p, c));
        refreshTargetStatus();
    }

    private void showSnapshot() {
        AccessibilitySnapshot s = AccessibilitySnapshotRepository.get().latest();
        if (s == null) {
            out.setText("暂无无障碍快照");
            return;
        }
        out.setText("包名: " + s.packageName
                + "\n采集时间: " + s.capturedAt
                + "\n目标匹配: " + TargetAppConfig.matches(this, s.packageName)
                + "\n文本: " + s.allVisibleTexts
                + "\nresourceIds: " + s.resourceIds
                + "\ncontentDesc: " + s.contentDescriptions);
        refreshTargetStatus();
    }

    private void captureOcr() {
        out.setText("正在申请单次截图...");
        ScreenCaptureManager.get().requestSingleCapture(this, new ScreenCaptureManager.CaptureCallback() {
            public void onStatus(ScreenCaptureManager.Status st, String m) {
                runOnUiThread(() -> out.setText(m));
            }

            public void onSuccess(CapturedImage img) {
                try {
                    OcrResult r = ocr.recognizeAsync(img.getBitmap()).get();
                    img.release(true);
                    AccessibilitySnapshot s = AccessibilitySnapshotRepository.get().latest();
                    PageTextResult p = new PageTextCollector().collect(s, r, img.capturedAt, true);
                    PageClassificationResult c = new PageClassifier().classify(p);
                    DebugState.update(p, c, true);
                    runOnUiThread(() -> out.setText(format(p, c)));
                } catch (Exception e) {
                    runOnUiThread(() -> out.setText("识别失败: " + safe(e)));
                }
            }

            public void onError(ScreenCaptureManager.Status st, String m) {
                runOnUiThread(() -> out.setText(m));
            }
        });
    }

    private void classifyNow() {
        AccessibilitySnapshot s = AccessibilitySnapshotRepository.get().latest();
        PageTextResult p = new PageTextCollector().collect(s, null, 0, true);
        PageClassificationResult c = new PageClassifier().classify(p);
        DebugState.update(p, c, true);
        out.setText(format(p, c));
        refreshTargetStatus();
    }

    private String format(PageTextResult p, PageClassificationResult c) {
        if (p == null) return "暂无页面识别结果";
        StringBuilder sb = new StringBuilder();
        sb.append("目标包名:\n").append(TargetAppConfig.getTargetPackage(this));
        sb.append("\n当前包名:\n").append(p.packageName);
        sb.append("\n目标匹配:\n").append(TargetAppConfig.matches(this, p.packageName));
        sb.append("\n页面类型:\n").append(c == null ? "" : c.pageType);
        sb.append("\n置信度:\n").append(c == null ? "" : c.confidence);
        sb.append("\n证据:\n").append(c == null ? "" : c.evidence);
        sb.append("\n截图是否已删除:\n").append(DebugState.screenshotDeleted());
        sb.append("\n\n无障碍文本:\n").append(p.accessibilityText);
        sb.append("\n\nOCR 文本:\n").append(p.ocrText);
        sb.append("\n\n合并文本:\n").append(p.mergedText);
        sb.append("\n\nresource-id:\n").append(p.resourceIds);
        sb.append("\ncontentDescription:\n").append(p.contentDescriptions);
        return sb.toString();
    }

    private String safe(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        ocr.shutdown();
    }
}
