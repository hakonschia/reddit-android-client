package com.example.hakonsreader.activities

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.example.hakonsreader.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test


@HiltAndroidTest
class MainActivityTest {


    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun init() {
        hiltRule.inject()
    }


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