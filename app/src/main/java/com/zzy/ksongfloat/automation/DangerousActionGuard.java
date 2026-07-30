package com.zzy.ksongfloat.automation;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Locale;

/**
 * 危险节点文本拦截。
 */
public final class DangerousActionGuard {
    public static boolean isDangerousNode(AccessibilityNodeInfo node) {
        if (node == null) return true;
        String text = join(node.getText(), node.getContentDescription(), node.getViewIdResourceName());
        String l = text.toLowerCase(Locale.ROOT);
        return l.contains("切换账号") || l.contains("退出当前账号") || l.contains("退出登录")
                || l.contains("注销") || l.contains("删除账号") || l.contains("修改密码")
                || l.contains("清除数据") || l.contains("确认退出") || l.contains("确认注销")
                || l.contains("设置") && (l.contains("账号") || l.contains("安全") || l.contains("隐私"));
    }

    public static boolean isDangerousLabel(String label) {
        if (label == null || label.isEmpty()) return true;
        return UserCardDetector.isExcludedNavLabel(label);
    }

    private static String join(CharSequence... parts) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence p : parts) {
            if (p == null || p.length() == 0) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p);
        }
        return sb.toString();
    }
}
