package com.example.hakonsreader.api;

import com.example.hakonsreader.api.utils.MarkdownAdjuster;

import org.junit.Test;
import static org.junit.Assert.*;

public class MarkdownAdjusterTest {


    /**
     * Tests that headers are adjusted as expected
     *
     * Eg. "#Header" is adjusted to "# Header"
     */
    @Test
    public void testHeaderAdjustment() {
        MarkdownAdjuster adjuster = new MarkdownAdjuster.Builder()
                .checkHeaderSpaces()
                .build();

        String markdown = "#Header without a space";
        String expected = "# Header without a space";
        String actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "#Header without a space\nNormal comment on next line";
        expected = "# Header without a space\nNormal comment on next line";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "##Smaller header without a space";
        expected = "## Smaller header without a space";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "##Smaller header without a space\nComment on next line";
        expected = "## Smaller header without a space\nComment on next line";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);
    }

    @Test
    public void testRedditLinks() {
        MarkdownAdjuster adjuster = new MarkdownAdjuster.Builder()
                .checkRedditSpecificLinks()
                .build();

        // Check that "r/<subreddit>" gets wrapped with a markdown link
        String markdown = "You should check out r/GlobalOffensive, it's a really cool subreddit";
        String expected = "You should check out [r/GlobalOffensive](https://www.reddit.com/r/GlobalOffensive), it's a really cool subreddit";
        String actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        // Check that "/r/<subreddit>" gets wrapped with a markdown link
        markdown = "You should check out /r/GlobalOffensive, it's a really cool subreddit";
        expected = "You should check out [/r/GlobalOffensive](https://www.reddit.com/r/GlobalOffensive), it's a really cool subreddit";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);


        // Check that it also works for user links
        markdown = "u/hakonschia has 3 followers!";
        expected = "[u/hakonschia](https://www.reddit.com/u/hakonschia) has 3 followers!";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "/u/hakonschia has 8-5 followers!";
        expected = "[/u/hakonschia](https://www.reddit.com/u/hakonschia) has 8-5 followers!";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);
    }

}
