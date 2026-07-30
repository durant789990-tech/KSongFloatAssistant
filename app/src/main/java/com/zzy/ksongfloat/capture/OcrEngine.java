package com.zzy.ksongfloat.capture;

import android.graphics.Bitmap;
import android.graphics.Rect;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.TextRecognizer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class OcrEngine {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public Future<OcrResult> recognizeAsync(final Bitmap bitmap) {
        return executor.submit(new Callable<OcrResult>() {
            @Override public OcrResult call() {
            if (bitmap == null || bitmap.isRecycled()) return OcrResult.empty("截图为空，无法进行 OCR");
                TextRecognizer recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
                try {
                    Text text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)));
                    List<OcrTextBlock> blocks = new ArrayList<>();
                    List<String> lines = new ArrayList<>();
                    for (Text.TextBlock b : text.getTextBlocks()) {
                        List<String> bl = new ArrayList<>();
                        for (Text.Line line : b.getLines()) { String c=TextCleaner.cleanLine(line.getText()); if(!c.isEmpty()){ bl.add(c); lines.add(c);} }
                        String bt = TextCleaner.cleanLine(b.getText());
                        Rect r = b.getBoundingBox();
                        if (!bt.isEmpty() || !bl.isEmpty()) blocks.add(new OcrTextBlock(bt, r, bl));
                    }
                    List<String> cleaned = TextCleaner.uniqueCleanLines(lines);
                    String full = TextCleaner.joinLimit(cleaned, 6000);
            if (full.isEmpty()) return OcrResult.empty("OCR 未识别到文字");
                    return new OcrResult(full, blocks, cleaned, true, "");
                } catch (Exception e) {
            return OcrResult.empty("OCR 识别失败：" + safe(e.getMessage()));
                } finally { try { recognizer.close(); } catch(Exception ignored){} }
            }
        });
    }
    public void shutdown() { executor.shutdownNow(); }
    private String safe(String s){ return s==null?"":(" 详情："+s); }
}
