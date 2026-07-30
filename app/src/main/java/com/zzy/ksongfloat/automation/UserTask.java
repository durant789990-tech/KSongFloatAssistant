package com.zzy.ksongfloat.automation;

import android.graphics.Rect;

/** 同城列表中的一个待处理用户。 */
public final class UserTask {
    public enum Status {
        PENDING, OPENING, ANALYZING, DRAFT_READY, FILLED,
        WAITING_CONFIRMATION, SENT, SKIPPED, FAILED, COMPLETED
    }

    public final String userKey;
    public final String displayName;
    public final String cardNodeKey;
    public final Rect originalBounds;
    public final int listWindowId;
    public final int queueIndex;
    public volatile Status status = Status.PENDING;
    public volatile int retryCount;
    public volatile String failureReason = "";
    public volatile String commentDraft = "";
    public volatile String messageDraft = "";

    UserTask(String userKey, String displayName, String cardNodeKey, Rect bounds, int windowId, int index) {
        this.userKey = userKey;
        this.displayName = displayName;
        this.cardNodeKey = cardNodeKey;
        this.originalBounds = bounds;
        this.listWindowId = windowId;
        this.queueIndex = index;
    }
}
