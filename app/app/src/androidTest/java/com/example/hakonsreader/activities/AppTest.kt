package com.example.hakonsreader.activities

import androidx.test.platform.app.InstrumentationRegistry
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.misc.Settings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.BeforeClass
import javax.inject.Inject


@HiltAndroidTest
class AppTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var api: RedditApi

    @Before
    fun init() {
        hiltRule.inject()

        // This can possibly be in @BeforeClass ?
        // Note: This MUST use "targetContext", not "context" ("targetContext" is the application with the
        // resources we use, "context" is the testing context)
        Settings.init(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /**
     * Tests that a fake API is injected to the test, which implicitly tests that a fake API
     * is injected into activities/fragments/views during tests
     */
    @Test
    fun testApiIsSetup(): Unit = runBlocking {
        when (val userInfo = api.user().info()) {
            is ApiResponse.Success -> {
                assertEquals("Hakonschia", userInfo.value.username)
            }
            is ApiResponse.Error -> fail("Incorrect API response returned")
        }
    }


    /**
     * Tests that [Settings] can be used during tests without throwing errors
     */
    @Test
    fun canUseSettings() {
        val someSetting = Settings.autoLoopVideos()
    }
}