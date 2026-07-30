package com.zzy.ksongfloat.capture;

import com.zzy.ksongfloat.accessibility.AccessibilitySnapshot;
import java.util.ArrayList; import java.util.List;

public class PageTextCollector {
    public PageTextResult collect(AccessibilitySnapshot snap, OcrResult ocr, long screenshotAt, boolean screenshotDeleted){
        List<String> warnings=new ArrayList<>();
        List<String> accLines = snap==null?new ArrayList<>():snap.allVisibleTexts;
        List<String> ocrLines = ocr==null?new ArrayList<>():ocr.lines;
        boolean accOk=snap!=null && snap.ksongForeground && !accLines.isEmpty(); boolean ocrOk=ocr!=null && ocr.available && !ocrLines.isEmpty();
        if(!accOk) warnings.add("无障碍暂未捕获到目标页面文本"); if(!ocrOk) warnings.add(ocr==null?"OCR 未运行":(ocr.warning.isEmpty()?"OCR 未识别到文字":ocr.warning));
        List<String> merged=TextCleaner.mergePreferLonger(accLines, ocrLines, 220);
        String accText=TextCleaner.joinLimit(TextCleaner.uniqueCleanLines(accLines),5000); String ocrText=TextCleaner.joinLimit(TextCleaner.uniqueCleanLines(ocrLines),5000); String mergedText=TextCleaner.joinLimit(merged,8000);
        return new PageTextResult(snap==null?"":snap.packageName, snap==null?"":snap.windowTitle, accText, ocrText, mergedText, snap==null?new ArrayList<>():snap.resourceIds, snap==null?new ArrayList<>():snap.contentDescriptions, System.currentTimeMillis(), screenshotAt, snap==null?0:snap.capturedAt, accOk, ocrOk, screenshotDeleted, snap==null?0:snap.editableCount, warnings);
    }
}
