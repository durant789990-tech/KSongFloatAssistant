package com.zzy.ksongfloat.runtime;

import java.util.concurrent.CopyOnWriteArrayList;

public class AssistantStateRepository {
    private static volatile AssistantRuntimeSnapshot snapshot = AssistantRuntimeSnapshot.initial();
    private static final CopyOnWriteArrayList<AssistantStateObserver> observers = new CopyOnWriteArrayList<>();

    public static AssistantRuntimeSnapshot get() { return snapshot; }

    public static void update(AssistantRuntimeSnapshot next) {
        if (next == null) return;
        snapshot = next;
        for (AssistantStateObserver observer : observers) {
            try { observer.onAssistantStateChanged(next); } catch (Exception ignored) {}
        }
    }

    public static void observe(AssistantStateObserver observer) {
        if (observer == null) return;
        observers.addIfAbsent(observer);
        observer.onAssistantStateChanged(snapshot);
    }

    public static void remove(AssistantStateObserver observer) {
        if (observer != null) observers.remove(observer);
    }
}
