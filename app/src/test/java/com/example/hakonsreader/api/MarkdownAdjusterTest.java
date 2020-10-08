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

        markdown = "# Header with a space";
        expected = "# Header with a space";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "#Header without a space\nNormal comment on next line";
        expected = "# Header without a space\nNormal comment on next line";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "##Smaller header without a space";
        expected = "## Smaller header without a space";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "### Smaller header with a space";
        expected = "### Smaller header with a space";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "##Smaller header without a space\nComment on next line";
        expected = "## Smaller header without a space\nComment on next line";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "##Smaller header without a space\nComment on next line\n#And now a large header";
        expected = "## Smaller header without a space\nComment on next line\n# And now a large header";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "For some reason, I have chosen to not start this text with a header\n#Instead, the header is on the second line";
        expected = "For some reason, I have chosen to not start this text with a header\n# Instead, the header is on the second line";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        // Sometimes *cough* on GlobalOffensive *cough* there is a link with a #, I'm assuming this is
        // for linking to a part on the web page
        markdown = "[I'm putting a header tag inside a link](#not-actually-a-header-but-follows-the-same-syntax)";
        expected = "[I'm putting a header tag inside a link](#not-actually-a-header-but-follows-the-same-syntax)";
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
