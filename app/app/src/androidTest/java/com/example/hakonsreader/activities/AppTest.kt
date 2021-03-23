package com.example.hakonsreader.activities

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hakonsreader.App
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.responses.ApiResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
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

    @Test
    fun testApiIsSetup(): Unit = runBlocking {
        when (val userInfo = api.user().info()) {
            is ApiResponse.Success -> {
                assertEquals(userInfo.value.username, "Hakonschia")
            }
            is ApiResponse.Error -> fail()
        }
    }
}