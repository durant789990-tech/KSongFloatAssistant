package com.zzy.ksongfloat.location;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * 模拟定位唯一状态源。
 */
public final class LocationStateRepository {
    public enum ServiceState {
        STOPPED,
        CHECKING_PERMISSION,
        STARTING,
        INJECTING,
        READBACK_CONFIRMED,
        PERMISSION_MISSING,
        PROVIDER_FAILED,
        SERVICE_FAILED,
        STOPPING
    }

    public static class State {
        public final ServiceState serviceState;
        public final MockLocationPermissionChecker.MockPermissionStatus permissionStatus;
        public final String label;
        public final String city;
        public final double latitude;
        public final double longitude;
        public final String lastInjectAt;
        public final String lastError;
        public final String packageName;
        public final int uid;
        public final String appOpsMode;
        public final long updatedAt;

        State(ServiceState serviceState, MockLocationPermissionChecker.MockPermissionStatus permissionStatus,
              String label, String city, double lat, double lng, String lastInjectAt, String lastError,
              String packageName, int uid, String appOpsMode) {
            this.serviceState = serviceState;
            this.permissionStatus = permissionStatus;
            this.label = label == null ? "" : label;
            this.city = city == null ? "" : city;
            this.latitude = lat;
            this.longitude = lng;
            this.lastInjectAt = lastInjectAt == null ? "" : lastInjectAt;
            this.lastError = lastError == null ? "" : lastError;
            this.packageName = packageName == null ? "" : packageName;
            this.uid = uid;
            this.appOpsMode = appOpsMode == null ? "" : appOpsMode;
            this.updatedAt = System.currentTimeMillis();
        }

        public String summaryLabel() {
            switch (serviceState) {
                case INJECTING:
                    return "正在模拟（" + (city.isEmpty() ? label : city) + "）";
                case READBACK_CONFIRMED:
                    return "已注入并验证（" + (city.isEmpty() ? label : city) + "）";
                case CHECKING_PERMISSION:
                    return "检查权限中";
                case STARTING:
                    return "正在启动";
                case PERMISSION_MISSING:
                    if (!lastError.isEmpty()) return lastError;
                    return "模拟权限未授权";
                case PROVIDER_FAILED:
                    if (!lastError.isEmpty()) return lastError;
                    return "Provider 初始化失败";
                case SERVICE_FAILED:
                    return "服务启动失败";
                case STOPPING:
                    return "正在停止";
                default:
                    if (permissionStatus == MockLocationPermissionChecker.MockPermissionStatus.PROBE_SUCCEEDED
                            || permissionStatus == MockLocationPermissionChecker.MockPermissionStatus.ALLOWED) {
                        return city.isEmpty() ? "权限已验证，未启动" : "已配置 · " + city;
                    }
                    return "未启用";
            }
        }
    }

    private static final LocationStateRepository INSTANCE = new LocationStateRepository();
    private final MutableLiveData<State> live = new MutableLiveData<>();
    private volatile State cached = defaultState();

    public static LocationStateRepository get() {
        return INSTANCE;
    }

    public LiveData<State> observe() {
        return live;
    }

    public State getCurrent() {
        return cached;
    }

    public String summaryLabel(Context ctx) {
        refreshPermission(ctx);
        return cached.summaryLabel();
    }

    public void refreshPermission(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        MockLocationPermissionChecker.CheckResult cr = MockLocationPermissionChecker.check(app);
        LocationMockManager.SavedLocation cfg = LocationMockManager.load(app);
        ServiceState st = LocationMockManager.isRunning(app) ? cached.serviceState : ServiceState.STOPPED;
        if (st == ServiceState.STOPPED && !cr.canStartMockService()) {
            st = ServiceState.PERMISSION_MISSING;
        }
        publish(st, cr.status, cfg, cr);
    }

    public void onInjecting(Context ctx, String msg) {
        LocationMockManager.SavedLocation cfg = LocationMockManager.load(ctx);
        MockLocationPermissionChecker.CheckResult cr = MockLocationPermissionChecker.check(ctx);
        publish(ServiceState.INJECTING, cr.status, cfg, cr, msg, now());
    }

    public void onStopped(Context ctx) {
        LocationMockManager.setRunning(ctx, false);
        refreshPermission(ctx);
    }

    public void onStarting(Context ctx) {
        LocationMockManager.SavedLocation cfg = LocationMockManager.load(ctx);
        MockLocationPermissionChecker.CheckResult cr = MockLocationPermissionChecker.check(ctx);
        publish(ServiceState.STARTING, cr.status, cfg, cr);
    }

    public void onProviderFailed(Context ctx, String err) {
        LocationMockManager.SavedLocation cfg = LocationMockManager.load(ctx);
        MockLocationPermissionChecker.CheckResult cr = MockLocationPermissionChecker.check(ctx);
        publish(ServiceState.PROVIDER_FAILED, cr.status, cfg, cr, err, cached.lastInjectAt);
    }

    public void onMockPermissionRevoked(Context ctx, String hint) {
        LocationMockManager.SavedLocation cfg = LocationMockManager.load(ctx);
        MockLocationPermissionChecker.CheckResult cr = MockLocationPermissionChecker.check(ctx);
        String msg = hint == null || hint.isEmpty() ? "请在开发者选项中重新勾选本应用" : hint;
        publish(ServiceState.PERMISSION_MISSING, MockLocationPermissionChecker.MockPermissionStatus.PROBE_SECURITY_DENIED,
                cfg, cr, msg, cached.lastInjectAt);
    }

    private void publish(ServiceState st, MockLocationPermissionChecker.MockPermissionStatus perm,
                         LocationMockManager.SavedLocation cfg, MockLocationPermissionChecker.CheckResult cr) {
        publish(st, perm, cfg, cr, cr.detail, cached.lastInjectAt);
    }

    private void publish(ServiceState st, MockLocationPermissionChecker.MockPermissionStatus perm,
                         LocationMockManager.SavedLocation cfg, MockLocationPermissionChecker.CheckResult cr,
                         String err, String injectAt) {
        cached = new State(st, perm, cfg.label, cfg.label, cfg.latitude, cfg.longitude,
                injectAt, err, cr.packageName, cr.uid, cr.appOpsMode);
        live.postValue(cached);
    }

    private static State defaultState() {
        return new State(ServiceState.STOPPED, MockLocationPermissionChecker.MockPermissionStatus.DENIED,
                "", "", 0, 0, "", "", "", 0, "");
    }

    private static String now() {
        return new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }
}
