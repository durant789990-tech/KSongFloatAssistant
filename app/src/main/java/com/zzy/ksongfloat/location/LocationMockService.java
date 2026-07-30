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
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.zzy.ksongfloat.MainActivity;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** 前台服务：每秒向 GPS / NETWORK Provider 注入带漂移的模拟坐标。 */
public class LocationMockService extends Service {
    public static final String ACTION_STOP = "com.zzy.ksongfloat.action.STOP_MOCK_LOCATION";
    private static final String CH = "mock_location";
    private static final String[] PROVIDERS = {
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationMockManager.PROVIDER
    };
    private static final Random RND = new Random();
    private ScheduledExecutorService scheduler;
    private volatile boolean injecting;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        ensureProviders();
        startScheduler();
        return START_NOT_STICKY;
    }

    private void startScheduler() {
        stopScheduler();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mock-location-inject");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::injectAll, 0, 1, TimeUnit.SECONDS);
    }

    private void injectAll() {
        if (!injectOnce()) return;
        LocationMockManager.SavedLocation cfg = LocationMockManager.load(this);
        if (!injecting) {
            injecting = true;
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(3001, buildNotification("已模拟 · " + cfg.label));
            }
        }
    }

    private boolean injectOnce() {
        try {
            LocationMockManager.SavedLocation cfg = LocationMockManager.load(this);
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) throw new IllegalStateException("LocationManager 不可用");
            ensureProviders();
            double lat = cfg.latitude + drift();
            double lng = cfg.longitude + drift();
            long now = System.currentTimeMillis();
            boolean securityDenied = false;
            for (String provider : PROVIDERS) {
                try {
                    Location loc = new Location(provider);
                    loc.setLatitude(lat);
                    loc.setLongitude(lng);
                    loc.setAccuracy(Math.max(5f, cfg.accuracy));
                    loc.setTime(now);
                    loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        loc.setMock(true);
                    }
                    lm.setTestProviderLocation(provider, loc);
                } catch (SecurityException se) {
                    securityDenied = true;
                    String hint = "请在开发者选项中重新勾选本应用";
                    LocationStateRepository.get().onMockPermissionRevoked(this, hint);
                    LocationMockManager.setLastResult(this, hint);
                    Log.w("LocationMockService", "setTestProviderLocation SecurityException: " + se.getMessage());
                } catch (Exception e) {
                    Log.w("LocationMockService", provider + " 注入失败：" + e.getMessage());
                }
            }
            if (securityDenied) {
                return false;
            }
            LocationMockManager.setRunning(this, true);
            LocationMockManager.setLastResult(this, "已注入 " + lat + "," + lng);
            LocationStateRepository.get().onInjecting(this, cfg.label);
            return true;
        } catch (SecurityException se) {
            String hint = "请在开发者选项中重新勾选本应用";
            LocationStateRepository.get().onMockPermissionRevoked(this, hint);
            LocationMockManager.setLastResult(this, hint);
            return false;
        } catch (Exception e) {
            LocationStateRepository.get().onProviderFailed(this, e.getMessage());
            LocationMockManager.setLastResult(this, "注入失败：" + e.getMessage());
            return false;
        }
    }

    private static double drift() {
        return (RND.nextDouble() - 0.5) * 0.0001; // ±0.00005
    }

    private void ensureProviders() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return;
        for (String p : PROVIDERS) {
            try {
                MockLocationPermissionChecker.removeProviderQuietly(lm, p);
                MockLocationPermissionChecker.addTestProvider(lm, p);
                lm.setTestProviderEnabled(p, true);
            } catch (Exception ignored) {
            }
        }
    }

    private void stopScheduler() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void stopMock() {
        stopScheduler();
        injecting = false;
        LocationMockManager.setRunning(this, false);
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                for (String p : PROVIDERS) {
                    try {
                        lm.setTestProviderEnabled(p, false);
                        MockLocationPermissionChecker.removeProviderQuietly(lm, p);
                    } catch (Exception ignored) {
                    }
                }
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
        Intent open = new Intent(this, MainActivity.class);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CH) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("K歌助手 · 模拟定位")
                .setContentText(text)
                .setContentIntent(android.app.PendingIntent.getActivity(this, 3002, open,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT | pendingImmutable()))
                .setOngoing(true)
                .build();
    }

    private static int pendingImmutable() {
        return Build.VERSION.SDK_INT >= 23 ? android.app.PendingIntent.FLAG_IMMUTABLE : 0;
    }
}
