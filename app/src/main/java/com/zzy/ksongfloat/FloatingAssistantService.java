package com.zzy.ksongfloat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingAssistantService extends Service {
    private WindowManager wm;
    private View bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;
    private boolean panelShown = false;
    private String lastSuggestion = "你的声音很有故事感，副歌情绪推得很自然，听完真的有被带到。";

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        startAsForeground();
        showBubble();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (bubble == null) showBubble();
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void startAsForeground() {
        String channelId = "float_assistant";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(channelId, "K歌悬浮助手", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, channelId) : new Notification.Builder(this);
        b.setContentTitle("K歌悬浮助手运行中")
                .setContentText("点击悬浮球展开面板，敏感操作需手动确认")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true);
        startForeground(1001, b.build());
    }

    private void showBubble() {
        if (bubble != null) return;
        TextView v = new TextView(this);
        v.setText("K\n歌");
        v.setTextColor(Color.WHITE);
        v.setTextSize(14);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        v.setBackground(Rounded.bg(Color.rgb(108, 92, 231), dp(28), Color.WHITE, dp(1)));
        bubble = v;
        bubbleParams = newParams(dp(58), dp(58));
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = dp(16);
        bubbleParams.y = dp(320);
        attachDrag(bubble, bubbleParams);
        v.setOnClickListener(x -> togglePanel());
        wm.addView(bubble, bubbleParams);
    }

    private void togglePanel() { if (panelShown) hidePanel(); else showPanel(); }

    private void showPanel() {
        if (panel != null) return;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(14));
        root.setBackground(Rounded.bg(Color.rgb(23, 29, 52), dp(18), Color.rgb(92, 110, 170), dp(1)));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("K歌助手", 16, Color.WHITE, true);
        TextView state = label("  等待确认  ", 11, Color.rgb(255, 221, 100), true);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(state);
        root.addView(top);

        TextView info = label("当前页面：" + KaraokeAccessibilityService.getPageSummary() + "\n敏感动作：评论/私信/送礼前必须手动确认", 12, Color.rgb(195, 205, 235), false);
        info.setPadding(0, dp(8), 0, dp(10));
        root.addView(info);

        TextView suggestion = label(lastSuggestion, 14, Color.WHITE, false);
        suggestion.setPadding(dp(10), dp(10), dp(10), dp(10));
        suggestion.setBackground(Rounded.bg(Color.rgb(15, 20, 38), dp(12), Color.rgb(56, 68, 110), dp(1)));
        root.addView(suggestion, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row1 = row();
        row1.addView(action("识别当前页", v -> info.setText("当前页面：" + KaraokeAccessibilityService.getPageSummary() + "\n已刷新页面摘要")), weight());
        row1.addView(action("生成评论", v -> { lastSuggestion = "你的演唱很有感染力，转音处理自然，听起来特别舒服。"; suggestion.setText(lastSuggestion); }), weight());
        root.addView(row1);

        LinearLayout row2 = row();
        row2.addView(action("生成私信", v -> { lastSuggestion = "刚听到你的作品，声音很干净，想请教你平时怎么练气息的～"; suggestion.setText(lastSuggestion); }), weight());
        row2.addView(action("复制", v -> copySuggestion()), weight());
        root.addView(row2);

        LinearLayout row3 = row();
        row3.addView(action("跳过记录", v -> toast("已记录为跳过（占位）")), weight());
        row3.addView(action("关闭", v -> hidePanel()), weight());
        root.addView(row3);

        panel = root;
        panelParams = newParams(dp(292), WindowManager.LayoutParams.WRAP_CONTENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = dp(72);
        panelParams.y = Math.max(dp(80), bubbleParams == null ? dp(260) : bubbleParams.y - dp(30));
        attachDrag(panel, panelParams);
        wm.addView(panel, panelParams);
        panelShown = true;
    }

    private Button action(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setBackground(Rounded.bg(Color.rgb(69, 82, 135), dp(10), 0, 0));
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(8), 0, 0);
        return r;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1);
        lp.setMargins(dp(3), 0, dp(3), 0);
        return lp;
    }

    private TextView label(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    private void copySuggestion() {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("K歌助手建议", lastSuggestion));
        toast("已复制，发送仍需你手动确认");
    }

    private void hidePanel() {
        if (panel != null) { try { wm.removeView(panel); } catch (Exception ignored) {} }
        panel = null;
        panelShown = false;
    }

    private WindowManager.LayoutParams newParams(int w, int h) {
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        return new WindowManager.LayoutParams(w, h, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
    }

    private void attachDrag(View view, WindowManager.LayoutParams params) {
        final int[] startX = new int[1], startY = new int[1];
        final float[] downX = new float[1], downY = new float[1];
        final boolean[] moved = new boolean[1];
        view.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX[0] = params.x; startY[0] = params.y;
                    downX[0] = e.getRawX(); downY[0] = e.getRawY(); moved[0] = false;
                    return false;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int)(e.getRawX() - downX[0]);
                    int dy = (int)(e.getRawY() - downY[0]);
                    if (Math.abs(dx) + Math.abs(dy) > dp(4)) moved[0] = true;
                    params.x = startX[0] + dx; params.y = startY[0] + dy;
                    try { wm.updateViewLayout(v, params); } catch (Exception ignored) {}
                    return true;
                case MotionEvent.ACTION_UP:
                    return moved[0];
            }
            return false;
        });
    }

    @Override public void onDestroy() {
        hidePanel();
        if (bubble != null) { try { wm.removeView(bubble); } catch (Exception ignored) {} }
        bubble = null;
        super.onDestroy();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}