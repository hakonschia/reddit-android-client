package com.example.hakonsreader.api

import org.junit.Test

class RedditApiTest {

    /**
     * Tests that the constructor throws an exception when the userAgent parameter is empty
     */
    @Test(expected = IllegalStateException::class)
    fun noUserAgent() {
        RedditApiImpl(userAgent = "", clientId = "hahuierjioogj")
    }

    /**
     * Tests that the constructor throws an exception when the clientId parameter is empty
     */
    @Test(expected = IllegalStateException::class)
    fun noClientId() {
        RedditApiImpl(userAgent = "v0.0.0 by u/hakonschia", clientId = "")
    }

    /**
     * Tests that the constructor throws an exception when the userAgent or clientId parameter is empty
     */
    @Test(expected = IllegalStateException::class)
    fun noUserAgentOrClientId() {
        RedditApiImpl(userAgent = "", clientId = "")
    }

    /**
     * Tests that calls to [RedditApiImpl.accessToken] throws an exception if the callback URL isn't given
     * when creating the object
     */
    @Test(expected = IllegalStateException::class)
    fun getAccessTokenNoCallbackUrl() {
        // Test with empty callback URL
        RedditApiImpl(userAgent = "a", clientId = "b").accessToken()
    }

    /**
     * Tests that calls to [RedditApiImpl.accessToken] throws an exception if the callback URL is given
     * but is empty when creating the object
     */
    @Test(expected = IllegalStateException::class)
    fun getAccessTokenEmptyCallbackUrl() {
        // Test with empty callback URL
        RedditApiImpl(userAgent = "a", clientId = "b", callbackUrl = "").accessToken()
    }
}