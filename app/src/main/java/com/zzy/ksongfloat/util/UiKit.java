package com.zzy.ksongfloat.util;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.zzy.ksongfloat.Rounded;
import com.zzy.ksongfloat.feedback.HapticFeedbackManager;
import com.zzy.ksongfloat.ui.theme.AppTheme;

public class UiKit {
    public static LinearLayout root(Activity a) {
        FrameLayout container = new FrameLayout(a);
        ScrollView sv = new ScrollView(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(a, AppTheme.PAGE_PADDING_DP);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(AppTheme.BG);
        sv.addView(root);
        container.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        a.setContentView(container);
        applyWindowInsets(container, root);
        return root;
    }

    public static void applyWindowInsets(View rootView, View contentView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = Math.max(sys.bottom, ime.bottom);
            contentView.setPadding(
                    contentView.getPaddingLeft(),
                    sys.top + contentView.getPaddingTop(),
                    contentView.getPaddingRight(),
                    bottom + dp(contentView.getContext(), 16));
            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }

    public static TextView text(Activity a, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.15f);
        t.setTextIsSelectable(false);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    public static TextView title(Activity a, String s) {
        TextView t = text(a, s, AppTheme.TITLE_SP, AppTheme.TEXT_PRIMARY, true);
        t.setPadding(0, 0, 0, dp(a, 8));
        return t;
    }

    public static TextView caption(Activity a, String s) {
        return text(a, s, AppTheme.CAPTION_SP, AppTheme.TEXT_SECONDARY, false);
    }

    public static Button button(Activity a, String s, int color) {
        Button b = new Button(a);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(AppTheme.BODY_SP);
        b.setTextColor(Color.WHITE);
        b.setBackground(Rounded.bg(color, dp(a, AppTheme.RADIUS_BUTTON_DP), 0, 0));
        b.setOnTouchListener((v, e) -> {
            if (!v.isEnabled()) return false;
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.86f).setDuration(100).start();
            } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start();
            }
            return false;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(a, AppTheme.BUTTON_HEIGHT_DP));
        lp.setMargins(0, dp(a, 4), 0, dp(a, AppTheme.CARD_GAP_DP));
        b.setLayoutParams(lp);
        return b;
    }

    public static Button textButton(Activity a, String s) {
        Button b = new Button(a);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(AppTheme.CAPTION_SP);
        b.setTextColor(AppTheme.BRAND);
        b.setBackgroundColor(Color.TRANSPARENT);
        return b;
    }

    public static void attachClick(Button b, View.OnClickListener l) {
        b.setOnClickListener(v -> {
            HapticFeedbackManager.tick(v);
            if (l != null) l.onClick(v);
        });
    }

    public static LinearLayout card(Activity a) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(a, 14), dp(a, 12), dp(a, 14), dp(a, 12));
        c.setBackground(Rounded.bg(AppTheme.CARD, dp(a, AppTheme.RADIUS_CARD_DP), AppTheme.CARD_BORDER, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(a, AppTheme.CARD_GAP_DP));
        c.setLayoutParams(lp);
        return c;
    }

    public static int dp(Activity a, int v) {
        return dp((android.content.Context) a, v);
    }

    public static int dp(android.content.Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static void addGap(LinearLayout root, Activity a, int dp) {
        View gap = new View(a);
        gap.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(a, dp)));
        root.addView(gap);
    }
}
