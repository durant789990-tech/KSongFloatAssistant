package com.zzy.ksongfloat.automation;

import com.zzy.ksongfloat.accessibility.KSongAccessibilityService;

/** 等待页面变化，带超时。 */
public final class PageChangeWaiter {
    public static boolean waitForChange(long revisionBefore, long timeoutMs) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (KSongAccessibilityService.getPageRevision() > revisionBefore) return true;
            Thread.sleep(120);
        }
        return KSongAccessibilityService.getPageRevision() > revisionBefore;
    }

    public static boolean waitForChange(long revisionBefore) throws InterruptedException {
        return waitForChange(revisionBefore, 5000);
    }
}
