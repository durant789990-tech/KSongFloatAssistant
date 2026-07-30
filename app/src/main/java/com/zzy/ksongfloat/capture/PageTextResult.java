package com.zzy.ksongfloat.capture;

import java.util.ArrayList; import java.util.List;
public class PageTextResult {
    public final String packageName, windowTitle, accessibilityText, ocrText, mergedText;
    public final List<String> resourceIds, contentDescriptions, warnings;
    public final long collectedAt, screenshotAt, accessibilityCapturedAt;
    public final boolean accessibilityAvailable, ocrAvailable, screenshotDeleted;
    public final int editableCount;
    public PageTextResult(String packageName,String windowTitle,String accessibilityText,String ocrText,String mergedText,List<String> resourceIds,List<String> contentDescriptions,long collectedAt,long screenshotAt,long accessibilityCapturedAt,boolean accessibilityAvailable,boolean ocrAvailable,boolean screenshotDeleted,int editableCount,List<String>warnings){
        this.packageName=n(packageName);this.windowTitle=n(windowTitle);this.accessibilityText=n(accessibilityText);this.ocrText=n(ocrText);this.mergedText=n(mergedText);this.resourceIds=list(resourceIds);this.contentDescriptions=list(contentDescriptions);this.collectedAt=collectedAt;this.screenshotAt=screenshotAt;this.accessibilityCapturedAt=accessibilityCapturedAt;this.accessibilityAvailable=accessibilityAvailable;this.ocrAvailable=ocrAvailable;this.screenshotDeleted=screenshotDeleted;this.editableCount=editableCount;this.warnings=list(warnings);
    }
    private static String n(String s){return s==null?"":s;} private static List<String> list(List<String> l){return l==null?new ArrayList<>():new ArrayList<>(l);} }
