package com.example.hakonsreader.api;

import com.example.hakonsreader.api.utils.LinkUtils;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Class testing {@link com.example.hakonsreader.api.utils.LinkUtils}
 */
public class LinkUtilsTest {


    /**
     * Tests {@link LinkUtils#BASE_SUBREDDIT_REGEX}
     *
     * This is a fairly incomprehensive test as this regex is meant to be used by other regexs
     * as a base, and will therefore not check for start/end characters, so it could match more than
     * what is expected
     */
    @Test
    public void testBaseSubredditRegex() {
        // Test cases that should match
        String[] matchingTests = {
                // Standard
                "r/GlobalOffensive/",
                // Capitalized R
                "R/GlobalOffensive",
                // Allowed characters: A-Za-z, 0-9, "_"
                "r/aAbB03_439ger",
        };

        Pattern p = Pattern.compile(LinkUtils.BASE_SUBREDDIT_REGEX);
        for (String matchingTest : matchingTests) {
            Matcher matcher = p.matcher(matchingTest);
            if (!matcher.find()) {
                fail(String.format("'%s' doesn't match", matchingTest));
            }
        }
    }

    /**
     * Tests that {@link LinkUtils#SUBREDDIT_REGEX_WITH_HTTPS} matches what it should and does not
     * match what it should not match
     */
    @Test
    public void testSubredditRegexWithHttps() {
        // Test cases that should match
        String[] matchingTests = {
                // Standard
                "https://www.reddit.com/r/GlobalOffensive/",
                // No trailing slash
                "https://www.reddit.com/r/GlobalOffensive",
                // Capitalized R
                "https://www.reddit.com/R/GlobalOffensive/",
                // No "www." should also match
                "https://reddit.com/r/GlobalOffensive",
                // Allowed characters: A-Za-z, 0-9, "_"
                "https://www.reddit.com/r/aAbB03_439ger",

                // Should match other subdomains
                "https://old.reddit.com/r/aAbB03_439ger",

                // Should match no subdomain
                "https://reddit.com/r/aAbB03_439ger",
        };

        Pattern p = Pattern.compile(LinkUtils.SUBREDDIT_REGEX_WITH_HTTPS);
        for (String matchingTest : matchingTests) {
            Matcher matcher = p.matcher(matchingTest);
            if (!matcher.find()) {
                fail(String.format("'%s' doesn't match", matchingTest));
            }
        }

        // Test cases that should NOT match
        String[] notMatchingTests = {
                // Duplicate r
                "https://www.reddit.com/rr/GlobalOffensive/",
                // URLs matching more than the subreddit (links to posts) should not match
                "https://www.reddit.com/r/norge/comments/ju1dvc/brelett_gang_rise_up/",
        };

        for (String notMatchingTest : notMatchingTests) {
            Matcher matcher = p.matcher(notMatchingTest);
            if (matcher.find()) {
                fail(String.format("'%s' matches", notMatchingTest));
            }
        }
    }

    /**
     * Tests {@link LinkUtils#SUBREDDIT_REGEX_NO_HTTPS} matches what it should and does not
     * match what it should not match
     */
    @Test
    public void testSubredditRegexNoHttps() {
        // Test cases that should match
        String[] matchingTests = {
                // Standard
                "/r/GlobalOffensive/",
                // No preceding slash
                "/r/GlobalOffensive/",
                // No trailing slash
                "/r/GlobalOffensive",
                // No preceding or trailing slash
                "r/GlobalOffensive",
                // Allowed characters: A-Za-z, 0-9, "_"
                "r/agbERGB4d_97",
        };

        Pattern p = Pattern.compile(LinkUtils.SUBREDDIT_REGEX_NO_HTTPS);
        for (String matchingTest : matchingTests) {
            Matcher matcher = p.matcher(matchingTest);
            if (!matcher.find()) {
                fail(String.format("'%s' doesn't match", matchingTest));
            }
        }

        // Test cases that should NOT match
        String[] notMatchingTests = {
                // This should NOT match https schemes
                "https://reddit.com/r/globaloffensive",
                // "-" is not allowed in subreddit names
                "r/global-offensive",
                // More than just "r/<subreddit/" should not match
                "r/norge/comments/ju1dvc/brelett_gang_rise_up/",

                "/r/",
                "r/",
                "/r",
                "r"
        };

        for (String notMatchingTest : notMatchingTests) {
            Matcher matcher = p.matcher(notMatchingTest);
            if (matcher.find()) {
                fail(String.format("'%s' matches", notMatchingTest));
            }
        }
    }

