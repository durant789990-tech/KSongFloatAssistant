package com.zzy.ksongfloat.location;

import android.app.AppOpsManager;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;

/**
 * 模拟位置权限唯一检测器：AppOps 预检 + Test Provider 真实探测。
 */
public final class MockLocationPermissionChecker {
    public static final String PROBE_PROVIDER = "ksong_mock_permission_probe";
    public static final String MOCK_PROVIDER = "ksong_mock_location";

    public enum MockPermissionStatus {
        ALLOWED,
        DENIED,
        UNKNOWN_OEM,
        CHECK_ERROR,
        PROBE_SUCCEEDED,
        PROBE_SECURITY_DENIED,
        PROBE_PROVIDER_ERROR
    }

    public static class CheckResult {
        public final MockPermissionStatus status;
        public final String appOpsMode;
        public final String packageName;
        public final int uid;
        public final String detail;

        CheckResult(MockPermissionStatus status, String appOpsMode, String packageName, int uid, String detail) {
            this.status = status;
            this.appOpsMode = appOpsMode == null ? "" : appOpsMode;
            this.packageName = packageName == null ? "" : packageName;
            this.uid = uid;
            this.detail = detail == null ? "" : detail;
        }

        public boolean canStartMockService() {
            return status == MockPermissionStatus.PROBE_SUCCEEDED || status == MockPermissionStatus.ALLOWED;
        }

        public String userLabel() {
            switch (status) {
                case PROBE_SUCCEEDED:
                case ALLOWED:
                    return "模拟位置权限已验证可用";
                case PROBE_SECURITY_DENIED:
                case DENIED:
                    return "模拟位置权限未授权";
                case PROBE_PROVIDER_ERROR:
                    return "Provider 初始化失败";
                case UNKNOWN_OEM:
                    return "系统无法读取 AppOps，请执行真实探测";
                case CHECK_ERROR:
                    return "权限检测异常";
                default:
                    return detail.isEmpty() ? "未知状态" : detail;
            }
        }
    }

    public static CheckResult check(Context context) {
        Context app = context.getApplicationContext();
        String pkg = app.getPackageName();
        int uid = Process.myUid();
        String appOps = readAppOps(app, pkg, uid);
        CheckResult probe = probeMockLocationCapability(app);
        if (probe.status == MockPermissionStatus.PROBE_SUCCEEDED) {
            return probe;
        }
        if ("allow".equalsIgnoreCase(appOps)) {
            return new CheckResult(MockPermissionStatus.ALLOWED, appOps, pkg, uid,
                    "AppOps=allow，但真实探测未成功：" + probe.detail);
        }
        if (probe.status == MockPermissionStatus.PROBE_SECURITY_DENIED) {
            return probe;
        }
        if (probe.status == MockPermissionStatus.PROBE_PROVIDER_ERROR) {
            return probe;
        }
        if ("default".equalsIgnoreCase(appOps) || "ignore".equalsIgnoreCase(appOps) || "deny".equalsIgnoreCase(appOps)) {
            return new CheckResult(MockPermissionStatus.DENIED, appOps, pkg, uid,
                    "AppOps=" + appOps + "，请在开发者选项选择本应用");
        }
        return new CheckResult(MockPermissionStatus.UNKNOWN_OEM, appOps, pkg, uid, probe.detail);
    }

    public static CheckResult probeMockLocationCapability(Context context) {
        Context app = context.getApplicationContext();
        String pkg = app.getPackageName();
        int uid = Process.myUid();
        LocationManager lm = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            return new CheckResult(MockPermissionStatus.PROBE_PROVIDER_ERROR, "", pkg, uid, "LocationManager 不可用");
        }
        String provider = PROBE_PROVIDER;
        try {
            removeProviderQuietly(lm, provider);
            addTestProvider(lm, provider);
            lm.setTestProviderEnabled(provider, true);
            Location loc = new Location(provider);
            loc.setLatitude(39.9042);
            loc.setLongitude(116.4074);
            loc.setAccuracy(10f);
            loc.setTime(System.currentTimeMillis());
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                loc.setMock(true);
            }
            lm.setTestProviderLocation(provider, loc);
            return new CheckResult(MockPermissionStatus.PROBE_SUCCEEDED, readAppOps(app, pkg, uid), pkg, uid, "真实探测成功");
        } catch (SecurityException se) {
            return new CheckResult(MockPermissionStatus.PROBE_SECURITY_DENIED, readAppOps(app, pkg, uid), pkg, uid,
                    "SecurityException：请先在开发者选项选择「" + pkg + "」为模拟位置应用");
        } catch (IllegalArgumentException iae) {
            return new CheckResult(MockPermissionStatus.PROBE_PROVIDER_ERROR, readAppOps(app, pkg, uid), pkg, uid,
                    "Provider 参数错误：" + iae.getMessage());
        } catch (Exception e) {
            return new CheckResult(MockPermissionStatus.CHECK_ERROR, readAppOps(app, pkg, uid), pkg, uid,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            removeProviderQuietly(lm, provider);
        }
    }

    static void addTestProvider(LocationManager lm, String name) {
        lm.addTestProvider(name, false, false, false, false,
                true, false, false,
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE);
    }

    static void removeProviderQuietly(LocationManager lm, String name) {
        try {
            lm.removeTestProvider(name);
        } catch (Exception ignored) {
        }
    }

    private static String readAppOps(Context app, String pkg, int uid) {
        try {
            AppOpsManager ops = (AppOpsManager) app.getSystemService(Context.APP_OPS_SERVICE);
            if (ops == null) return "unknown";
            int mode;
            if (Build.VERSION.SDK_INT >= 29) {
                mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, uid, pkg);
            } else {
                mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, uid, pkg);
            }
            if (mode == AppOpsManager.MODE_ALLOWED) return "allow";
            if (mode == AppOpsManager.MODE_IGNORED) return "ignore";
            if (mode == AppOpsManager.MODE_ERRORED) return "deny";
            return "default";
        } catch (Exception e) {
            return "error:" + e.getClass().getSimpleName();
        }
    }
}
