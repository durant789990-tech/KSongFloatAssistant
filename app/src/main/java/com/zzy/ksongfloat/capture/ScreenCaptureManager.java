package com.zzy.ksongfloat.capture;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenCaptureManager {
    public enum Status { WAITING_PERMISSION, CAPTURING, SUCCESS, FAILED, CANCELLED }
    public interface CaptureCallback { void onStatus(Status status, String message); void onSuccess(CapturedImage image); void onError(Status status, String message); }
    private static final ScreenCaptureManager INSTANCE = new ScreenCaptureManager();
    public static ScreenCaptureManager get(){ return INSTANCE; }
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Context appContext; private volatile CaptureCallback callback;

    public boolean isRunning(){ return running.get(); }
    public void requestSingleCapture(Context context, CaptureCallback cb) {
        if (!running.compareAndSet(false, true)) { if(cb!=null) cb.onError(Status.FAILED, "已有截图任务正在运行"); return; }
        this.appContext = context.getApplicationContext(); this.callback = cb;
        notifyStatus(Status.WAITING_PERMISSION, "等待截图授权...");
        Intent i = new Intent(appContext, ScreenCapturePermissionActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(i);
    }
    public void notifyStatus(Status s, String m){ CaptureCallback cb=callback; if(cb!=null) cb.onStatus(s,m); }
    public void onPermissionDenied(String msg){ CaptureCallback cb=callback; running.set(false); if(cb!=null) cb.onError(Status.CANCELLED, msg==null?"已取消截图授权":msg); clear(); }
    public void onPermissionGranted(int resultCode, Intent data){
        final Context c=appContext; final CaptureCallback cb=callback;
        if(c==null){ running.set(false); return; }
        notifyStatus(Status.CAPTURING, "正在截图...");
        new Thread(() -> doCapture(c, resultCode, data, cb), "single-screen-capture").start();
    }
    private void doCapture(Context context, int resultCode, Intent data, CaptureCallback cb){
        MediaProjection projection=null; ImageReader reader=null; VirtualDisplay vd=null; HandlerThread ht=null; Bitmap bitmap=null; Image image=null;
        try{
            WindowManager wm=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm=new DisplayMetrics(); wm.getDefaultDisplay().getRealMetrics(dm);
            int w=dm.widthPixels,h=dm.heightPixels,dpi=dm.densityDpi;
            MediaProjectionManager mpm=(MediaProjectionManager)context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            projection=mpm.getMediaProjection(resultCode,data);
            if(projection==null) throw new IllegalStateException("截图授权无效");
            ht=new HandlerThread("capture-frame"); ht.start(); Handler handler=new Handler(ht.getLooper());
            if (Build.VERSION.SDK_INT >= 34) {
                MediaProjection finalProjection = projection;
                projection.registerCallback(new MediaProjection.Callback(){ @Override public void onStop(){} }, handler);
            }
            reader=ImageReader.newInstance(w,h, PixelFormat.RGBA_8888,2);
            vd=projection.createVirtualDisplay("ksong-single-frame",w,h,dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,handler);
            long end=System.currentTimeMillis()+2500;
            while(System.currentTimeMillis()<end && image==null){ Thread.sleep(80); image=reader.acquireLatestImage(); }
            if(image==null) throw new IllegalStateException("未获取到屏幕图像");
            Image.Plane plane=image.getPlanes()[0]; ByteBuffer buffer=plane.getBuffer(); int pixelStride=plane.getPixelStride(); int rowStride=plane.getRowStride(); int rowPadding=rowStride-pixelStride*w;
            Bitmap padded=Bitmap.createBitmap(w+rowPadding/pixelStride,h,Bitmap.Config.ARGB_8888); padded.copyPixelsFromBuffer(buffer);
            bitmap=Bitmap.createBitmap(padded,0,0,w,h); padded.recycle();
            File f=new File(context.getCacheDir(),"screen_"+System.currentTimeMillis()+".png");
            FileOutputStream fos=new FileOutputStream(f); bitmap.compress(Bitmap.CompressFormat.PNG,90,fos); fos.close();
            CapturedImage ci=new CapturedImage(f, bitmap, System.currentTimeMillis()); bitmap=null;
            if(cb!=null){ cb.onStatus(Status.SUCCESS,"截图完成"); cb.onSuccess(ci); }
        }catch(Exception e){ if(cb!=null) cb.onError(Status.FAILED,"截图失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage())); }
        finally{
            try{ if(image!=null) image.close(); }catch(Exception ignored){}
            try{ if(vd!=null) vd.release(); }catch(Exception ignored){}
            try{ if(reader!=null) reader.close(); }catch(Exception ignored){}
            try{ if(projection!=null) projection.stop(); }catch(Exception ignored){}
            try{ if(ht!=null) ht.quitSafely(); }catch(Exception ignored){}
            try{ if(bitmap!=null && !bitmap.isRecycled()) bitmap.recycle(); }catch(Exception ignored){}
            running.set(false); clear();
        }
    }
    public void cancel(){ running.set(false); clear(); }
    private void clear(){ appContext=null; callback=null; }
}
