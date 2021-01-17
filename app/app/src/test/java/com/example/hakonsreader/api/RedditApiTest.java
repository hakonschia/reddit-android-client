package com.example.hakonsreader.api;

import org.junit.Test;
import org.mockito.Mockito;

public class RedditApiTest {

    // TODO fix test cases for kotlin

    /**
     * Tests that the builder constructor throws an exception if no user agent is given.
     * The user agent must be present to respect Reddit's API rules of setting a custom
     * User-Agent header
     */
    @Test(expected = IllegalStateException.class)
    public void builderNoUserAgent() {
      //  new RedditApi.Builder("", "gerg");
    }

    /**
     * Tests that the builder constructor throws an exception if no client ID is given
     */
    @Test(expected = IllegalStateException.class)
    public void builderNoClientId() {
   //     new RedditApi.Builder("fffff", "");
    }

    /**
     * Tests that the builder constructor throws an exception if no user agent or client ID is given
     */
    @Test(expected = IllegalStateException.class)
    public void builderNoUserAgentOrClientId() {
     //   new RedditApi.Builder("", "");
    }

    /**
     * Tests that trying to get an access token without a callback URL fails.
     * This has to fail as the callback URL is mandatory to make this call
     */
    @Test(expected = IllegalStateException.class)
    public void getAccessTokenNoCallbackUrl() {
        // TODO fix this for the Kotlin version
   //     new RedditApi.Builder("a", "b").build().accessToken().get("", null, null);
    }

}
