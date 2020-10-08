package com.example.hakonsreader.api.utils;

public class MarkdownAdjuster {

    private boolean checkRedditSpecificLinks;
    private boolean checkHeaderSpaces;

    private MarkdownAdjuster() {}

    public static class Builder {
        boolean bCheckRedditSpecificLinks = false;
        boolean bCheckHeaderSpaces = false;


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
         * Sets if the adjuster should look for strings such as "r/GlobalOffensive" and wrap them in
         * markdown links
         *
         * @return This builder
         */
        public Builder checkRedditSpecificLinks() {
            bCheckRedditSpecificLinks = true;
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
        return markdown;
    }
}
