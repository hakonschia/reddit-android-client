package com.example.hakonsreader.markwonplugins;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;


/**
 * Testing class that tests {@link LinkPlugin}
 *
 * This plugin should look for links in markdown that aren't already in a markdown link
 */
public class LinkPluginTest {

    private int totalMatches(Matcher m) {
        int matches = 0;
        while(m.find()) {
            matches++;
        }
        return matches;
    }

    /**
     * Test that normal links works as expected
     */
    @Test
    public void testLinks() {
        Pattern pattern = LinkPlugin.RE;
        Matcher m;

        String link = "https://www.reddit.com/r/hakonschia/comments/jtyz0e/cool_image/";
        m = pattern.matcher(link);
        assertEquals(1, totalMatches(m));

        link = "https://www.reddit.com/message/compose/?to=RepostSleuthBot&amp;subject=False%20Positive&amp;message={\\\"post_id\\\": \\\"k3tlrg\\\", \\\"meme_template\\\": null})";
        m = pattern.matcher(link);
        assertEquals(1, totalMatches(m));

        link = "Check out this link: https://www.reddit.com/message/compose/?to=RepostSleuthBot&amp;subject=False%20Positive&amp;message={\\\"post_id\\\": \\\"k3tlrg\\\", \\\"meme_template\\\": null})";
        m = pattern.matcher(link);
        assertEquals(1, totalMatches(m));
    }

    /**
     * Tests
     */
    @Test
    public void testLinksAlreadyInMarkdown() {
        Pattern pattern = LinkPlugin.RE;
        Matcher m;

        String link = "[reddit](https://www.reddit.com/r/hakonschia/comments/jtyz0e/cool_image/)";
        m = pattern.matcher(link);
        assertEquals(0, totalMatches(m));
    }
}
