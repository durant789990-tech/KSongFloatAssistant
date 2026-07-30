package com.zzy.ksongfloat.testcheck;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;

import com.zzy.ksongfloat.navigation.AppNavigator;
import com.zzy.ksongfloat.ui.components.IosNavigationBar;
import com.zzy.ksongfloat.util.UiKit;

public class DeviceTestChecklistActivity extends Activity {
    private LinearLayout list;
    private SharedPreferences sp;
    private final String[] items = {
            "悬浮窗显示和拖动", "无障碍文字读取", "MediaProjection 单帧截图", "ML Kit 中文 OCR",
            "用户主页分类", "作品页分类", "评论输入页分类", "私信输入页分类",
            "AI 连接测试", "AI 页面分析", "评论填入", "私信填入",
            "关闭悬浮窗后任务停止", "小米后台运行", "重启手机后权限状态"
    };
    private final String[] states = {"未测试", "通过", "失败", "需要调整"};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("device_test_checklist", MODE_PRIVATE);
        LinearLayout root = UiKit.root(this);
        IosNavigationBar nav = new IosNavigationBar(this, "真机测试清单", "");
        UiKit.attachClick(nav.left, v -> AppNavigator.finish(this));
        root.addView(nav);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        render();
    }

    private void render() {
        list.removeAllViews();
        for (int i = 0; i < items.length; i++) {
            int index = i;
            LinearLayout card = UiKit.card(this);
            String state = sp.getString("state_" + i, states[0]);
            String note = sp.getString("note_" + i, "");
            card.addView(UiKit.text(this, (i + 1) + ". " + items[i] + "\n状态：" + state + (note.length() == 0 ? "" : "\n备注：" + note), 14, Color.rgb(35,42,62), false));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(btn("状态", v -> chooseState(index)), new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1));
            row.addView(btn("备注", v -> editNote(index)), new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1));
            card.addView(row);
            list.addView(card);
        }
    }

    private Button btn(String s, android.view.View.OnClickListener l) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); UiKit.attachClick(b, l); return b; }
    private void chooseState(int i) { new AlertDialog.Builder(this).setTitle(items[i]).setItems(states, (d,w) -> { sp.edit().putString("state_" + i, states[w]).apply(); render(); }).show(); }
    private void editNote(int i) { EditText e = new EditText(this); e.setText(sp.getString("note_" + i, "")); new AlertDialog.Builder(this).setTitle("失败备注 / 调整说明").setView(e).setNegativeButton("取消", null).setPositiveButton("保存", (d,w) -> { sp.edit().putString("note_" + i, e.getText().toString()).apply(); render(); }).show(); }
}
