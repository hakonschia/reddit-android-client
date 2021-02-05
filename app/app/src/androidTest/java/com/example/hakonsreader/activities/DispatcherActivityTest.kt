package com.example.hakonsreader.api

import android.content.Context
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hakonsreader.activites.DispatcherActivity
import com.example.hakonsreader.activites.VideoActivity
import com.example.hakonsreader.misc.createIntent
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class DispatcherActivityTest {
    lateinit var instrumentationContext: Context

    @Before
    fun setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
    }

    /**
     * This tests that [DispatcherActivity]
     */
    @Test
    fun dispatcherCreatesCorrespondingIntent() {
        var intent = createIntent("https://reddit.com", instrumentationContext)
        assertIntentIsForClass(intent, VideoActivity::class.java)
    }

    /**
     * Asserts that an intent is for a given class
     *
     * @param intent The intent to verify
     * @param clazz The class the intent should be for
     */
    private fun <T>assertIntentIsForClass(intent: Intent, clazz: Class<T>) {
        val actual = intent.component?.className
        val expected = clazz.name
        assertEquals("Expected '$expected', got '$actual'", expected, actual)
    }
}