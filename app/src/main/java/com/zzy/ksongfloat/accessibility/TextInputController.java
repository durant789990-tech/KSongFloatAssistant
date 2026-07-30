package com.zzy.ksongfloat.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import com.zzy.ksongfloat.classifier.PageType;
import com.zzy.ksongfloat.config.TargetAppConfig;

public class TextInputController {
    private final AccessibilityService service;

    public TextInputController(AccessibilityService s) {
        service = s;
    }

    public String canFill(android.content.Context c, TextFillRequest r) {
        if (r == null || r.text == null || r.text.trim().isEmpty()) return "建议内容为空";
        if (!TargetAppConfig.matches(c, KSongAccessibilityService.getForegroundPackage())) return "当前不是目标 K 歌页面";
        if (!(r.pageType == PageType.COMMENT_INPUT || r.pageType == PageType.PRIVATE_MESSAGE_INPUT)) return "当前页面类型不适合填入";
        if (r.type.equals("COMMENT") && r.pageType != PageType.COMMENT_INPUT) return "当前不是评论输入页面";
        if (r.type.equals("PRIVATE_MESSAGE") && r.pageType != PageType.PRIVATE_MESSAGE_INPUT) return "当前不是私信输入页面";

        AccessibilityNodeInfo root = service.getRootInActiveWindow(), f = null;
        try {
            f = root == null ? null : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (f == null || !f.isEditable()) return "未找到可编辑输入框，请先点一下 K 歌输入框";
            return "";
        } finally {
            try { if (f != null) f.recycle(); } catch (Exception ignored) {}
            try { if (root != null) root.recycle(); } catch (Exception ignored) {}
        }
    }

    public boolean fill(TextFillRequest r) {
        AccessibilityNodeInfo root = service.getRootInActiveWindow(), f = null;
        try {
            f = root == null ? null : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (f == null || !f.isEditable()) return false;
            String text = r.text;
            if (r.append) {
                CharSequence cur = f.getText();
                text = (cur == null ? "" : cur.toString()) + text;
            }
            Bundle b = new Bundle();
            b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return f.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
        } finally {
            try { if (f != null) f.recycle(); } catch (Exception ignored) {}
            try { if (root != null) root.recycle(); } catch (Exception ignored) {}
        }
    }
}
