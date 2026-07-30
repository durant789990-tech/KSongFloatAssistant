package com.zzy.ksongfloat.permission;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * HyperOS / MIUI 等无法系统检测的权限，使用三态：已开启 / 未开启 / 用户已确认。
 */
public final class PermissionStateRepository {
    public enum DetectState { SYSTEM_ON, SYSTEM_OFF, USER_CONFIRMED, UNKNOWN }

    public static class Item {
        public final String key;
        public final String title;
        public final DetectState state;

        Item(String key, String title, DetectState state) {
            this.key = key;
            this.title = title;
            this.state = state;
        }

        public String label() {
            switch (state) {
                case SYSTEM_ON: return "系统检测已开启";
                case USER_CONFIRMED: return "用户已确认开启";
                case SYSTEM_OFF: return "系统检测未开启";
                default: return "系统无法检测";
            }
        }
    }

    private static final PermissionStateRepository INSTANCE = new PermissionStateRepository();
    private static final String P = "hyperos_perm";
    private final MutableLiveData<Item[]> live = new MutableLiveData<>();

    public static PermissionStateRepository get() {
        return INSTANCE;
    }

    public LiveData<Item[]> observe() {
        return live;
    }

    public void refresh(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE);
        Item[] items = new Item[]{
                item("autostart", "自启动", sp),
                item("background_popup", "后台弹出界面", sp),
                item("battery", "省电策略无限制", sp)
        };
        live.postValue(items);
    }

    public void userConfirm(Context ctx, String key) {
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putBoolean(key + "_confirmed", true).apply();
        refresh(ctx);
    }

    public boolean isBlocking(String key) {
        return false;
    }

    private Item item(String key, String title, SharedPreferences sp) {
        boolean confirmed = sp.getBoolean(key + "_confirmed", false);
        DetectState st = confirmed ? DetectState.USER_CONFIRMED : DetectState.UNKNOWN;
        return new Item(key, title, st);
    }
}
