package com.zzy.ksongfloat.automation;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;

public class NodeActionController {
    public static class FillResult {
        public boolean focused;
        public boolean filled;
        public boolean sent;
        public String method = "";
        public String error = "";
    }

    private static final String[] SEND_KEYWORDS = {"发送", "发布", "提交", "发表", "发送消息", "发表评论", "完成"};
    private static final String[] INPUT_HINTS = {"说点什么", "评论", "输入", "私信", "消息", "聊天", "写评论", "请输入"};

    private final AccessibilityService service;

    public NodeActionController(AccessibilityService service) {
        this.service = service;
    }

    public boolean click(NodeFinder.Match match) {
        if (match == null || match.node == null) return false;
        AccessibilityNodeInfo node = match.node;
        try {
            if (performClick(node)) return true;
            AccessibilityNodeInfo p = node.getParent();
            try {
                return p != null && performClick(p);
            } finally {
                NodeFinder.recycle(p);
            }
        } finally {
            NodeFinder.recycle(node);
        }
    }

    public boolean clickByTexts(String... keywords) {
        AccessibilityNodeInfo root = null;
        try {
            root = freshRoot();
            NodeFinder.Match m = NodeFinder.findClickableByTexts(root, keywords);
            return click(m);
        } finally {
            NodeFinder.recycle(root);
        }
    }

    public boolean setText(String text) {
        FillResult r = fillInput(text, false);
        return r.filled;
    }

    public FillResult fillInputAndSend(String text, boolean autoSend) {
        FillResult r = fillInput(text, true);
        if (!r.filled) return r;
        if (autoSend) {
            r.sent = clickSendButton();
            if (r.sent) AutomationLog.info("已点击发送按钮");
            else AutomationLog.warn("文本已填入，但未找到发送按钮");
        }
        return r;
    }

    public FillResult fillInput(String text, boolean clickFieldFirst) {
        FillResult result = new FillResult();
        if (text == null || text.trim().isEmpty()) {
            result.error = "文本为空";
            return result;
        }
        AccessibilityNodeInfo root = null;
        NodeFinder.Match editable = null;
        try {
            root = freshRoot();
            editable = NodeFinder.findInputField(root, INPUT_HINTS);
            if (editable == null) {
                AccessibilityNodeInfo focused = root == null ? null : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (focused != null && (focused.isEditable() || focused.isFocusable())) {
                    editable = new NodeFinder.Match(focused, "focus", "");
                } else {
                    NodeFinder.recycle(focused);
                }
            }
            if (editable == null || editable.node == null) {
                result.error = "未找到输入框";
                return result;
            }
            AccessibilityNodeInfo node = editable.node;
            if (clickFieldFirst || !node.isFocused()) {
                result.focused = performClick(node);
                if (!result.focused) node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            }
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                result.filled = true;
                result.method = "ACTION_SET_TEXT";
                return result;
            }
            if (pasteViaClipboard(node, text)) {
                result.filled = true;
                result.method = "ACTION_PASTE";
                return result;
            }
            AccessibilityNodeInfo parent = node.getParent();
            try {
                if (parent != null) {
                    Bundle parentArgs = new Bundle();
                    parentArgs.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                    if (parent.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, parentArgs)) {
                        result.filled = true;
                        result.method = "PARENT_SET_TEXT";
                        return result;
                    }
                }
            } finally {
                NodeFinder.recycle(parent);
            }
            result.error = "SET_TEXT 与 PASTE 均失败";
            return result;
        } finally {
            if (editable != null) NodeFinder.recycle(editable.node);
            NodeFinder.recycle(root);
        }
    }

    public boolean clickSendButton() {
        AccessibilityNodeInfo root = null;
        try {
            root = freshRoot();
            NodeFinder.Match send = NodeFinder.findClickableByTexts(root, SEND_KEYWORDS);
            return click(send);
        } finally {
            NodeFinder.recycle(root);
        }
    }

    public boolean back() {
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    public boolean home() {
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    private AccessibilityNodeInfo freshRoot() {
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root != null) return root;
        KSongAccessibilityService ks = KSongAccessibilityService.getInstance();
        return ks == null ? null : ks.getRootInActiveWindowSafe();
    }

    private boolean performClick(AccessibilityNodeInfo node) {
        if (node == null) return false;
        if (node.isClickable() && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
        if (node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        return false;
    }

    private boolean pasteViaClipboard(AccessibilityNodeInfo node, String text) {
        Context ctx = service.getApplicationContext();
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return false;
        ClipData old = cm.getPrimaryClip();
        try {
            cm.setPrimaryClip(ClipData.newPlainText("auto_fill", text));
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            return node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        } catch (Exception e) {
            return false;
        } finally {
            if (old != null) {
                try {
                    cm.setPrimaryClip(old);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
