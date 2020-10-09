package com.example.hakonsreader.api.utils;


import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownAdjuster {
    private static final String ALLOWED_SUBREDDIT_CHARACTERS = "_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private boolean checkRedditSpecificLinks;
    private boolean checkHeaderSpaces;
    private boolean checkNormalLinks;


    private MarkdownAdjuster() {}

    public static class Builder {
        boolean bCheckRedditSpecificLinks = false;
        boolean bCheckHeaderSpaces = false;
        boolean bCheckNormalLinks = false;


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
         * Builds the MarkdownAdjuster
         *
         * @return The MarkdownAdjuster
         */
        public MarkdownAdjuster build() {
            MarkdownAdjuster adjuster = new MarkdownAdjuster();
            adjuster.checkHeaderSpaces = bCheckHeaderSpaces;
            adjuster.checkRedditSpecificLinks = bCheckRedditSpecificLinks;
            adjuster.checkNormalLinks = bCheckNormalLinks;

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
            markdown = this.replaceNormalLinks(markdown);
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

    private String adjustRedditSpecificLinks(String markdown) {
        // Find every /r/, r/ and wrap that and the rest of the text until a non A-Za-z or _ character appears
        // Find every /u/, u/ and wrap that and the rest of the text until a non A-Za-z0-9, -, or _ character appears

        // TODO check if it is already in a link, if so dont wrap again
        markdown = this.replaceSubredditLinks(markdown);
        markdown = this.replaceRedditUserLinks(markdown);

        return markdown;
    }


    /**
     * Wraps reddit links to subreddits (r/... and /r/...) with markdown links
     *
     * @param markdown The markdown to adjust
     * @return The adjuset markdown
     */
    private String replaceSubredditLinks(String markdown) {
        // Matches either the start of the text, or a whitespace preceding the subreddit
        String pattern = "(^|\\s)/?(r|R)/[A-Za-z_]+";
        String replaceFormat = "[%s](https://www.reddit.com/%s/)";

        return this.replaceRedditLinks(markdown, pattern, replaceFormat);
    }

    /**
     * Wraps reddit links to user profiles (u/... and /u/...) with markdown links
     *
     * @param markdown The markdown to adjust
     * @return The adjuset markdown
     */
    private String replaceRedditUserLinks(String markdown) {
        String pattern = "(^|\\s)/?(u|U)/[0-9A-Za-z_-]+";
        String replaceFormat = "[%s](https://www.reddit.com/%s/)";

        return this.replaceRedditLinks(markdown, pattern, replaceFormat);
    }

    /**
     * Wraps normal links that aren't already wrapped with markdown links
     *
     * @param markdown The markdown to adjust
     * @return The adjuset markdown
     */
    private String replaceNormalLinks(String markdown) {
        return markdown;
    }

    /**
     * Replaces every occurrence of a reddit link pattern with a given format
     *
     * @param markdown The text to replace in
     * @param pattern The pattern to replace
     * @param replaceFormat The format to use for the replacement
     * @return
     */
    private String replaceRedditLinks(String markdown, String pattern, String replaceFormat) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(markdown);

        // Map holding the "r/subreddit" mapped to its linked string "[r/subreddit](https://...)"
        Map<String, String> map = new HashMap<>();

        while (m.find()) {
            // Although we do care about the whitespace in front, we will later just replace
            // the subreddit text itself, so the whitespace won't matter
            String replaced = m.group().trim();

            String withoutSlash = replaced;

            // Remove the first slash if present
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
