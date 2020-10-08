package com.example.hakonsreader.api.utils;


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
        // If we are in a link [](#here) it isn't actually a header, so we shouldn't add a space

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
                    builder.append('\n');
                }
            }
        }

        return builder.toString();
    }

    private String adjustRedditSpecificLinks(String markdown) {
        // Find every /r/, r/ and wrap that and the rest of the text until a non A-Za-z or _ character appears
        // Find every /u/, u/ and wrap that and the rest of the text until a non A-Za-z0-9, -, or _ character appears

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
        // Contains the name of every subreddit (the /r/ is removed)
        // TODO this should only match exactly r/... and /r/... as if there are already linked subreddits it messes stuff up
        String[] subreddits = markdown.split("/?r/");

        StringBuilder builder = new StringBuilder();

        // The first element will never be an actual subreddit. If the markdown starts with a "/r/"
        // it will be an empty string, otherwise it will be the text before the first actual sub
        builder.append(subreddits[0]);

        for (int i = 1; i < subreddits.length; i++) {
            String s = subreddits[i];

            int endPos = s.length();
            boolean endFound = false;

            // Find where the subreddit ends (ie. where the first character not allowed in a subreddit is)
            int j = 0;
            while (!endFound && j < s.length()){
                if (ALLOWED_SUBREDDIT_CHARACTERS.indexOf(s.charAt(j)) == -1) {
                    endPos = j;
                }
                j++;
                endFound = endPos != s.length();
            }

            // Get the subreddit name and create the link around it
            String sub = s.substring(0, endPos);
            String linked = String.format("[r/%1$s](https://www.reddit.com/r/%1$s/)", sub);

            // The string after the link
            String after = "";
            if (endPos < s.length()) {
                after = s.substring(endPos);
            }

            String full = linked + after;
            builder.append(full);
        }

        markdown = builder.toString();

        return markdown;
    }

    /**
     * Wraps reddit links to user profiles (u/... and /u/...) with markdown links
     *
     * @param markdown The markdown to adjust
     * @return The adjuset markdown
     */
    private String replaceRedditUserLinks(String markdown) {
        return markdown;
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
