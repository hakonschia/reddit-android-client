package com.example.hakonsreader.api;

import android.view.textclassifier.TextClassification;

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

        // A single newline in markdown doesn't do anything, so for the next line two have to be used
        markdown = "#Header without a space\n\nNormal comment on next line";
        expected = "# Header without a space\n\nNormal comment on next line";
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

        markdown = "##Smaller header without a space\n\nComment on next line";
        expected = "## Smaller header without a space\n\nComment on next line";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "##Smaller header without a space\n\nComment on next line\n\n#And now a large header";
        expected = "## Smaller header without a space\n\nComment on next line\n\n# And now a large header";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "For some reason, I have chosen to not start this text with a header\n\n#Instead, the header is on the second line";
        expected = "For some reason, I have chosen to not start this text with a header\n\n# Instead, the header is on the second line";
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

        markdown = "Link [https://nrk.no](https://nrk.no) already wrapped";
        expected = "Link [https://nrk.no](https://nrk.no) already wrapped";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "Link https://www.nrk.no not wrapped";
        expected = "Link [https://www.nrk.no](https://www.nrk.no) not wrapped";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "Check out old.reddit.com: https://old.reddit.com/r/GlobalOffensive";
        expected = "Check out old.reddit.com: [https://old.reddit.com/r/GlobalOffensive](https://old.reddit.com/r/GlobalOffensive)";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "Check out old.reddit.com: https://old.reddit.com/r/GlobalOffensive?sort=top&not_really_a_parameter=false with parameters";
        expected = "Check out old.reddit.com: [https://old.reddit.com/r/GlobalOffensive?sort=top&not_really_a_parameter=false](https://old.reddit.com/r/GlobalOffensive?sort=top&not_really_a_parameter=false) with parameters";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);
    }

    @Test
    public void testHeadersAndLinks() {
        MarkdownAdjuster adjuster = new MarkdownAdjuster.Builder()
                .checkRedditSpecificLinks()
                .checkHeaderSpaces()
                .build();

        String markdown = "#Here are 5 reason why you should check out GlobalOffensive, number 9 will shock you!\n\nYou should check out r/GlobalOffensive, it's a really cool subreddit";
        String expected = "# Here are 5 reason why you should check out GlobalOffensive, number 9 will shock you!\n\nYou should check out [r/GlobalOffensive](https://www.reddit.com/r/GlobalOffensive/), it's a really cool subreddit";
        String actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "#This is a header with a link to a r/subreddit";
        expected = "# This is a header with a link to a [r/subreddit](https://www.reddit.com/r/subreddit/)";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);
    }

    /**
     * Tests that URLs in markdown links are encoded correctly
     *
     * Some users/bots use spaces, curly brackets, double quotes etc. in URLs which makes Markwon
     * not recognize them as links
     */
    @Test
    public void testUrlEncodeChange() {
        MarkdownAdjuster adjuster = new MarkdownAdjuster.Builder()
                .checkUrlEncoding()
                .build();
        String markdown;
        String expected;
        String actual;

        // Spaces should be replaced with %20
        markdown = "[Link with spaces](https://www.reddit.com/hello there general kenobi)";
        expected = "[Link with spaces](https://www.reddit.com/hello%20there%20general%20kenobi)";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        // Opening curly brackets should be replaced with "%7B" and closing with "%7D"
        // Double quotes should be replaced with %22
        markdown = "[False Positive](https://www.reddit.com/message/compose/?to=RepostSleuthBot&amp;subject=False%20Positive&amp;message={\"post_id\": \"k3tlrg\", \"meme_template\": null})";
        expected = "[False Positive](https://www.reddit.com/message/compose/?to=RepostSleuthBot&amp;subject=False%20Positive&amp;message=%7B%22post_id%22:%20%22k3tlrg%22,%20%22meme_template%22:%20null%7D)";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        // No errors, should be the same
        markdown = "[Nothing wrong with this link](https://meta.stackexchange.com/questions/79057/curly-brackets-in-urls)";
        expected = "[Nothing wrong with this link](https://meta.stackexchange.com/questions/79057/curly-brackets-in-urls)";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        // In a sentence
        markdown = "This markdown link contains errors [False Positive](https://www.reddit.com/message/compose/?to=RepostSleuthBot&amp;subject=False%20Positive&amp;message={\"post_id\": \"k3tlrg\", \"meme_template\": null}) since it is using curly brackets";
        expected = "This markdown link contains errors [False Positive](https://www.reddit.com/message/compose/?to=RepostSleuthBot&amp;subject=False%20Positive&amp;message=%7B%22post_id%22:%20%22k3tlrg%22,%20%22meme_template%22:%20null%7D) since it is using curly brackets";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        // Multiple errors in one sentence
        markdown = "This is an error [error](https://reddit.com/horsing around), and another one [error 2](https://nrk.no/cool article)";
        expected = "This is an error [error](https://reddit.com/horsing%20around), and another one [error 2](https://nrk.no/cool%20article)";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        // Spaces should be replaced with %20
        markdown = "Hello there general\n\n Kenobi [Link with spaces](https://www.reddit.com/hello there general kenobi)";
        expected = "Hello there general\n\n Kenobi [Link with spaces](https://www.reddit.com/hello%20there%20general%20kenobi)";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        // Brackets that aren't for a link and a link later
        markdown = "COMMUNITY: Yeah LETS DO IT [gather 100k usd]\n" +
                "\n" +
                "COMMUNITY[after first loss]: THIS HAS BEEN THE WORST TRADE DEAL IN THE HISTORY OF THE WORLD!\n" +
                "\n" +
                "LAUNDER: [OMG I AGREE](https://tenor.com/view/zombie-land-comedy-crying-upset-sad-gif-3359111)";
        expected = "COMMUNITY: Yeah LETS DO IT [gather 100k usd]\n" +
                "\n" +
                "COMMUNITY[after first loss]: THIS HAS BEEN THE WORST TRADE DEAL IN THE HISTORY OF THE WORLD!\n" +
                "\n" +
                "LAUNDER: [OMG I AGREE](https://tenor.com/view/zombie-land-comedy-crying-upset-sad-gif-3359111)";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);
    }

    @Test
    public void testImageLinkConverter() {
        MarkdownAdjuster adjuster = new MarkdownAdjuster.Builder()
                .convertImageLinksToMarkdown()
                .build();

        String markdown;
        String expected;
        String actual;

        // Image file extension (.png)
        markdown = "Next comes an image: https://i.redd.it/z4sgyaoenlf61.png what a nice image";
        expected = "Next comes an image: ![image](https://i.redd.it/z4sgyaoenlf61.png) what a nice image";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "https://i.redd.it/z4sgyaoenlf61.png";
        expected = "![image](https://i.redd.it/z4sgyaoenlf61.png)";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        // Image file in query parameter (?format=jpg)
        markdown = "https://pbs.twimg.com/media/Es_qtWVXEAMKEBd?format=jpg&name=large";
        expected = "![image](https://pbs.twimg.com/media/Es_qtWVXEAMKEBd?format=jpg&name=large)";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "Next comes an image: https://pbs.twimg.com/media/Es_qtWVXEAMKEBd?format=jpg&name=large what a nice image";
        expected = "Next comes an image: ![image](https://pbs.twimg.com/media/Es_qtWVXEAMKEBd?format=jpg&name=large) what a nice image";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        markdown = "This image is already ![image description](https://i.redd.it/z4sgyaoenlf61.png) in image markdown";
        expected = "This image is already ![image description](https://i.redd.it/z4sgyaoenlf61.png) in image markdown";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);

        // This could potentially be made to match as well, the downside is if an image is linked like this
        // it might be odd to have it break that text with an image (could potentially be a setting of its own
        // to show images directly if they're linked with text)
        markdown = "This image is already [in a link](https://i.redd.it/z4sgyaoenlf61.png) in image markdown";
        expected = "This image is already [in a link](https://i.redd.it/z4sgyaoenlf61.png) in image markdown";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);


        // If the link is in a markdown link, but the text and link is just the link, then we can convert it
        markdown = "This is a raw image link in markdown [https://i.redd.it/z4sgyaoenlf61.png](https://i.redd.it/z4sgyaoenlf61.png)";
        expected = "This is a raw image link in markdown ![image](https://i.redd.it/z4sgyaoenlf61.png)";
        actual = adjuster.adjust(markdown);
        assertEquals(expected, actual);
    }
}
