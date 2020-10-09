package com.example.hakonsreader.api.utils;


import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class for adjusting some generic markdown faults
 *
 * <p>Use {@link MarkdownAdjuster.Builder} to create new objects</p>
 */
public class MarkdownAdjuster {
    private boolean checkRedditSpecificLinks;
    private boolean checkHeaderSpaces;
    private boolean checkNormalLinks;
    private boolean checkSuperScript;


    private MarkdownAdjuster() {}

    public static class Builder {
        boolean bCheckRedditSpecificLinks = false;
        boolean bCheckHeaderSpaces = false;
        boolean bCheckNormalLinks = false;
        boolean bCheckSuperScript = false;


        /**
         * Sets if the text should look for headers that don't have a space in between the text and
         * header symbol
         *
         * <p>Eg "#Header" will be converted into "# Header"</p>
         *
         * @return This builder
         */
        public Builder checkHeaderSpaces() {
            bCheckHeaderSpaces = true;
            return this;
        }

        /**
         * Sets if the adjuster should look for strings such as "r/GlobalOffensive" and wraps them in
         * markdown links
         *
         * @return This builder
         */
        public Builder checkRedditSpecificLinks() {
            bCheckRedditSpecificLinks = true;
            return this;
        }

        /**
         * Sets if the adjuster should look for normal links (ie. https://...) and wraps them
         * in markdown links
         *
         * <p>This only checks for https links, not http</p>
         *
         * @return This builder
         */
        public Builder checkNormalLinks() {
            // TODO add flag to only keep domain (ie https://nrk.no -> nrk.no)
            bCheckNormalLinks = true;
            return this;
        }

        /**
         * Sets if the adjuster should look for superscript symbols (^) and replace them with
         * {@code <sup>} HTML tags. If you are using this ensure that your markdown renderer supports
         * HTML tags
         *
         * @return This builder
         */
        public Builder checkSuperScript() {
            bCheckSuperScript = true;
            return this;
        }

        /**
         * Builds the MarkdownAdjuster
         *
         * @return The MarkdownAdjuster
         */
        public MarkdownAdjuster build() {
            MarkdownAdjuster adjuster = new MarkdownAdjuster();
            adjuster.checkHeaderSpaces = bCheckHeaderSpaces;
            adjuster.checkRedditSpecificLinks = bCheckRedditSpecificLinks;
            adjuster.checkNormalLinks = bCheckNormalLinks;
            adjuster.checkSuperScript = bCheckSuperScript;

            return adjuster;
        }
    }


    /**
     * Adjusts a markdown text
     *
     * @param markdown The text to adjust
     * @return The adjusted markdown text
     */
    public String adjust(String markdown) {
        if (checkHeaderSpaces) {
            markdown = this.adjustHeaderSpaces(markdown);
        }
        if (checkRedditSpecificLinks) {
            markdown = this.adjustRedditSpecificLinks(markdown);
        }
        if (checkNormalLinks) {
            markdown = this.adjustNormalLinks(markdown);
        }
        if (checkSuperScript) {
            markdown = this.adjustSuperScript(markdown);
        }

        return markdown;
    }

