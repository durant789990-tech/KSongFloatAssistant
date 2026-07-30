package com.zzy.ksongfloat;

import com.zzy.ksongfloat.accessibility.AccessibilityStateDetector;
import com.zzy.ksongfloat.floating.FloatingTouchController;

import org.junit.Test;

import static org.junit.Assert.*;

public class RuntimeAndFloatingTest {
    @Test public void accessibilityComponentMatchesFullAndShortName() {
        String flat = "com.zzy.ksongfloat/com.zzy.ksongfloat.accessibility.KSongAccessibilityService";
        String shortFlat = "com.zzy.ksongfloat/.accessibility.KSongAccessibilityService";
        assertTrue(AccessibilityStateDetector.matches(flat, flat, shortFlat));
        assertTrue(AccessibilityStateDetector.matches(shortFlat, flat, shortFlat));
        assertFalse(AccessibilityStateDetector.matches("com.other/.OtherService", flat, shortFlat));
    }

    @Test public void floatingGestureClassifiesClickDragAndLongPress() {
        assertEquals(FloatingTouchController.Gesture.CLICK, FloatingTouchController.classify(2, 3, 120, 12, 500));
        assertEquals(FloatingTouchController.Gesture.DRAG, FloatingTouchController.classify(20, 1, 120, 12, 500));
        assertEquals(FloatingTouchController.Gesture.LONG_PRESS, FloatingTouchController.classify(2, 3, 700, 12, 500));
    }
}
