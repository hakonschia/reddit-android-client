package com.example.hakonsreader.api.utils;

import android.util.Log;

import com.google.android.exoplayer2.extractor.mp4.PsshAtomUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        if (checkHeaderSpaces) {
            markdown = this.adjustHeaderSpaces(markdown);
        }
        if (checkRedditSpecificLinks) {
            markdown = this.adjustRedditSpecificLinks(markdown);
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


        // TODO I can probably find the position of a # and find the position of the first non-hashtag
        //  and just insert a space there if needed (without looping through the entire text)
        //  Not Sure if that would actually be faster, but I would assume so

        for (int i = 0; i < markdown.length(); i++) {
            char character = markdown.charAt(i);

            char previous = '\0';
            if (i != 0) {
                previous = markdown.charAt(i - 1);
            }

            // Now on a header (#) and the previous character wasn't a '(' (as in inside a link)
            if (character == '#' && previous != '(') {
                markdown = this.addSpacesToHeader(markdown, i);
            }
        }

        return markdown;
    }

    private String adjustRedditSpecificLinks(String markdown) {
        // Find every /r/, r/ and wrap that and the rest of the text until a non A-Za-z or _ character appears
        // Find every /u/, u/ and wrap that and the rest of the text until a non A-Za-z0-9, -, or _ character appears

        return markdown;
    }


    /**
     * Adds spaces between a header symbol and the content
     *
     * @param markdown The markdown to change
     * @param startPos The position of the first header symbol
     * @return A string with the markdown adjusted (if necessary)
     */
    private String addSpacesToHeader(String markdown, int startPos) {

        // The starting point of the actual header
        int startText = -1;

        boolean startTextFound = false;
        int i = startPos + 1;

        while (!startTextFound && i < markdown.length()) {
            char character = markdown.charAt(i);

            // No longer on the header (can be multiple hashtags, ie. ###Header) and first
            // time on a different character
            if (character != '#') {
                // Already a space here, no need to do anything
                if (character == ' ') {
                    break;
                }
                startText = i;
            }

            startTextFound = startText != -1;
            i++;
        }

        // If we need to insert the space
        if (startText != -1) {
            StringBuilder stringBuilder = new StringBuilder(markdown);
            stringBuilder.insert(startText, ' ');
            markdown = stringBuilder.toString();
        }

        return markdown;
    }

}
