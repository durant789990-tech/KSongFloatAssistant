package com.zzy.ksongfloat.location;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;

import com.zzy.ksongfloat.ui.theme.AppTheme;
import com.zzy.ksongfloat.util.UiKit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationSettingsActivity extends AppCompatActivity {
    private TextView status, diag, injectInfo;
    private EditText lat, lng, acc, cityField;
    private CheckBox slightMove;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        root = UiKit.root(this);
        scroll.addView(root);
        setContentView(scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom + UiKit.dp(this, 16));
            return insets;
        });
        buildUi();
        LocationStateRepository.get().observe().observe(this, s -> refreshLabels());
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocationStateRepository.get().refreshPermission(this);
        refreshLabels();
    }

    private void buildUi() {
        root.removeAllViews();
        LocationMockManager.SavedLocation cfg = LocationMockManager.load(this);
        root.addView(UiKit.title(this, "虚拟定位设置"));
        root.addView(UiKit.caption(this, "包名：" + getPackageName() + " · UID " + Process.myUid()));

        LinearLayout permCard = UiKit.card(this);
        permCard.addView(UiKit.text(this, "当前授权状态", AppTheme.BODY_SP, AppTheme.TEXT_PRIMARY, true));
        status = UiKit.caption(this, "");
        diag = UiKit.caption(this, "");
        permCard.addView(status);
        permCard.addView(diag);
        Button recheck = UiKit.button(this, "重新检测权限", AppTheme.BRAND);
        UiKit.attachClick(recheck, v -> {
            LocationStateRepository.get().refreshPermission(this);
            MockLocationPermissionChecker.CheckResult r = MockLocationPermissionChecker.probeMockLocationCapability(this);
            toast(r.userLabel());
            refreshLabels();
        });
        permCard.addView(recheck);
        Button dev = UiKit.button(this, "去开发者选项", AppTheme.BRAND_DARK);
        UiKit.attachClick(dev, v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)));
        permCard.addView(dev);
        root.addView(permCard);

        LinearLayout targetCard = UiKit.card(this);
        targetCard.addView(UiKit.text(this, "目标位置", AppTheme.BODY_SP, AppTheme.TEXT_PRIMARY, true));
        cityField = field(cfg.label);
        lat = field(String.valueOf(cfg.latitude));
        lng = field(String.valueOf(cfg.longitude));
        acc = field(String.valueOf(cfg.accuracy));
        targetCard.addView(UiKit.caption(this, "城市名称"));
        targetCard.addView(cityField);
        Button beijing = UiKit.button(this, "模拟北京", AppTheme.BRAND);
        UiKit.attachClick(beijing, v -> applyPreset("北京"));
        targetCard.addView(beijing);
        targetCard.addView(UiKit.caption(this, "纬度 / 经度 / 精度(米)"));
        targetCard.addView(lat);
        targetCard.addView(lng);
        targetCard.addView(acc);
        slightMove = new CheckBox(this);
        slightMove.setText("模拟轻微移动（默认关闭）");
        targetCard.addView(slightMove);
        root.addView(targetCard);

        LinearLayout svcCard = UiKit.card(this);
        svcCard.addView(UiKit.text(this, "服务状态", AppTheme.BODY_SP, AppTheme.TEXT_PRIMARY, true));
        injectInfo = UiKit.caption(this, "");
        svcCard.addView(injectInfo);
        Button start = UiKit.button(this, "开始模拟定位", AppTheme.BRAND);
        UiKit.attachClick(start, v -> startMock());
        Button stop = UiKit.button(this, "停止模拟定位", AppTheme.DANGER);
        UiKit.attachClick(stop, v -> stopMock());
        Button test = UiKit.button(this, "测试定位", AppTheme.BRAND_DARK);
        UiKit.attachClick(test, v -> testLocation());
        svcCard.addView(start);
        svcCard.addView(stop);
        svcCard.addView(test);
        root.addView(svcCard);
        refreshLabels();
    }

    private void applyPreset(String city) {
        LocationMockManager.SavedLocation s = LocationMockManager.preset(city);
        cityField.setText(s.label);
        lat.setText(String.valueOf(s.latitude));
        lng.setText(String.valueOf(s.longitude));
    }

    private EditText field(String v) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setText(v);
        return e;
    }

    private void startMock() {
        MockLocationPermissionChecker.CheckResult perm = MockLocationPermissionChecker.probeMockLocationCapability(this);
        if (!perm.canStartMockService()) {
            new AlertDialog.Builder(this)
                    .setTitle("需要启用模拟位置")
                    .setMessage("请前往开发者选项 → 选择模拟位置信息应用 → 选择当前安装的 K歌助手。\n\n当前包名："
                            + getPackageName() + "\n\n若选错旧版同名 App，请先卸载旧版再重新选择。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("去设置", (d, w) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)))
                    .show();
            return;
        }
        try {
            LocationMockManager.SavedLocation s = new LocationMockManager.SavedLocation();
            s.latitude = Double.parseDouble(lat.getText().toString().trim());
            s.longitude = Double.parseDouble(lng.getText().toString().trim());
            s.accuracy = Float.parseFloat(acc.getText().toString().trim());
            s.label = cityField.getText().toString().trim();
            if (s.label.isEmpty()) s.label = s.latitude + "," + s.longitude;
            s.enabled = true;
            LocationMockManager.save(this, s);
            Intent i = new Intent(this, LocationMockService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
            toast("正在启动模拟定位服务…");
            refreshLabels();
        } catch (Exception e) {
            toast("参数错误：" + e.getMessage());
        }
    }

    private void stopMock() {
        Intent i = new Intent(this, LocationMockService.class);
        i.setAction(LocationMockService.ACTION_STOP);
        startService(i);
        toast("已请求停止");
        refreshLabels();
    }

    private void testLocation() {
        if (!LocationMockManager.isRunning(this)) {
            toast("模拟服务未运行");
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            toast("缺少精确位置权限，无法读取验证结果，但模拟位置授权可能已有效");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 4401);
            return;
        }
        LocationMockManager.SavedLocation target = LocationMockManager.load(this);
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) {
            toast("LocationManager 不可用");
            return;
        }
        Location best = null;
        try {
            best = lm.getLastKnownLocation(LocationMockManager.PROVIDER);
            if (best == null) best = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
        }
        if (best == null) {
            toast("服务正在运行，但系统尚未返回目标坐标");
            return;
        }
        float[] dist = new float[1];
        Location.distanceBetween(target.latitude, target.longitude, best.getLatitude(), best.getLongitude(), dist);
        boolean fresh = System.currentTimeMillis() - best.getTime() < 15000;
        String mockFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && best.isMock() ? " · isMock=true" : "";
        String msg = "读取坐标 " + best.getLatitude() + "," + best.getLongitude()
                + "\n距目标 " + (int) dist[0] + "m · " + (fresh ? "时间较新" : "时间偏旧") + mockFlag;
        if (dist[0] < 500 && fresh) {
            toast("已注入并读取到接近目标坐标\n" + msg);
        } else {
            toast("坐标已注入，但读取结果与目标仍有偏差\n" + msg);
        }
    }

    private void refreshLabels() {
        LocationStateRepository.State st = LocationStateRepository.get().getCurrent();
        MockLocationPermissionChecker.CheckResult cr = MockLocationPermissionChecker.check(this);
        if (status != null) {
            status.setText("AppOps：" + cr.appOpsMode + "\n探测：" + cr.status.name() + "\n" + cr.userLabel());
        }
        if (diag != null) {
            diag.setText("服务：" + st.serviceState.name()
                    + "\n最近注入：" + (st.lastInjectAt.isEmpty() ? "无" : st.lastInjectAt)
                    + "\n最近错误：" + (st.lastError.isEmpty() ? "无" : st.lastError));
        }
        if (injectInfo != null) {
            injectInfo.setText(LocationMockManager.getLastResult(this));
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
