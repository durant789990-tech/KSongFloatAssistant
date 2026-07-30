package com.zzy.ksongfloat.automation;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** 已处理用户去重，支持时间过滤。 */
public final class ProcessedUserRepository {
    private static final String P = "processed_users";
    private final Set<String> sessionKeys = new HashSet<>();
    private long filterBeforeMs;

    public ProcessedUserRepository(long filterMinutes) {
        filterBeforeMs = filterMinutes <= 0 ? 0 : System.currentTimeMillis() - filterMinutes * 60_000L;
    }

    public void clearSession() {
        sessionKeys.clear();
    }

    public boolean isProcessed(String userKey) {
        return userKey != null && !userKey.isEmpty() && sessionKeys.contains(userKey);
    }

    public void markProcessed(Context ctx, String userKey) {
        if (userKey == null || userKey.isEmpty()) return;
        sessionKeys.add(userKey);
        if (ctx != null) {
            ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
                    .putLong("t_" + userKey.hashCode(), System.currentTimeMillis())
                    .apply();
        }
    }

    public boolean wasProcessedRecently(Context ctx, String userKey) {
        if (userKey == null || userKey.isEmpty()) return false;
        if (sessionKeys.contains(userKey)) return true;
        if (ctx == null || filterBeforeMs <= 0) return false;
        long t = ctx.getSharedPreferences(P, Context.MODE_PRIVATE)
                .getLong("t_" + userKey.hashCode(), 0);
        return t > filterBeforeMs;
    }
}
