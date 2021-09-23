package com.example.hakonsreader.activities

import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.responses.ApiResponse
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
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
    }

    /**
     * Tests that a fake API is injected to the test, which implicitly tests that a fake API
     * is injected into activities/fragments/views during tests.
     *
     * Tests some basic information from the implemented API calls to ensure they are read
     * from the test data files correctly
     */
    @Test
    fun testApiIsSetup(): Unit = runBlocking {
        // There are some comments in the fake API implementation about the response data that might be of use

        when (val userInfo = api.user().info()) {
            is ApiResponse.Success -> assertEquals("ArneRofinn", userInfo.value.username)
            is ApiResponse.Error -> fail("Incorrect API response returned from #user.info()")
        }

        when (val subreddits = api.subreditts().subscribedSubreddits()) {
            is ApiResponse.Success -> {
                assertEquals(12, subreddits.value.size)
                assertEquals("pics", subreddits.value.first().name)
                assertEquals("PrequelMemes", subreddits.value.last().name)
            }
            is ApiResponse.Error -> fail("Incorrect API response returned from #subscribedSubreddits()")
        }

        when (val subreddits = api.subreditts().defaultSubreddits()) {
            is ApiResponse.Success -> {
                assertEquals(10, subreddits.value.size)
                assertEquals("gadgets", subreddits.value.first().name)
                assertEquals("funny", subreddits.value.last().name)
            }
            is ApiResponse.Error -> fail("Incorrect API response returned")
        }

        when (val subreddits = api.subreditts().search("dogs")) {
            is ApiResponse.Success -> {
                assertEquals(5, subreddits.value.size)
                assertEquals("dogs", subreddits.value.first().name)
                assertEquals("woofbarkwoof", subreddits.value.last().name)
            }
            is ApiResponse.Error -> fail("Incorrect API response returned from #search()")
        }

        when (val posts = api.subreddit("").posts()) {
            is ApiResponse.Success -> {
                assertEquals(15, posts.value.size)
                assertEquals("Albino Indians", posts.value.first().title)
                assertEquals("I couldn’t come up with a good title for this genius.", posts.value.last().title)
            }
            is ApiResponse.Error -> fail("Incorrect API response returned from #subreddit.posts()")
        }
    }

}