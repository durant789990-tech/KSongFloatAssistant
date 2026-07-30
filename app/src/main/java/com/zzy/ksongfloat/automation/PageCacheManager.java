package com.zzy.ksongfloat.automation;

import com.zzy.ksongfloat.capture.PageTextResult;
import com.zzy.ksongfloat.classifier.PageClassificationResult;
import com.zzy.ksongfloat.classifier.PageType;

/**
 * 页面识别缓存，绑定 session/windowId，1.5s 过期。
 */
public final class PageCacheManager {
    public static class CachedPage {
        public final PageTextResult page;
        public final PageClassificationResult cls;
        public final String sessionId;
        public final String packageName;
        public final int windowId;
        public final long generatedAt;
        public final String rootSignature;

        CachedPage(PageTextResult page, PageClassificationResult cls, String sessionId,
                   String packageName, int windowId, String rootSignature) {
            this.page = page;
            this.cls = cls;
            this.sessionId = sessionId == null ? "" : sessionId;
            this.packageName = packageName == null ? "" : packageName;
            this.windowId = windowId;
            this.generatedAt = System.currentTimeMillis();
            this.rootSignature = rootSignature == null ? "" : rootSignature;
        }

        public boolean isFresh(int expectedWindowId) {
            if (System.currentTimeMillis() - generatedAt > 1500L) return false;
            return windowId < 0 || expectedWindowId < 0 || windowId == expectedWindowId;
        }
    }

    private static final PageCacheManager INSTANCE = new PageCacheManager();
    private volatile CachedPage cached;

    public static PageCacheManager get() {
        return INSTANCE;
    }

    public void put(CachedPage page) {
        cached = page;
    }

    public CachedPage getIfFresh(String sessionId, int windowId) {
        CachedPage c = cached;
        if (c == null) return null;
        if (sessionId != null && !sessionId.equals(c.sessionId)) return null;
        if (!c.isFresh(windowId)) return null;
        return c;
    }

    public void invalidate(String reason) {
        cached = null;
        AutomationLog.info("PAGE_CACHE_INVALID " + reason);
    }

    public PageType currentType() {
        CachedPage c = cached;
        return c == null || c.cls == null ? PageType.UNKNOWN : c.cls.pageType;
    }
}
