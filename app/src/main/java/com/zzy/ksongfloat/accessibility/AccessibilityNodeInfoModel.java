package com.zzy.ksongfloat.accessibility;

public class AccessibilityNodeInfoModel {
    public final String text, contentDescription, resourceId, className;
    public final boolean editable, clickable;
    public AccessibilityNodeInfoModel(String text, String contentDescription, String resourceId, String className, boolean editable, boolean clickable) {
        this.text=n(text); this.contentDescription=n(contentDescription); this.resourceId=n(resourceId); this.className=n(className); this.editable=editable; this.clickable=clickable;
    }
    private static String n(String s){ return s==null?"":s; }
}