    /**
     * Tests {@link LinkUtils#SUBREDDIT_REGEX_COMBINED}. This should match
     * strings that are either {@link LinkUtils#SUBREDDIT_REGEX_WITH_HTTPS} or {@link LinkUtils#SUBREDDIT_REGEX_NO_HTTPS}
     */
    @Test
    public void testSubredditRegexCombined() {
        // Test cases that should match
        // These test cases can perfecetly fine by copied from the previous test, as this test
        // is testing that the regex is combined correctly
        String[] matchingTests = {
                // With https

                // Standard
                "https://www.reddit.com/r/GlobalOffensive/",
                // No trailing slash
                "https://www.reddit.com/r/GlobalOffensive",
                // Capitalized R
                "https://www.reddit.com/R/GlobalOffensive/",
                // No "www." should also match
                "https://reddit.com/r/GlobalOffensive",
                // Allowed characters: A-Za-z, 0-9, "_"
                "https://www.reddit.com/r/aAbB03_439ger",


                // Without https

                // Standard
                "/r/GlobalOffensive/",
                // No preceding slash
                "/r/GlobalOffensive/",
                // No trailing slash
                "/r/GlobalOffensive",
                // No preceding or trailing slash
                "r/GlobalOffensive",
                // Allowed characters: A-Za-z, 0-9, "_"
                "r/agbERGB4d_97",
        };

        Pattern p = Pattern.compile(LinkUtils.SUBREDDIT_REGEX_COMBINED);
        for (String matchingTest : matchingTests) {
            Matcher matcher = p.matcher(matchingTest);
            if (!matcher.find()) {
                fail(String.format("'%s' doesn't match", matchingTest));
            }
        }

        // Test cases that should NOT match
        // These cant be straight up copied as some test cases in one should not match in the other
        String[] notMatchingTests = {
                // With https

                // Duplicate r
                "https://www.reddit.com/rr/GlobalOffensive/",
                // URLs matching more than the subreddit (links to posts) should not match
                "https://www.reddit.com/r/norge/comments/ju1dvc/brelett_gang_rise_up/",


                // Without https

                // "-" is not allowed in subreddit names
                "r/global-offensive",
                // More than just "r/<subreddit/" should not match
                "r/norge/comments/ju1dvc/brelett_gang_rise_up/"
        };

        for (String notMatchingTest : notMatchingTests) {
            Matcher matcher = p.matcher(notMatchingTest);
            if (matcher.find()) {
                fail(String.format("'%s' matches", notMatchingTest));
            }
        }
    }


    /**
     * Tests {@link LinkUtils#BASE_USER_REGEX}. This should match links like "u/hakonschia"
     * that link to reddit user profiles
     */
    @Test
    public void testBaseUserRegex() {
        // Test cases that should match
        String[] matchingTests = {
                "/u/hakonschia/",
                "/u/hakonschia",
                "u/hakonschia",
                // Characters allowed: a-zA-Z, 0-9, "-", "_"
                "u/hakon-schia9",
                "/u/hanko_schia-s5",
        };

        Pattern p = Pattern.compile(LinkUtils.BASE_USER_REGEX);
        for (String matchingTest : matchingTests) {
            Matcher matcher = p.matcher(matchingTest);
            if (!matcher.find()) {
                fail(String.format("'%s' doesn't match", matchingTest));
            }
        }

        // Test cases that should NOT match
        String[] notMatchingTests = {
                // Should not match if nothing is given after the user
                "/u/",
                "u/",
                "/user/",
                "user/",

                // Characters not allowed (this is by no means including literally everything)
                "/u/!\"#¤%&/()=?`,.*"
        };

        for (String notMatchingTest : notMatchingTests) {
            Matcher matcher = p.matcher(notMatchingTest);
            if (matcher.find()) {
                fail(String.format("'%s' matches", notMatchingTest));
            }
        }
    }

