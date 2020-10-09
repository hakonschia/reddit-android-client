package com.example.hakonsreader.api;

import com.example.hakonsreader.api.utils.MarkdownAdjuster;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class MarkdownAdjusterTest {


    @Test
    public void k() {
        String markdown = "Hello r/GlobalOffensive, how are you doing /r/hydro_homies.good thanks on this fine day?";

        Pattern p = Pattern.compile("(^|\\s)/?(r|R)/[A-Za-z_]+");
        Matcher m = p.matcher(markdown);

        // Map holding the "r/subreddit" mapped to its linked string "[r/subreddit](https://...)"
        Map<String, String> subredditMap = new HashMap<>();

        while (m.find()) {
            // Although we do care about the whitespace in front, we will later just replace
            // the subreddit text itself, so the whitespace won't matter
            String sub = m.group().trim();

            String withoutSlash = sub;

            // Remove the first slash if present
            System.out.println(sub);
            if (sub.charAt(0) == '/') {
                withoutSlash = sub.substring(1);
            }

            // The hypertext should still have the slash present, but for the link we need it without
            String linked = String.format("[%s](https://www.reddit.com/%s/)", sub, withoutSlash);

            System.out.println(linked);
            subredditMap.put(sub, linked);
        }

        for (Map.Entry<String, String> entry : subredditMap.entrySet()) {
            String sub = entry.getKey();
            String linked = entry.getValue();

            // replaceAll is probably bad to use since we're actually only replacing the exact match each time
            markdown = markdown.replaceAll(sub, linked);
        }

        System.out.println(markdown);
    }

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

    /**
     * Tests that if a line has what technically would look like multiple headers, space is only added
     * for the first header as you can't have nested headers
     */
    @Test
    public void testMultipleHeadersInOneLine() {
        MarkdownAdjuster adjuster = new MarkdownAdjuster.Builder()
                .checkHeaderSpaces()
                .build();

        String markdown = "#Header without a space, and without a new line ##i am trying to add another header";
        String expected = "# Header without a space, and without a new line ##i am trying to add another header";
        String actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);
    }


    @Test
    public void testRedditLinks() {
        MarkdownAdjuster adjuster = new MarkdownAdjuster.Builder()
                .checkRedditSpecificLinks()
                .build();

        // Check that "r/<subreddit>" gets wrapped with a markdown link
        String markdown = "You should check out r/GlobalOffensive, it's a really cool subreddit";
        String expected = "You should check out [r/GlobalOffensive](https://www.reddit.com/r/GlobalOffensive/), it's a really cool subreddit";
        String actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        // Check that "/r/<subreddit>" gets wrapped with a markdown link
        markdown = "You should check out /r/GlobalOffensive, it's a really cool subreddit";
        expected = "You should check out [/r/GlobalOffensive](https://www.reddit.com/r/GlobalOffensive/), it's a really cool subreddit";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        // Check that already linked subs don't get wrapped again
        markdown = "This linked [r/subreddit](https://www.reddit.com/r/subreddit/) already has a link around it";
        expected = "This linked [r/subreddit](https://www.reddit.com/r/subreddit/) already has a link around it";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);


        // Check that it also works for user links
        markdown = "u/hakonschia has 3 followers!";
        expected = "[u/hakonschia](https://www.reddit.com/u/hakonschia/) has 3 followers!";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "/u/hakonschia has 8-5 followers!";
        expected = "[/u/hakonschia](https://www.reddit.com/u/hakonschia/) has 8-5 followers!";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);
    }


    /**
     * Tests that normal links (ie. https://...) get wrapped with markdown correctly
     */
    @Test
    public void testNormalLinks() {
        MarkdownAdjuster adjuster = new MarkdownAdjuster.Builder()
                .checkNormalLinks()
                .build();

        // Check that "https://..." gets wrapped with a markdown link
        String markdown = "You should check out https://nrk.no, it's a pretty good news site";
        String expected = "You should check out [https://nrk.no](https://nrk.no), it's a pretty good news site";
        String actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);
    }

    /**
     * Tests that links already wrapped with markdown don't get modified
     */
    @Test
    public void testAlreadyWrappedLinks() {

    }


    @Test
    public void testHeadersAndLinks() {
        MarkdownAdjuster adjuster = new MarkdownAdjuster.Builder()
                .checkRedditSpecificLinks()
                .checkHeaderSpaces()
                .build();

        String markdown = "#Here are 5 reason why you should check out GlobalOffensive, number 9 will shock you!\nYou should check out r/GlobalOffensive, it's a really cool subreddit";
        String expected = "# Here are 5 reason why you should check out GlobalOffensive, number 9 will shock you!\nYou should check out [r/GlobalOffensive](https://www.reddit.com/r/GlobalOffensive/), it's a really cool subreddit";
        String actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "#This is a header with a link to a r/subreddit";
        expected = "# This is a header with a link to a [r/subreddit](https://www.reddit.com/r/subreddit)";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);
    }
}
