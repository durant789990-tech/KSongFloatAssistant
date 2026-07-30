package com.zzy.ksongfloat.automation;

import android.content.Context;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 当前屏幕用户任务队列，按 Y 坐标从上到下。 */
public final class UserTaskQueue {
    private final List<UserTask> tasks = new ArrayList<>();
    private int currentIndex = 0;

    public List<UserTask> all() {
        return new ArrayList<>(tasks);
    }

    public int size() {
        return tasks.size();
    }

    public int pendingCount() {
        int n = 0;
        for (UserTask t : tasks) if (t.status == UserTask.Status.PENDING) n++;
        return n;
    }

    public UserTask current() {
        if (currentIndex < 0 || currentIndex >= tasks.size()) return null;
        return tasks.get(currentIndex);
    }

    public UserTask nextPending() {
        for (UserTask t : tasks) {
            if (t.status == UserTask.Status.PENDING) return t;
        }
        return null;
    }

    public boolean hasPending() {
        return nextPending() != null;
    }

    public void buildFromCards(Context ctx, List<UserCardDetector.Candidate> cards,
                               ProcessedUserRepository processed, int windowId) {
        tasks.clear();
        currentIndex = 0;
        if (cards == null || cards.isEmpty()) return;
        List<UserCardDetector.Candidate> sorted = new ArrayList<>(cards);
        sorted.sort(Comparator.comparingInt(c -> c.bounds.top));
        Set<String> seen = new HashSet<>();
        int idx = 0;
        for (UserCardDetector.Candidate c : sorted) {
            if (c.nodeKey == null || seen.contains(c.nodeKey)) continue;
            if (processed.wasProcessedRecently(ctx, c.nodeKey)) continue;
            seen.add(c.nodeKey);
            tasks.add(new UserTask(c.nodeKey, c.label, c.nodeKey, new Rect(c.bounds), windowId, idx++));
        }
        AutomationLog.info("QUEUE_BUILT count=" + tasks.size() + " windowId=" + windowId);
    }

    public void advanceAfterComplete() {
        currentIndex++;
    }

    public void clear() {
        tasks.clear();
        currentIndex = 0;
    }
}