    /**
     * Test {@link LinkUtils#POST_REGEX}. This should match strings like
     * https://reddit.com/r/comments/gjioer/FaZe_actually_wins_major/" that link to posts
     */
    @Test
    public void testPostRegex() {
        // Test cases that should match
        String[] matchingTests = {
                "https://reddit.com/r/hakonschia/comments/jtyz0e/cool_image/",
                "https://reddit.com/r/hakonschia/comments/jtyz0e/cool_image/",
                "https://reddit.com/r/hakonschia/comments/jtyz0e/cool_image",
                "https://reddit.com/r/hakonschia/comments/jtyz0e/cool_image",
                "https://reddit.com/R/hakonschia/comments/jtyz0e/cool_image",

                // The text at the end (which is part of the title of the post) isn't a requirement
                "https://reddit.com/r/hakonschia/comments/jtyz0e/",
                "https://reddit.com/r/hakonschia/comments/jtyz0e",

                "https://reddit.com/R/hakon_schia/comments/jtyz0e/cool_image",
                "https://reddit.com/R/hakon_schia2/comments/jtyz0e/cool_image",

                "https://reddit.com/r/hakon_schia/comments/jtyz0e/cool_image",
                "https://reddit.com/r/hakon_schia/comments/jtyz0e/",
                "https://reddit.com/r/hakon_schia/comments/jtyz0e",

                // The full urls should also match a comment chain (the ID at the end links to a comment ID)
                "https://reddit.com/r/hakon_schia/comments/jtyz0e/cool_image/gerger",
                "https://reddit.com/r/hakon_schia/comments/jtyz0e/cool_image/gerger/",
        };

        Pattern p = Pattern.compile(LinkUtils.POST_REGEX);
        for (String matchingTest : matchingTests) {
            Matcher matcher = p.matcher(matchingTest);
            if (!matcher.find()) {
                fail(String.format("'%s' doesn't match", matchingTest));
            }
        }

        // Test cases that should NOT match
        String[] notMatchingTests = {
                // No post ID given
                "r/hakonschia/comments/",
                "r/hakonschia/comments",
                "https://reddit.com/r/hakon_schia/comments/",
                "https://reddit.com/r/hakon_schia/comments",

                // TODO fix so it must be on reddit.com
                "https://www.removeddit.com/r/DivinityOriginalSin/comments/jtx6t9/my_so_wanted_me_to_play_this_with_him_he_knew/gc8jjne"
        };

        for (String notMatchingTest : notMatchingTests) {
            Matcher matcher = p.matcher(notMatchingTest);
            if (matcher.find()) {
                fail(String.format("'%s' matches", notMatchingTest));
            }
        }
    }

    /**
     * Tests {@link LinkUtils#POST_REGEX_NO_SUBREDDIT}. This should match strings that link to posts
     * but give no subreddit info, such as "comments/gfeir"
     */
    @Test
    public void testPostRegexNoSubreddit() {
        // Test cases that should match
        String[] matchingTests = {
                // Since these are for no subreddits, they cant match "r/../comments/<postid>" etc.
                "https://reddit.com/comments/jtyz0e/cool_image/",
                "https://reddit.com/comments/jtyz0e/cool_image",
                "https://reddit.com/comments/jtyz0e/",
                "https://reddit.com/comments/jtyz0e",
        };

        Pattern p = Pattern.compile(LinkUtils.POST_REGEX_NO_SUBREDDIT);
        for (String matchingTest : matchingTests) {
            Matcher matcher = p.matcher(matchingTest);
            if (!matcher.find()) {
                fail(String.format("'%s' doesn't match", matchingTest));
            }
        }

        // Test cases that should NOT match
        String[] notMatchingTests = {
                // No post ID given
                "https://reddit.com/comments/",
                "https://reddit.com//comments",
        };

        for (String notMatchingTest : notMatchingTests) {
            Matcher matcher = p.matcher(notMatchingTest);
            if (matcher.find()) {
                fail(String.format("'%s' matches", notMatchingTest));
            }
        }
    }

    /**
     * Tests {@link LinkUtils#POST_SHORTENED_URL_REGEX}. This should match urls such as "https://redd.it/ghreg"
     */
    @Test
    public void testShortenedPostUrl() {
        // Test cases that should match
        String[] matchingTests = {
                "https://redd.it/poerhg/",
                "https://redd.it/poerhg",
                "https://www.redd.it/poerhg"
        };

        Pattern p = Pattern.compile(LinkUtils.POST_SHORTENED_URL_REGEX);
        for (String matchingTest : matchingTests) {
            Matcher matcher = p.matcher(matchingTest);
            if (!matcher.find()) {
                fail(String.format("'%s' doesn't match", matchingTest));
            }
        }

        // Test cases that should NOT match
        String[] notMatchingTests = {
                // No post ID given
                "https://redd.it/",
                "https://redd.it"
        };

        for (String notMatchingTest : notMatchingTests) {
            Matcher matcher = p.matcher(notMatchingTest);
            if (matcher.find()) {
                fail(String.format("'%s' matches", notMatchingTest));
            }
        }
    }
}
