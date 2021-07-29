package com.example.hakonsreader.app.misc

import com.example.hakonsreader.misc.parseYouTubeTimestamp
import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeTimestampParseTest {

    /**
     * Tests that a link with a timestamp given with only a number (no s at the end) is parsed correctly
     */
    @Test
    fun linkWithSecondsWithoutPostfixS() {
        // Link: https://youtu.be/WYhKBjfqYaU?t=78
        val timestamp = parseYouTubeTimestamp("78")

        assertEquals(78, timestamp)
    }

    /**
     * Tests that a link with a timestamp given with a number and an "s" at the end is parsed correctly
     */
    @Test
    fun linkWithSecondsWithPostfixS() {
        // Link: https://youtu.be/WYhKBjfqYaU?t=78s
        val timestamp = parseYouTubeTimestamp("78s")

        assertEquals(78, timestamp)
    }

    /**
     * Tests that a link with a timestamp given with minutes and seconds is parsed correctly
     */
    @Test
    fun linkWithMinutesAndSeconds() {
        // Link: https://youtu.be/WYhKBjfqYaU?t=1m18s
        val timestamp = parseYouTubeTimestamp("1m18s")

        assertEquals(78, timestamp)
    }


    /**
     * Tests that a link with a timestamp given with minutes and seconds, where seconds is given first, is parsed correctly
     */
    @Test
    fun linkWithSecondsAndMinutes() {
        // Link: https://youtu.be/WYhKBjfqYaU?t=18s1m
        val timestamp = parseYouTubeTimestamp("18s1m")

        assertEquals(78, timestamp)
    }

    /**
     * Tests that a link with a timestamp given with only minutes is parsed correctly
     */
    @Test
    fun linkWithOnlyMinutes() {
        // Link: https://youtu.be/WYhKBjfqYaU?t=1m
        val timestamp = parseYouTubeTimestamp("1m")

        assertEquals(60, timestamp)
    }

    /**
     * Tests that a link with a timestamp given with an incorrect format for minutes and seconds
     * is parsed as 0 and does not throw an error
     */
    @Test
    fun linkWithIncorrectMinutesAndSeconds() {
        // Link: https://youtu.be/WYhKBjfqYaU?t=1m
        val timestamp = parseYouTubeTimestamp("1f18s")

        assertEquals(0, timestamp)
    }
}