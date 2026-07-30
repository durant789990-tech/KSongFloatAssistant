package com.zzy.ksongfloat;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class AiSettingsNavigationTest {
    @Before public void prepare() {
        Context c = ApplicationProvider.getApplicationContext();
        c.getSharedPreferences("first_run", Context.MODE_PRIVATE).edit().putBoolean("guide_seen", true).apply();
    }

    @Test public void openAiSettingsAndBack() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(allOf(isAssignableFrom(android.widget.Button.class), withText("AI 接口设置"))).perform(scrollTo(), click());
            onView(withText("接口信息")).check(matches(isDisplayed()));
            onView(withText("‹ 返回")).perform(click());
        }
    }

    @Test public void emptyFieldsShowValidationError() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(allOf(isAssignableFrom(android.widget.Button.class), withText("AI 接口设置"))).perform(scrollTo(), click());
            onView(withText("测试连接")).perform(scrollTo(), click());
            onView(withText("Base URL 不能为空")).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
}
