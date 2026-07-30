package com.zzy.ksongfloat.history;

public final class InteractionStatus {
    public static final String DISCOVERED = "DISCOVERED";
    public static final String ANALYZED = "ANALYZED";
    public static final String COMMENT_PREPARED = "COMMENT_PREPARED";
    public static final String MESSAGE_PREPARED = "MESSAGE_PREPARED";
    public static final String CONTACTED = "CONTACTED";
    public static final String REPLIED = "REPLIED";
    public static final String FRIEND_ADDED = "FRIEND_ADDED";
    public static final String SKIPPED = "SKIPPED";
    public static final String DO_NOT_CONTACT = "DO_NOT_CONTACT";

    private InteractionStatus() {}

    public static String label(String status) {
        if (ANALYZED.equals(status)) return "已分析";
        if (COMMENT_PREPARED.equals(status)) return "已准备评论";
        if (MESSAGE_PREPARED.equals(status)) return "已准备私信";
        if (CONTACTED.equals(status)) return "已联系";
        if (REPLIED.equals(status)) return "已回复";
        if (FRIEND_ADDED.equals(status)) return "已加好友";
        if (SKIPPED.equals(status)) return "已跳过";
        if (DO_NOT_CONTACT.equals(status)) return "不再联系";
        return "新发现";
    }
}
