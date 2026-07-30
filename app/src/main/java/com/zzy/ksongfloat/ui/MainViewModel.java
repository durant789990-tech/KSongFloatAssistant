package com.zzy.ksongfloat.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.zzy.ksongfloat.ai.AiConfigRepository;
import com.zzy.ksongfloat.ai.AiConfigState;
import com.zzy.ksongfloat.automation.AutomationOrchestrator;
import com.zzy.ksongfloat.automation.AutomationRuntime;
import com.zzy.ksongfloat.automation.AutomationSettings;
import com.zzy.ksongfloat.automation.AutomationSettingsRepository;
import com.zzy.ksongfloat.engine.AutomationEngineSelector;
import com.zzy.ksongfloat.location.LocationStateRepository;
import com.zzy.ksongfloat.runtime.ForegroundAppResolver;

public class MainViewModel extends AndroidViewModel {
    private final MediatorLiveData<DashboardState> dashboard = new MediatorLiveData<>();
    private final AiConfigRepository aiRepo = AiConfigRepository.get();

    public MainViewModel(@NonNull Application application) {
        super(application);
        aiRepo.attachPreferenceListener(application);
        dashboard.addSource(aiRepo.observeConfigState(), s -> publish());
        dashboard.addSource(LocationStateRepository.get().observe(), s -> publish());
        publish();
    }

    public LiveData<DashboardState> observeDashboard() {
        return dashboard;
    }

    public void refresh() {
        aiRepo.refresh(getApplication());
        LocationStateRepository.get().refreshPermission(getApplication());
        publish();
    }

    private void publish() {
        Application app = getApplication();
        AiConfigState ai = aiRepo.getCurrentState();
        AutomationSettings auto = AutomationSettingsRepository.load(app);
        AutomationRuntime.UiStatus st = AutomationRuntime.getStatus();
        String fg = ForegroundAppResolver.displayPackage(app);
        String loc = LocationStateRepository.get().getCurrent().summaryLabel();
        AutomationOrchestrator orch = AutomationOrchestrator.get();
        dashboard.postValue(new DashboardState(
                st,
                AutomationRuntime.getCurrentPage(),
                fg,
                AutomationEngineSelector.currentEngineLabel(app),
                AutomationRuntime.getLastAction(),
                AutomationRuntime.getProcessedCount(),
                AutomationRuntime.getConsecutiveFail(),
                AutomationRuntime.getFloatMessage(),
                ai,
                auto,
                loc,
                orch.isRunning(),
                orch.isPaused(),
                orch.getTaskQueue() == null ? 0 : orch.getTaskQueue().pendingCount(),
                orch.getSession() == null ? "" : orch.getSession().currentUserName
        ));
    }

    public static class DashboardState {
        public final AutomationRuntime.UiStatus status;
        public final String page;
        public final String foregroundPackage;
        public final String engine;
        public final String lastAction;
        public final int processed;
        public final int consecutiveFail;
        public final String message;
        public final AiConfigState ai;
        public final AutomationSettings automation;
        public final String locationStatus;
        public final boolean running;
        public final boolean paused;
        public final int queuePending;
        public final String currentUserName;

        public DashboardState(AutomationRuntime.UiStatus status, String page, String foregroundPackage,
                              String engine, String lastAction, int processed, int consecutiveFail,
                              String message, AiConfigState ai, AutomationSettings automation,
                              String locationStatus, boolean running, boolean paused,
                              int queuePending, String currentUserName) {
            this.status = status;
            this.page = page;
            this.foregroundPackage = foregroundPackage;
            this.engine = engine;
            this.lastAction = lastAction;
            this.processed = processed;
            this.consecutiveFail = consecutiveFail;
            this.message = message;
            this.ai = ai;
            this.automation = automation;
            this.locationStatus = locationStatus == null ? "未启用" : locationStatus;
            this.running = running;
            this.paused = paused;
            this.queuePending = queuePending;
            this.currentUserName = currentUserName == null ? "" : currentUserName;
        }
    }
}
