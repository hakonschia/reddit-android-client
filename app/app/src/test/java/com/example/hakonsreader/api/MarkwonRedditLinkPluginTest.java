package com.example.hakonsreader.api;

import com.example.hakonsreader.markwonplugins.RedditLinkPlugin;
import com.example.hakonsreader.markwonplugins.SuperscriptPlugin;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Tests that the regex on the custom Markwon plugin for reddit specific links works as expected
 */
public class MarkwonRedditLinkPluginTest {


    private int totalMatches(Matcher m) {
        int matches = 0;
        while(m.find()) {
            matches++;
        }
        return matches;
    }

    @Test
    public void linkPlugin() {
        Pattern pattern = RedditLinkPlugin.RE;

        String text = "r/globaloffensive";
        Matcher m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));
        text = "R/globaloffensive";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        text = "/r/GlobalOffensive";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        text = "/r/GlobalOffensive/";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        text = "/R/globaloffensive";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        text = "https://www.reddit.com/r/GlobalOffensive/";
        m = pattern.matcher(text);
        assertEquals(0, totalMatches(m));

        text = "https://www.reddit.com/u/hakonschia/";
        m = pattern.matcher(text);
        assertEquals(0, totalMatches(m));

        text = "https://www.reddit.com/user/hakonschia/";
        m = pattern.matcher(text);
        assertEquals(0, totalMatches(m));

        text = "r/R/globaloffensive";
        m = pattern.matcher(text);
        assertEquals(0, totalMatches(m));

        text = "/u/hakonschia";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        text = "u/hello_there";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        text = "u/hello_there/";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        text = "user/hello_there/";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        text = "Hello there, /user/general_kenobi";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        text = "/USER/YOUMUSTBEveryproud";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        text = "200 000 units are ready /user/general_kenobi, with a million more on the way u/magnificent-arent_they. See r/prequelmemes for more";
        m = pattern.matcher(text);
        assertEquals(3, totalMatches(m));

        text = "This is not a subreddit/r/globaloffensive";
        m = pattern.matcher(text);
        assertEquals(0, totalMatches(m));

        text = "This is not a hrt/u/globaloffensive";
        m = pattern.matcher(text);
        assertEquals(0, totalMatches(m));

        text = "This is not a user/user/globaloffensive";
        m = pattern.matcher(text);
        assertEquals(0, totalMatches(m));

        // This should not match as a text might randomly include r/something without the intention
        // being to link to a subreddit/user
        text = "ar/globaloffensive is not a subreddit, and apeu/erge is not a user. But u/hakonschia is!";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        text = "The reason I like r/GlobalOffensive is that I, u/hakonschia, enjoy watching CS:GO";
        m = pattern.matcher(text);
        assertEquals(2, totalMatches(m));
    }
}
