package com.zzy.ksongfloat;

import com.zzy.ksongfloat.capture.PageTextResult;
import com.zzy.ksongfloat.classifier.PageClassificationResult;

public class DebugState {
    private static volatile PageTextResult pageTextResult; private static volatile PageClassificationResult classificationResult; private static volatile boolean screenshotDeleted; private static volatile long lastAnalyzedAt;
    public static synchronized void update(PageTextResult p, PageClassificationResult c, boolean deleted){ pageTextResult=p; classificationResult=c; screenshotDeleted=deleted; lastAnalyzedAt=System.currentTimeMillis(); }
    public static PageTextResult page(){ return pageTextResult; } public static PageClassificationResult cls(){ return classificationResult; } public static boolean screenshotDeleted(){ return screenshotDeleted; } public static long lastAnalyzedAt(){ return lastAnalyzedAt; }
    public static synchronized void clear(){ pageTextResult=null; classificationResult=null; screenshotDeleted=false; lastAnalyzedAt=0; }
}
