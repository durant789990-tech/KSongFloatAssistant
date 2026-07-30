package com.zzy.ksongfloat.accessibility;

public class AccessibilitySnapshotRepository {
    private static final AccessibilitySnapshotRepository INSTANCE = new AccessibilitySnapshotRepository();
    public static AccessibilitySnapshotRepository get(){ return INSTANCE; }
    private volatile AccessibilitySnapshot latest = AccessibilitySnapshot.unavailable("");
    public synchronized void update(AccessibilitySnapshot s){ if(s!=null) latest=s; }
    public synchronized AccessibilitySnapshot latest(){ return latest; }
    public synchronized void clear(){ latest=AccessibilitySnapshot.unavailable(""); }
}
