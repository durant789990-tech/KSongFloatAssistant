package com.zzy.ksongfloat.automation;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;
import com.zzy.ksongfloat.shizuku.ShizukuHelper;

public class NodeActionController {
    private static final int ACTION_IME_ENTER = 0x0100000d;

    public static class FillResult {
        public boolean focused;
        public boolean filled;
        public boolean sent;
        public String method = "";
        public String error = "";
    }

    private static final String[] SEND_KEYWORDS = {
            "发送", "发布", "提交", "发表", "发送消息", "发表评论", "完成", "确定"
    };
    private static final String[] INPUT_HINTS = {
            "打个招呼", "说点什么", "评论", "输入", "私信", "聊天", "写评论", "请输入", "留言"
    };
    private static final String[] INPUT_ENTRY_KEYWORDS = {
            "打个招呼", "评论", "说点什么", "写评论", "发表评论", "留言", "输入"
    };

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

    public boolean openInputEntry() {
        if (clickByTexts(INPUT_ENTRY_KEYWORDS)) {
            AutomationLog.info("已点击输入入口");
            return true;
        }
        return clickInputField(false);
    }

    public boolean clickInputField(boolean requireEditable) {
        AccessibilityNodeInfo root = null;
        NodeFinder.Match field = null;
        try {
            root = freshRoot();
            field = NodeFinder.findInputField(root, INPUT_HINTS);
            if (field == null || field.node == null) {
                AccessibilityNodeInfo focused = root == null ? null : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (focused != null && (!requireEditable || focused.isEditable() || focused.isFocusable())) {
                    int[] size = ScreenBoundsGuard.screenSize(root);
                    if (ScreenBoundsGuard.isSafeNode(focused, size[0], size[1])) {
                        field = new NodeFinder.Match(focused, "focus", "");
                    } else {
                        NodeFinder.recycle(focused);
                    }
                } else {
                    NodeFinder.recycle(focused);
                }
            }
            if (field == null || field.node == null) return false;
            boolean ok = performClick(field.node);
            if (!ok) {
                field.node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ok = field.node.isFocused();
            }
            if (ok) AutomationLog.info("已点击输入框 matched=" + field.matchedText);
            return ok;
        } finally {
            if (field != null) NodeFinder.recycle(field.node);
            NodeFinder.recycle(root);
        }
    }

    public boolean setText(String text) {
        FillResult r = fillInput(text, true);
        return r.filled;
    }

    public FillResult fillInputAndSend(String text) {
        return fillInputAndSend(text, true);
    }

    public FillResult fillInputAndSend(String text, boolean autoSend) {
        FillResult r = fillInput(text, true);
        if (!r.filled) return r;
        if (autoSend) {
            r.sent = clickSendButton();
            if (r.sent) {
                AutomationLog.info("已点击发送按钮");
            } else {
                r.sent = sendEnterKey();
                if (r.sent) AutomationLog.info("已通过回车键发送");
                else AutomationLog.warn("未找到发送按钮，回车发送也失败");
            }
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
                    int[] size = ScreenBoundsGuard.screenSize(root);
                    if (ScreenBoundsGuard.isSafeNode(focused, size[0], size[1])) {
                        editable = new NodeFinder.Match(focused, "focus", "");
                    } else {
                        NodeFinder.recycle(focused);
                    }
                } else {
                    NodeFinder.recycle(focused);
                }
            }
            if (editable == null || editable.node == null) {
                result.error = "未找到输入框";
                return result;
            }
            AccessibilityNodeInfo node = editable.node;

            if (ShizukuHelper.isReady()) {
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                sleepQuiet(150);
                if (ShizukuHelper.inputText(text)) {
                    result.filled = true;
                    result.focused = true;
                    result.method = "SHIZUKU_INPUT_TEXT";
                    return result;
                }
                AutomationLog.warn("Shizuku input text 失败，降级无障碍填字");
            }

            putClipboard(text);
            if (clickFieldFirst || !node.isFocused()) {
                result.focused = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (!result.focused) result.focused = performClick(node);
                if (!result.focused) node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                sleepQuiet(300);
            }

            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                result.filled = true;
                result.method = "ACTION_SET_TEXT";
                return result;
            }

            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            sleepQuiet(120);
            if (node.performAction(AccessibilityNodeInfo.ACTION_PASTE)) {
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
                    if (parent.performAction(AccessibilityNodeInfo.ACTION_PASTE)) {
                        result.filled = true;
                        result.method = "PARENT_PASTE";
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

    public boolean sendEnterKey() {
        if (ShizukuHelper.isReady() && ShizukuHelper.keyEnter()) {
            return true;
        }
        AccessibilityNodeInfo root = null;
        AccessibilityNodeInfo focused = null;
        try {
            root = freshRoot();
            focused = root == null ? null : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focused != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (focused.performAction(ACTION_IME_ENTER)) {
                        return true;
                    }
                }
                Bundle args = new Bundle();
                args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
                if (focused.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, args)) {
                    return clickSendButton();
                }
            }
        } finally {
            NodeFinder.recycle(focused);
            NodeFinder.recycle(root);
        }
        return false;
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

    private void putClipboard(String text) {
        Context ctx = service.getApplicationContext();
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return;
        try {
            cm.setPrimaryClip(ClipData.newPlainText("auto_fill", text));
        } catch (Exception ignored) {
        }
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
