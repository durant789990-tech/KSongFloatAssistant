package com.zzy.ksongfloat;

import android.app.Application;

import com.zzy.ksongfloat.ai.AiConfigRepository;
import com.zzy.ksongfloat.automation.AutomationSessionManager;
import com.zzy.ksongfloat.location.LocationStateRepository;
import com.zzy.ksongfloat.permission.PermissionStateRepository;

public class KSongApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AiConfigRepository.get().attachPreferenceListener(this);
        PermissionStateRepository.get().refresh(this);
        LocationStateRepository.get().refreshPermission(this);
        AutomationSessionManager.get().resetOnAppStart();
    }
}
