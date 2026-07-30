package com.zzy.ksongfloat.ai;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zzy.ksongfloat.ai.model.AiAnalysisResult;
import com.zzy.ksongfloat.navigation.AppNavigator;
import com.zzy.ksongfloat.security.SecureStorage;
import com.zzy.ksongfloat.ui.components.IosNavigationBar;
import com.zzy.ksongfloat.util.UiKit;

public class AiDebugActivity extends Activity {
    TextView out;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(24, 24, 24, 24);
        out = new TextView(this);
        out.setTextIsSelectable(true);

        IosNavigationBar nav = new IosNavigationBar(this, "AI 调试", "");
        UiKit.attachClick(nav.left, v -> AppNavigator.finish(this));

        Button settings = new Button(this);
        settings.setText("打开 AI 设置");
        settings.setAllCaps(false);
        UiKit.attachClick(settings, v -> AppNavigator.aiSettings(this));

        Button parse = new Button(this);
        parse.setText("测试 JSON 解析和风控");
        parse.setAllCaps(false);
        UiKit.attachClick(parse, v -> parse());

        Button clear = new Button(this);
        clear.setText("清空 AI 调试状态");
        clear.setAllCaps(false);
        UiKit.attachClick(clear, v -> { AiDebugState.clear(); show(); });

        r.addView(nav);
        r.addView(settings);
        r.addView(parse);
        r.addView(clear);
        r.addView(out);
        setContentView(r);
        show();
    }

    void show() {
        AiSettings s = AiSettingsRepository.load(this);
        out.setText("Base URL 可用：" + BaseUrlNormalizer.normalize(s.baseUrl).ok
                + "\n模型：" + s.model
                + "\nAPI Key：" + (SecureStorage.hasApiKey(this) ? "已保存" : "未保存")
                + "\n最近 HTTP：" + AiDebugState.lastHttp
                + "\n最近耗时：" + AiDebugState.lastElapsedMs
                + "\n状态：" + AiDebugState.lastState
                + "\nJSON 正常：" + AiDebugState.lastJsonOk
                + "\n建议数量：" + AiDebugState.lastSuggestionCount
                + "\n风险数量：" + AiDebugState.lastRiskCount
                + "\n错误：" + AiDebugState.lastError);
    }

    void parse() {
        try {
            String j = "{\"pageType\":\"USER_PROFILE\",\"nickname\":\"test\",\"profileSummary\":\"summary\",\"musicPreferences\":[\"pop\"],\"conversationAngles\":[\"song\"],\"commentSuggestions\":[{\"text\":\"good\",\"reason\":\"safe\"}],\"privateMessageSuggestions\":[{\"text\":\"hello\",\"reason\":\"safe\"}],\"riskFlags\":[],\"confidence\":0.8}";
            AiAnalysisResult r = new AiResponseParser().parse(j);
            new AiSuggestionSafetyFilter().filter(r);
            out.setText("解析成功：评论建议 " + r.commentSuggestions.size() + " 条，私信建议 " + r.privateMessageSuggestions.size() + " 条");
        } catch (Exception e) {
            out.setText("解析失败：" + e.getMessage());
        }
    }
}
