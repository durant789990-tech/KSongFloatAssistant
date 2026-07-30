package com.zzy.ksongfloat.capture;

import java.util.ArrayList;
import java.util.List;

public class OcrResult {
    public final String fullText;
    public final List<OcrTextBlock> blocks;
    public final List<String> lines;
    public final boolean available;
    public final String warning;
    public OcrResult(String fullText, List<OcrTextBlock> blocks, List<String> lines, boolean available, String warning) {
        this.fullText = fullText == null ? "" : fullText;
        this.blocks = blocks == null ? new ArrayList<>() : blocks;
        this.lines = lines == null ? new ArrayList<>() : lines;
        this.available = available;
        this.warning = warning == null ? "" : warning;
    }
    public static OcrResult empty(String warning) { return new OcrResult("", new ArrayList<>(), new ArrayList<>(), false, warning); }
}
