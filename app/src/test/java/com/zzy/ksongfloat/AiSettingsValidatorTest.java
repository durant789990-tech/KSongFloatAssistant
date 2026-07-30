package com.zzy.ksongfloat;

import com.zzy.ksongfloat.ui.settings.AiSettingsValidator;

import org.junit.Test;

import static org.junit.Assert.*;

public class AiSettingsValidatorTest {
    @Test public void rejectEmptyBaseUrl() {
        assertFalse(AiSettingsValidator.validateForSave("", "m", "60", 0.7, "1200").ok);
    }

    @Test public void rejectEmptyModel() {
        assertFalse(AiSettingsValidator.validateForSave("https://api.example.com/v1", "", "60", 0.7, "1200").ok);
    }

    @Test public void timeoutRange() {
        assertFalse(AiSettingsValidator.validateForSave("https://api.example.com/v1", "m", "3", 0.7, "1200").ok);
        assertFalse(AiSettingsValidator.validateForSave("https://api.example.com/v1", "m", "181", 0.7, "1200").ok);
        assertTrue(AiSettingsValidator.validateForSave("https://api.example.com/v1", "m", "60", 0.7, "1200").ok);
    }

    @Test public void temperatureRange() {
        assertFalse(AiSettingsValidator.validateForSave("https://api.example.com/v1", "m", "60", -0.1, "1200").ok);
        assertFalse(AiSettingsValidator.validateForSave("https://api.example.com/v1", "m", "60", 2.1, "1200").ok);
    }

    @Test public void maxTokensRange() {
        assertFalse(AiSettingsValidator.validateForSave("https://api.example.com/v1", "m", "60", 0.7, "50").ok);
        assertFalse(AiSettingsValidator.validateForSave("https://api.example.com/v1", "m", "60", 0.7, "5000").ok);
    }

    @Test public void testRequiresApiKey() {
        assertFalse(AiSettingsValidator.validateForTest("https://api.example.com/v1", false, "", "m", "60", 0.7, "1200").ok);
        assertTrue(AiSettingsValidator.validateForTest("https://api.example.com/v1", true, "", "m", "60", 0.7, "1200").ok);
    }
}
