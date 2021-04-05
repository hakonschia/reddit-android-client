package com.example.hakonsreader.activities

import android.app.Application
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.persistence.RedditDatabase
import com.example.hakonsreader.api.persistence.RedditUserInfoDatabase
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.SharedPreferencesManager
import com.example.hakonsreader.states.AppState
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject


@HiltAndroidTest
class MainActivityTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Inject
    lateinit var api: RedditApi

    @Inject
    lateinit var database: RedditDatabase

    @Inject
    lateinit var userInfoDatabase: RedditUserInfoDatabase

    @Before
    fun init() {
        hiltRule.inject()

        // This has to set before AppState.init()
        SharedPreferencesManager.create(InstrumentationRegistry.getInstrumentation().targetContext
                .getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, Application.MODE_PRIVATE)
        )
        AppState.init(api, database, userInfoDatabase)
        Settings.init(InstrumentationRegistry.getInstrumentation().targetContext)
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