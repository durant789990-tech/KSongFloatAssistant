package com.zzy.ksongfloat.capture;

import android.graphics.Bitmap;
import java.io.File;

public class CapturedImage {
    public final File file;
    public final int width;
    public final int height;
    public final long capturedAt;
    private Bitmap bitmap;

    public CapturedImage(File file, Bitmap bitmap, long capturedAt) {
        this.file = file;
        this.bitmap = bitmap;
        this.width = bitmap == null ? 0 : bitmap.getWidth();
        this.height = bitmap == null ? 0 : bitmap.getHeight();
        this.capturedAt = capturedAt;
    }
    public synchronized Bitmap getBitmap() { return bitmap; }
    public synchronized void release(boolean deleteFile) {
        if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        bitmap = null;
        if (deleteFile && file != null && file.exists()) file.delete();
    }
}
