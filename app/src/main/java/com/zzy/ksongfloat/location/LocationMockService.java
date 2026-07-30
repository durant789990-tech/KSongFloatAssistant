package com.zzy.ksongfloat.location;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import com.zzy.ksongfloat.MainActivity;

public class LocationMockService extends Service {
    public static final String ACTION_STOP = "com.zzy.ksongfloat.action.STOP_MOCK_LOCATION";
    private static final String CH = "mock_location";
    private static volatile boolean injecting;
    private Handler handler;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (injectOnce()) {
                if (handler != null) handler.postDelayed(this, 1500);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 虚拟定位功能已禁用，禁止启动前台服务或注入线程
        stopSelf();
        return START_NOT_STICKY;
    }

    @SuppressWarnings("unused")
    private int onStartCommandDisabled(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopMock();
            stopSelf();
            return START_NOT_STICKY;
        }
        LocationStateRepository.get().onStarting(this);
        MockLocationPermissionChecker.CheckResult perm = MockLocationPermissionChecker.probeMockLocationCapability(this);
        if (!perm.canStartMockService()) {
            LocationStateRepository.get().onProviderFailed(this, perm.detail);
            LocationMockManager.setLastResult(this, perm.userLabel());
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(3001, buildNotification("正在启动模拟定位…"));
        injecting = false;
        ensureProvider();
        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_NOT_STICKY;
    }

    private boolean injectOnce() {
        try {
            LocationMockManager.SavedLocation cfg = LocationMockManager.load(this);
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) throw new IllegalStateException("LocationManager 不可用");
            ensureProvider();
            Location loc = new Location(LocationMockManager.PROVIDER);
            loc.setLatitude(cfg.latitude);
            loc.setLongitude(cfg.longitude);
            loc.setAccuracy(cfg.accuracy);
            loc.setTime(System.currentTimeMillis());
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                loc.setMock(true);
            }
            lm.setTestProviderLocation(LocationMockManager.PROVIDER, loc);
            LocationMockManager.setRunning(this, true);
            LocationMockManager.setLastResult(this, "已注入 " + cfg.latitude + "," + cfg.longitude);
            if (!injecting) {
                injecting = true;
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) nm.notify(3001, buildNotification(cfg.label + " · " + cfg.latitude + "," + cfg.longitude));
            }
            LocationStateRepository.get().onInjecting(this, "注入成功");
            return true;
        } catch (SecurityException se) {
            LocationStateRepository.get().onProviderFailed(this, "SecurityException：" + se.getMessage());
            LocationMockManager.setLastResult(this, "权限不足：请在开发者选项选择本应用");
            stopMock();
            stopSelf();
            return false;
        } catch (Exception e) {
            LocationStateRepository.get().onProviderFailed(this, e.getMessage());
            LocationMockManager.setLastResult(this, "注入失败：" + e.getMessage());
            return false;
        }
    }

    private void ensureProvider() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return;
        String p = LocationMockManager.PROVIDER;
        MockLocationPermissionChecker.removeProviderQuietly(lm, p);
        MockLocationPermissionChecker.addTestProvider(lm, p);
        lm.setTestProviderEnabled(p, true);
    }

    private void stopMock() {
        handler.removeCallbacks(tick);
        injecting = false;
        LocationMockManager.setRunning(this, false);
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                lm.setTestProviderEnabled(LocationMockManager.PROVIDER, false);
                MockLocationPermissionChecker.removeProviderQuietly(lm, LocationMockManager.PROVIDER);
            }
        } catch (Exception ignored) {
        }
        LocationMockManager.setLastResult(this, "已停止模拟定位");
        LocationStateRepository.get().onStopped(this);
    }

    @Override
    public void onDestroy() {
        stopMock();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification(String text) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CH, "模拟定位", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(c);
        }
        Intent stop = new Intent(this, LocationMockService.class);
        stop.setAction(ACTION_STOP);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CH) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("K歌助手 · 模拟定位")
                .setContentText(text)
                .setOngoing(true)
                .build();
    }
}
