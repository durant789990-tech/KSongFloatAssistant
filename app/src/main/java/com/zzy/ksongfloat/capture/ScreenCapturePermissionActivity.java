package com.zzy.ksongfloat.capture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Toast;

public class ScreenCapturePermissionActivity extends Activity {
    public static final int REQ = 9011;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            ScreenCaptureManager.get().notifyStatus(ScreenCaptureManager.Status.WAITING_PERMISSION, "等待截图授权...");
            MediaProjectionManager mpm = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mpm == null) throw new IllegalStateException("MediaProjectionManager 不可用");
            startActivityForResult(mpm.createScreenCaptureIntent(), REQ);
        } catch(Exception e) {
            Toast.makeText(this, "无法打开截图授权：" + e.getMessage(), Toast.LENGTH_LONG).show();
            ScreenCaptureManager.get().onPermissionDenied("无法打开截图授权：" + e.getMessage());
            finish();
        }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ) {
            if (resultCode == RESULT_OK && data != null) ScreenCaptureManager.get().onPermissionGranted(resultCode, data);
        else ScreenCaptureManager.get().onPermissionDenied("已取消截图授权");
        }
        finish();
    }
}