    /**
     * Adjusts header spacing.
     *
     * <p>Headers in markdown are recognized by a hashtag (#), and there can be multiple hashtags
     * in a row to produce smaller headers. Proper markdown has to have a space between the header
     * and the content of the header, and this function ensures that is added</p>
     * <p>Markdown links, [](), can include a hashtag as the link to link to a part on the same web page.
     * Spaces are not added to links as that would break the link</p>
     *
     * @param markdown The markdown to adjust
     * @return A new string with the headers spaced out
     */
    private String adjustHeaderSpaces(String markdown) {
        // Find everywhere where there are # without a space next, and insert a space (technically max 6 # but it's
        // not super important to be strictly compliant here)

        // Markdown works on lines, so for headers we only need to care about the first section of hashtags as a header
        // So if another hashtag that would be seen as a header appears, nothing will be done (as it should be)
        String[] lines = markdown.split("\n");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.isEmpty()) {
                line = this.addSpacesToHeader(line);

                // The line object has now either been edited, or is as it should be, so append it on the builder
                builder.append(line);

                // Add back the newline unless we are at the end
                if (i + 1 != lines.length) {
                    builder.append("\n");
                }
            }
        }

        return builder.toString();
    }

    /**
     * Adjusts reddit specific links (to subreddits, r/..., and users, u/...) and wraps them
     * in markdown links
     *
     * @param markdown The markdown to adjust
     * @return The adjusted markdown
     */
    private String adjustRedditSpecificLinks(String markdown) {
        markdown = this.replaceSubredditLinks(markdown);
        markdown = this.replaceRedditUserLinks(markdown);

        return markdown;
    }

    /**
     * Wraps normal links that aren't already wrapped with markdown links
     *
     * @param markdown The markdown to adjust
     * @return The adjusted markdown
     */
    private String adjustNormalLinks(String markdown) {
        // This regex is taken (mostly) from: https://stackoverflow.com/a/3809435/7750841
        String pattern = "(^|\\s)https://[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)";
        String replaceFormat = "[%s](%s)";

        // Need to escape characters like ?
        return this.replace(markdown, pattern, replaceFormat);
    }

    /**
     * Adjusts superscript tags from ^() to {@code <sup></sup>}
     *
     * @param markdown The markdown to adjust
     * @return The adjusted markdown
     */
    private String adjustSuperScript(String markdown) {
        // Every ^( needs to be replaced with "<sup>" and every ) that matches with a ^( needs to be
        // replaced with "</sup>"
        return markdown;
    }

    /**
     * Wraps reddit links to subreddits (r/... and /r/...) with markdown links
     *
     * @param markdown The markdown to adjust
     * @return The adjusted markdown
     */
    private String replaceSubredditLinks(String markdown) {
        // Matches either the start of the text, or a whitespace preceding the subreddit
        // TODO it should also match at the start anything not a normal character (so that superscript with ^r/subreddit) works correctly
        String pattern = "(^|\\s)/?(r|R)/[A-Za-z_]+";
        String replaceFormat = "[%s](https://www.reddit.com/%s/)";

        return this.replace(markdown, pattern, replaceFormat);
    }

    /**
     * Wraps reddit links to user profiles (u/... and /u/...) with markdown links
     *
     * @param markdown The markdown to adjust
     * @return The adjusted markdown
     */
    private String replaceRedditUserLinks(String markdown) {
        String pattern = "(^|\\s)/?(u|U)/[0-9A-Za-z_-]+";
        String replaceFormat = "[%s](https://www.reddit.com/%s/)";

        return this.replace(markdown, pattern, replaceFormat);
    }

    /**
     * Replaces every occurrence of a pattern with a given format
     *
     * <p>For reddit links that start with a slash (ie. /r/...) the first slash is removed</p>
     *
     * @param markdown The text to replace in
     * @param pattern The pattern to replace
     * @param replaceFormat The format to use for the replacement
     * @return The full replaced text
     */
    private String replace(String markdown, String pattern, String replaceFormat) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(markdown);

        // Map holding the "r/subreddit" mapped to its linked string "[r/subreddit](https://...)"
        Map<String, String> map = new HashMap<>();

        while (m.find()) {
            // Although we do care about the whitespace in front, we will later just replace
            // the subreddit text itself, so the whitespace won't matter
            String replaced = m.group().trim();

            String withoutSlash = replaced;

            // Remove the first slash if present (for reddit links)
            if (replaced.charAt(0) == '/') {
                withoutSlash = replaced.substring(1);
            }

            String formatted = String.format(replaceFormat, replaced, withoutSlash);

            map.put(replaced, formatted);
        }

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String sub = entry.getKey();
            String formatted = entry.getValue();

            // replaceAll is probably bad to use since we're actually only replacing the exact match each time
            markdown = markdown.replaceAll(sub, formatted);
        }

        return markdown;
    }



    /**
     * Adds spaces between a header symbol and the content
     *
     * @param markdown The markdown to change
     * @return A string with the markdown adjusted (if necessary)
     */
    private String addSpacesToHeader(String markdown) {
        // Not a header, return the text
        if (markdown.charAt(0) != '#') {
            return markdown;
        }

        // The starting point of the actual header
        int textStart = -1;
        boolean startTextFound = false;

        // Start at the next character
        int i = 1;

        // Find the starting point of the text content
        while (!startTextFound && i < markdown.length()) {
            char character = markdown.charAt(i);

            // No longer on the header (can be multiple hashtags, ie. ###Header) and first
            // time on a different character
            if (character != '#') {
                // Already a space here, no need to do anything
                if (character == ' ') {
                    break;
                }
                textStart = i;
            }

            startTextFound = textStart != -1;
            i++;
        }

        // If we need to insert the space
        if (textStart != -1) {
            StringBuilder stringBuilder = new StringBuilder(markdown);
            stringBuilder.insert(textStart, ' ');
            markdown = stringBuilder.toString();
        }

        return markdown;
    }

}
