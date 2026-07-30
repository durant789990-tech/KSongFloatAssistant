package com.zzy.ksongfloat.automation;

import android.view.accessibility.AccessibilityNodeInfo;

import com.zzy.ksongfloat.capture.PageTextResult;
import com.zzy.ksongfloat.classifier.PageType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 敏感页面硬拦截：设置、账号、系统弹窗。
 */
public final class SensitivePageGuard {
    public static class Result {
        public final boolean sensitive;
        public final PageType pageType;
        public final String reason;

        Result(boolean sensitive, PageType pageType, String reason) {
            this.sensitive = sensitive;
            this.pageType = pageType;
            this.reason = reason == null ? "" : reason;
        }
    }

    private static final Set<String> DANGEROUS = new HashSet<>(Arrays.asList(
            "设置", "账号设置", "账号与安全", "切换账号", "退出当前账号", "退出登录", "注销账号",
            "删除账号", "修改密码", "实名认证", "支付设置", "清除数据", "确认退出", "确认注销",
            "隐私设置", "安全中心", "账号管理", "退出", "注销"
    ));

    public static Result inspect(PageTextResult page, AccessibilityNodeInfo root) {
        String hay = buildHaystack(page, root);
        if (containsAny(hay, "com.android.settings", "settings", "setting")) {
            return hit(PageType.SYSTEM_SETTINGS, "检测到系统设置页面");
        }
        if (containsAny(hay, "切换账号", "退出当前账号", "退出登录", "注销账号", "删除账号", "确认退出", "确认注销")) {
            return hit(PageType.ACCOUNT_SETTINGS, "检测到账号/退出相关页面");
        }
        if (containsAny(hay, "账号与安全", "账号设置", "安全中心", "隐私设置", "支付设置", "实名认证")) {
            return hit(PageType.KSONG_SETTINGS, "检测到 K 歌设置/账号页面");
        }
        if (containsAny(hay, "设置") && (containsAny(hay, "帮助与反馈", "清除缓存", "关于", "权限", "通知"))) {
            return hit(PageType.KSONG_SETTINGS, "检测到设置页面");
        }
        if (containsAny(hay, "permissioncontroller", "packageinstaller", "系统界面", "systemui")) {
            return hit(PageType.SYSTEM_DIALOG, "检测到系统弹窗");
        }
        return new Result(false, PageType.UNKNOWN, "");
    }

    private static Result hit(PageType type, String reason) {
        return new Result(true, type, reason);
    }

    private static String buildHaystack(PageTextResult page, AccessibilityNodeInfo root) {
        StringBuilder sb = new StringBuilder();
        if (page != null) {
            append(sb, page.windowTitle);
            append(sb, page.mergedText);
            append(sb, page.packageName);
        }
        appendTree(sb, root, 0);
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private static void appendTree(StringBuilder sb, AccessibilityNodeInfo n, int depth) {
        if (n == null || depth > 10) return;
        append(sb, n.getClassName() == null ? "" : n.getClassName().toString());
        append(sb, n.getText());
        append(sb, n.getContentDescription());
        append(sb, n.getViewIdResourceName());
        int c = Math.min(n.getChildCount(), 25);
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo ch = n.getChild(i);
            try {
                appendTree(sb, ch, depth + 1);
            } finally {
                NodeFinder.recycle(ch);
            }
        }
    }

    private static void append(StringBuilder sb, CharSequence s) {
        if (s == null || s.length() == 0) return;
        if (sb.length() > 0) sb.append('\n');
        sb.append(s);
    }

    private static boolean containsAny(String hay, String... keys) {
        for (String k : keys) {
            if (k != null && !k.isEmpty() && hay.contains(k.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
