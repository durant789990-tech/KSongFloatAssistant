package com.zzy.ksongfloat.guide;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.graphics.Color;

import com.zzy.ksongfloat.navigation.AppNavigator;
import com.zzy.ksongfloat.ui.components.IosNavigationBar;
import com.zzy.ksongfloat.util.UiKit;

public class UsageGuideActivity extends Activity {
    private int page = 0;
    private LinearLayout root;
    private final String[] titles = {"人工操作辅助工具", "页面公开文字分析", "填入后仍需手动发送"};
    private final String[] texts = {
            "这是一个人工操作辅助工具，不会自动发送评论、私信、点赞或送礼。",
            "分析时会读取当前全民 K 歌页面的公开文字，并根据你的 AI 设置生成互动建议。",
            "评论和私信必须由你检查确认。点击填入只会写入输入框，发送仍需你手动点击。"
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        render();
    }

    private void render() {
        root = UiKit.root(this);
        IosNavigationBar nav = new IosNavigationBar(this, "使用说明", "");
        UiKit.attachClick(nav.left, v -> AppNavigator.finish(this));
        root.addView(nav);
        root.addView(UiKit.title(this, titles[page]));
        root.addView(UiKit.text(this, texts[page], 16, Color.rgb(55,65,88), false));
        UiKit.addGap(root, this, 18);
        Button next = UiKit.button(this, page == 2 ? "开始配置" : "下一页", Color.rgb(108,92,231));
        UiKit.attachClick(next, v -> { if (page < 2) { page++; render(); } else { getSharedPreferences("first_run", MODE_PRIVATE).edit().putBoolean("guide_seen", true).apply(); AppNavigator.finish(this); }});
        root.addView(next);
    }
}
