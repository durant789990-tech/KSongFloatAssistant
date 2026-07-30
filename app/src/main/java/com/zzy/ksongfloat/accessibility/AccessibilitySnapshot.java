package com.zzy.ksongfloat.accessibility;

import java.util.ArrayList;
import java.util.List;

public class AccessibilitySnapshot {
    public final String packageName, windowTitle;
    public final long capturedAt;
    public final List<String> nodeTexts, contentDescriptions, resourceIds, editableTexts, clickableTexts, allVisibleTexts;
    public final int editableCount;
    public final boolean ksongForeground;
    public AccessibilitySnapshot(String packageName, String windowTitle, long capturedAt, List<String> nodeTexts, List<String> contentDescriptions, List<String> resourceIds, List<String> editableTexts, List<String> clickableTexts, List<String> allVisibleTexts, int editableCount, boolean ksongForeground) {
        this.packageName=n(packageName); this.windowTitle=n(windowTitle); this.capturedAt=capturedAt;
        this.nodeTexts=list(nodeTexts); this.contentDescriptions=list(contentDescriptions); this.resourceIds=list(resourceIds); this.editableTexts=list(editableTexts); this.clickableTexts=list(clickableTexts); this.allVisibleTexts=list(allVisibleTexts); this.editableCount=editableCount; this.ksongForeground=ksongForeground;
    }
    public static AccessibilitySnapshot unavailable(String pkg){ return new AccessibilitySnapshot(pkg,"",System.currentTimeMillis(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),0,false); }
    private static String n(String s){ return s==null?"":s; } private static List<String> list(List<String> l){ return l==null?new ArrayList<>():new ArrayList<>(l); }
}
