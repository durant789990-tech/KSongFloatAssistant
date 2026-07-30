package com.zzy.ksongfloat.location;

import android.content.Context;
import android.content.SharedPreferences;

public final class LocationMockManager {
    private static final String P = "location_mock";
    public static final String PROVIDER = MockLocationPermissionChecker.MOCK_PROVIDER;

    public static class SavedLocation {
        public double latitude = 39.9042;
        public double longitude = 116.4074;
        public float accuracy = 20f;
        public String label = "北京";
        public boolean enabled;
    }

    public static SavedLocation load(Context c) {
        SharedPreferences sp = c.getSharedPreferences(P, Context.MODE_PRIVATE);
        SavedLocation l = new SavedLocation();
        l.latitude = Double.longBitsToDouble(sp.getLong("lat", Double.doubleToLongBits(39.9042)));
        l.longitude = Double.longBitsToDouble(sp.getLong("lng", Double.doubleToLongBits(116.4074)));
        l.accuracy = sp.getFloat("acc", 20f);
        l.label = sp.getString("label", "北京");
        l.enabled = sp.getBoolean("enabled", false);
        return l;
    }

    public static void save(Context c, SavedLocation l) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
                .putLong("lat", Double.doubleToLongBits(l.latitude))
                .putLong("lng", Double.doubleToLongBits(l.longitude))
                .putFloat("acc", l.accuracy)
                .putString("label", l.label == null ? "" : l.label)
                .putBoolean("enabled", l.enabled)
                .apply();
    }

    public static void setRunning(Context c, boolean running) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putBoolean("running", running).apply();
    }

    public static boolean isRunning(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE).getBoolean("running", false);
    }

    public static void setLastResult(Context c, String msg) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString("lastResult", msg == null ? "" : msg).apply();
    }

    public static String getLastResult(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE).getString("lastResult", "");
    }

    public static String statusLabel(Context c) {
        return LocationStateRepository.get().summaryLabel(c);
    }

    public static SavedLocation preset(String city) {
        SavedLocation l = new SavedLocation();
        l.label = city;
        switch (city) {
            case "上海": l.latitude = 31.2304; l.longitude = 121.4737; break;
            case "广州": l.latitude = 23.1291; l.longitude = 113.2644; break;
            case "深圳": l.latitude = 22.5431; l.longitude = 114.0579; break;
            case "成都": l.latitude = 30.5728; l.longitude = 104.0668; break;
            default: l.latitude = 39.9042; l.longitude = 116.4074; l.label = "北京"; break;
        }
        return l;
    }
}
