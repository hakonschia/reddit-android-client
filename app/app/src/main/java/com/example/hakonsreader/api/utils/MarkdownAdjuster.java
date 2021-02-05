package com.example.hakonsreader.api.utils;


import com.example.hakonsreader.activites.ImageActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class for adjusting some generic Reddit markdown faults
 *
 * <p>Use {@link MarkdownAdjuster.Builder} to create new objects</p>
 */
public class MarkdownAdjuster {
    private boolean checkRedditSpecificLinks;
    private boolean checkHeaderSpaces;
    private boolean checkNormalLinks;
    private boolean checkUrlEncoding;
    private boolean convertImageLinksToMarkdown;

    private MarkdownAdjuster() {}

    public static class Builder {
        boolean bCheckRedditSpecificLinks = false;
        boolean bCheckHeaderSpaces = false;
        boolean bCheckNormalLinks = false;
        boolean bCheckUrlEncoding = false;
        boolean bConvertImageLinksToMarkdown = false;


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
            bCheckNormalLinks = true;
            return this;
        }

        /**
         * Checks links in markdown links for errors in URL encoding which might cause markdown
         * renderers not to recognize them as URLs.
         *
         * This will check and replace:
         * <ol>
         *     <li>Spaces to %20</li>
         *     <li>Double quotes <i>"</i> to %22</li>
         *     <li>Curly brackets to %7B <i>{</i> and %7D <i>}</i></li>
         * </ol>
         *
         * Eg. "[link](https://link.com/{some path})" will become "[link](https://link.com/%7Bsome%20path%7D")
         *
         * @return This builder
         */
        public Builder checkUrlEncoding() {
            bCheckUrlEncoding = true;
            return this;
        }

        /**
         * Wraps URLs pointing to images in Markdown image formatting to inline images directly.
         *
         * Ie. "https://i.redd.it/z4sgyaoenlf61.png" will be converted to "![image](https://i.redd.it/z4sgyaoenlf61.png)"
         *
         * Only URLs with https will be matched (not http)
         *
         * @return This builder
         */
        public Builder convertImageLinksToMarkdown() {
            bConvertImageLinksToMarkdown = true;
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
            adjuster.checkUrlEncoding = bCheckUrlEncoding;
            adjuster.convertImageLinksToMarkdown = bConvertImageLinksToMarkdown;

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
        if (checkUrlEncoding) {
            markdown = this.adjustUrlEncoding(markdown);
        }
        if (convertImageLinksToMarkdown) {
            markdown = this.convertImageLinks(markdown);
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
        // TODO this ruins the formatting
        String[] lines = markdown.split("\n");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            line = this.addSpacesToHeader(line);

            // The line object has now either been edited, or is as it should be, so append it on the builder
            builder.append(line);

            // Add back the newline unless we are at the end
            if (i + 1 != lines.length) {
                builder.append("\n");
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

            markdown = markdown.replace(sub, formatted);
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
        if (markdown.isEmpty() || markdown.charAt(0) != '#') {
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


    /**
     * Adjusts incorrect URL encoding in markdown links
     *
     * @param markdown The markdown to adjust
     * @return The adjusted markdown
     */
    private String adjustUrlEncoding(String markdown) {
        // Find all markdown links: (some text)[some text which is the link]
        // Replace characters that should be replaced
        // Spaces = %20
        // Double quotes = %22
        // Opening curly brackets = %7B
        // Closing curly brackets = %7D

        // Match []() with anything between both [] and ()
        Pattern p = Pattern.compile("\\[([^\\[\\])]+)]\\(([^()]+)\\)");
        Matcher m = p.matcher(markdown);

        // The amount of characters added to the markdown string during the loop
        int charactersAdded = 0;

        while (m.find()) {
            int start = m.start();
            String group = m.group();

            // Find pos of the first "]" as that will be the end of the text, the next pos is a "(" which is the
            // start of the link wrapper, and the link is everything from after the "(" to a ")"
            // From "](<here>)" to "<here>)"
            int linkStart = group.indexOf(']') + 2;
            int linkEnd = group.length() - 1;

            String link = group.substring(linkStart, linkEnd);

            int markdownLength = markdown.length();

            // Perform all replacements
            link = link.replaceAll(" ", "%20");
            link = link.replaceAll("\"", "%22");
            link = link.replaceAll("\\{", "%7B");
            // If "}" isn't escaped it fails when using it in the android app, but it doesn't in tests ¯\_(ツ)_/¯
            link = link.replaceAll("\\}", "%7D");

            // Replace the new link in the original text
            StringBuilder buffer = new StringBuilder(markdown);

            int startInMarkdown = start + linkStart + charactersAdded;
            int endInMarkdown = start + linkEnd + charactersAdded;
            buffer.replace(startInMarkdown, endInMarkdown, link);

            markdown = buffer.toString();

            // For when multiple matches occur we need to save how many characters have been previously added
            // as we're changing the markdown text in the loop, which messes up the matcher somewhat
            // as "start" will be what the start would have been before any changes
            charactersAdded += markdown.length() - markdownLength;
        }

        return markdown;
    }

    /**
     * Wraps image URLs in markdown images, ie.: ![image](https://imgur.com/rthrth.png)
     *
     * @param markdown The markdown to adjust
     * @return The adjusted markdown
     */
    private String convertImageLinks(String markdown) {
        // This would also match stuff like "https:///.png", but I'm taking the chance that that won't happen
        // over bothering to create a proper URL regex (it very likely won't happen)
        // This has to match https only, as this will load actual images over the network, which might not
        // support cleartext
        String pattern = "(^|\\s)https://" +
                // This will somewhat limit the URL correctly as it requires a slash before the image format
                ".*/.*" +
                "(" +
                "\\.(png|jpg|jpeg)" +
                // Kind of (very) bad as the format HAS to be the first query parameter
                "|(\\?format=(png|jpg|jpeg))" +
                ")" +
                // Match anything afterwards until a whitespace or end of line (if .png isn't the last, or ?format=png&something=else)
                "[^\\s]*(\\s|$)";
        String replaceFormat = "![image](%s)";

        return this.replace(markdown, pattern, replaceFormat);
    }
}
