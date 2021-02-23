package com.example.hakonsreader.activities

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.example.hakonsreader.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4ClassRunner::class)
class MainActivityTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    /**
     * Simple test for testing that MainActivity is displayed
     */
    @Test
    fun activityIsInView() {
        onView(withId(R.id.mainParentLayout)).check(matches(isDisplayed()))
    }

    /**
     * Tests that the bottom navigation view is displayed, with all of its navigation elements
     */
    @Test
    fun bottomNavIsDisplayed() {
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))

        // The nav bar items (these are always displayed)
        onView(withId(R.id.navHome)).check(matches(isDisplayed()))
        onView(withId(R.id.navSubreddit)).check(matches(isDisplayed()))
        onView(withId(R.id.navProfile)).check(matches(isDisplayed()))
        onView(withId(R.id.navSettings)).check(matches(isDisplayed()))
    }
}