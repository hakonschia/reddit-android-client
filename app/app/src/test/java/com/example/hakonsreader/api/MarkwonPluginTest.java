package com.example.hakonsreader.api;

import com.example.hakonsreader.markwonplugins.RedditLinkPlugin;
import com.example.hakonsreader.markwonplugins.SuperScriptPlugin;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Tests that the regex on the custom Markwon plugins work as expected
 */
public class MarkwonPluginTest {


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

        text = "/r/globaloffensive";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));
        text = "/R/globaloffensive";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        // This should not match as a text might randomly include r/something without the intention
        // being to link to a subreddit/user
        text = "ar/globaloffensive is not a subreddit, and apeu/erge is not a user. But u/hakonschia is!";
        m = pattern.matcher(text);
        assertEquals(1, totalMatches(m));

        text = "The reason I like r/GlobalOffensive is that I, u/hakonschia, enjoy watching CS:GO";
        m = pattern.matcher(text);
        assertEquals(2, totalMatches(m));
    }

    @Test
    public void superScriptPlugin() {
        Pattern pattern = SuperScriptPlugin.RE;

        String text = "hello ^(how ^(are) you) :)";
        Matcher m = pattern.matcher(text);

    }
}
