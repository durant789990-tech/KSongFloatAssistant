package com.zzy.ksongfloat.automation;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class RandomDelayHelper {
    private static final Random RND = new Random();

    public static void delay(AutomationSettings s, AtomicBoolean stop) throws InterruptedException {
        delay(s, stop, 1.0);
    }

    public static void delay(AutomationSettings s, AtomicBoolean stop, double scale) throws InterruptedException {
        int min = Math.max(500, s.delayMinMs);
        int max = Math.max(min + 200, s.delayMaxMs);
        int ms = min + RND.nextInt(max - min + 1);
        sleepScaled(ms, scale, stop);
    }

    public static void sleepScaled(int ms, double scale, AtomicBoolean stop) throws InterruptedException {
        long end = System.currentTimeMillis() + (long) (ms * scale);
        while (System.currentTimeMillis() < end) {
            if (stop != null && stop.get()) throw new InterruptedException("stopped");
            Thread.sleep(120);
        }
    }

    public static int jitter(int base, int spread) {
        return base + RND.nextInt(Math.max(1, spread));
    }
}
