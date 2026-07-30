package com.zzy.ksongfloat.history;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;

import com.zzy.ksongfloat.navigation.AppNavigator;
import com.zzy.ksongfloat.ui.components.IosNavigationBar;
import com.zzy.ksongfloat.util.UiKit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends Activity {
    private LinearLayout list;
    private EditText search;
    private String currentStatus = "";
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = UiKit.root(this);
        IosNavigationBar nav = new IosNavigationBar(this, "互动历史", "");
        UiKit.attachClick(nav.left, v -> AppNavigator.finish(this));
        root.addView(nav);

        search = new EditText(this);
        search.setHint("搜索昵称");
        root.addView(search);
        Button searchBtn = UiKit.button(this, "搜索", Color.rgb(96,112,170));
        UiKit.attachClick(searchBtn, v -> loadSearch());
        root.addView(searchBtn);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(filters);
        addFilter(filters, "全部", "");
        addFilter(filters, "已分析", InteractionStatus.ANALYZED);
        addFilter(filters, "已准备评论", InteractionStatus.COMMENT_PREPARED);
        addFilter(filters, "已准备私信", InteractionStatus.MESSAGE_PREPARED);
        addFilter(filters, "已联系", InteractionStatus.CONTACTED);
        addFilter(filters, "已回复", InteractionStatus.REPLIED);
        addFilter(filters, "已加好友", InteractionStatus.FRIEND_ADDED);
        addFilter(filters, "已跳过", InteractionStatus.SKIPPED);
        addFilter(filters, "不再联系", InteractionStatus.DO_NOT_CONTACT);
        root.addView(hs);

        Button export = UiKit.button(this, "复制导出 JSON（不包含 API Key）", Color.rgb(92,101,125));
        UiKit.attachClick(export, v -> exportJson());
        root.addView(export);
        Button clear = UiKit.button(this, "清空记录", Color.rgb(168,72,72));
        UiKit.attachClick(clear, v -> new AlertDialog.Builder(this).setTitle("确认清空").setMessage("这会删除所有本地互动历史，不会影响 API Key。")
                .setNegativeButton("取消", null).setPositiveButton("清空", (d,w) -> clearAll()).show());
        root.addView(clear);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        loadAll();
    }

    private void addFilter(LinearLayout filters, String label, String status) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        UiKit.attachClick(b, v -> { currentStatus = status; loadAll(); });
        filters.addView(b, new LinearLayout.LayoutParams(UiKit.dp(this, 120), UiKit.dp(this, 42)));
    }

    private void loadAll() {
        if (currentStatus == null || currentStatus.length() == 0) {
            HistoryRepository.all(this, cb());
        } else {
            HistoryRepository.byStatus(this, currentStatus, cb());
        }
    }

    private void loadSearch() {
        HistoryRepository.search(this, search.getText().toString(), cb());
    }

    private HistoryRepository.Callback<List<InteractionRecord>> cb() {
        return new HistoryRepository.Callback<List<InteractionRecord>>() {
            public void onResult(List<InteractionRecord> value) { runOnUiThread(() -> render(value)); }
            public void onError(Exception e) { runOnUiThread(() -> toast("加载失败：" + e.getMessage())); }
        };
    }

    private void render(List<InteractionRecord> rows) {
        list.removeAllViews();
        if (rows == null || rows.isEmpty()) {
            list.addView(UiKit.text(this, "暂无记录", 14, Color.rgb(95,104,128), false));
            return;
        }
        for (InteractionRecord r : rows) {
            LinearLayout card = UiKit.card(this);
            TextView t = UiKit.text(this,
                    "昵称：" + empty(r.nickname) + "\n状态：" + InteractionStatus.label(r.interactionStatus)
                            + "\n页面：" + r.pageType
                            + "\n上次分析：" + time(r.lastAnalyzedAt)
                            + "\n上次评论：" + brief(r.generatedComments)
                            + "\n上次私信：" + brief(r.generatedMessages)
                            + "\n备注：" + empty(r.userNotes),
                    13, Color.rgb(35,42,62), false);
            t.setTextIsSelectable(true);
            card.addView(t);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(small("详情", v -> detail(r)), new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1));
            row.addView(small("状态", v -> chooseStatus(r)), new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1));
            row.addView(small("备注", v -> editNotes(r)), new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1));
            row.addView(small("删除", v -> deleteOne(r)), new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1));
            card.addView(row);
            list.addView(card);
        }
    }

    private Button small(String s, android.view.View.OnClickListener l) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(12); UiKit.attachClick(b, l); return b;
    }

    private void detail(InteractionRecord r) {
        new AlertDialog.Builder(this).setTitle(empty(r.nickname)).setMessage(
                "状态：" + InteractionStatus.label(r.interactionStatus)
                        + "\n首次发现：" + time(r.firstSeenAt)
                        + "\n最近出现：" + time(r.lastSeenAt)
                        + "\n最近分析：" + time(r.lastAnalyzedAt)
                        + "\n简介：" + empty(r.visibleBio)
                        + "\n歌曲：" + empty(r.visibleSongs)
                        + "\n风险：" + empty(r.riskFlags))
                .setPositiveButton("关闭", null).show();
    }

    private void chooseStatus(InteractionRecord r) {
        String[] values = {InteractionStatus.DISCOVERED, InteractionStatus.ANALYZED, InteractionStatus.COMMENT_PREPARED, InteractionStatus.MESSAGE_PREPARED, InteractionStatus.CONTACTED, InteractionStatus.REPLIED, InteractionStatus.FRIEND_ADDED, InteractionStatus.SKIPPED, InteractionStatus.DO_NOT_CONTACT};
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) labels[i] = InteractionStatus.label(values[i]);
        new AlertDialog.Builder(this).setTitle("修改状态").setItems(labels, (d, which) -> HistoryRepository.updateStatus(this, r.id, values[which], boolCb("状态已更新"))).show();
    }

    private void editNotes(InteractionRecord r) {
        EditText e = new EditText(this);
        e.setText(r.userNotes);
        new AlertDialog.Builder(this).setTitle("备注").setView(e).setNegativeButton("取消", null)
                .setPositiveButton("保存", (d,w) -> HistoryRepository.updateNotes(this, r.id, e.getText().toString(), boolCb("备注已保存"))).show();
    }

    private void deleteOne(InteractionRecord r) {
        HistoryRepository.delete(this, r, boolCb("已删除"));
    }

    private void clearAll() {
        HistoryRepository.clear(this, boolCb("已清空"));
    }

    private void exportJson() {
        HistoryRepository.exportJson(this, new HistoryRepository.Callback<String>() {
            public void onResult(String value) {
                runOnUiThread(() -> {
                    ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("history-json", value));
                    toast("JSON 已复制");
                });
            }
            public void onError(Exception e) { runOnUiThread(() -> toast("导出失败：" + e.getMessage())); }
        });
    }

    private HistoryRepository.Callback<Boolean> boolCb(String ok) {
        return new HistoryRepository.Callback<Boolean>() {
            public void onResult(Boolean value) { runOnUiThread(() -> { toast(ok); loadAll(); }); }
            public void onError(Exception e) { runOnUiThread(() -> toast("操作失败：" + e.getMessage())); }
        };
    }

    private String time(long t) { return t <= 0 ? "暂无" : fmt.format(new Date(t)); }
    private String empty(String s) { return s == null || s.length() == 0 ? "暂无" : s; }
    private String brief(String s) { if (s == null || s.length() == 0 || "[]".equals(s)) return "暂无"; return s.length() > 80 ? s.substring(0, 80) + "..." : s; }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
