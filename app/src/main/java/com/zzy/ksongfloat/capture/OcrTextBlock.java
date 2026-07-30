package com.zzy.ksongfloat.capture;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;

public class OcrTextBlock {
    public final String text;
    public final Rect boundingBox;
    public final List<String> lines;
    public OcrTextBlock(String text, Rect boundingBox, List<String> lines) {
        this.text = text == null ? "" : text;
        this.boundingBox = boundingBox;
        this.lines = lines == null ? new ArrayList<>() : lines;
    }
}
