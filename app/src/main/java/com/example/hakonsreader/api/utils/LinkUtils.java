package com.example.hakonsreader.api.utils;

public final class LinkUtils {

    /**
     * Regex matching imgur image URLs
     */
    public static final String IMGUR_IMAGE_REGEX = "^https://imgur.com/[A-Za-z0-9]{5,7}$";

    /**
     * Regex matching imgur gif URLs.
     * <p>Matches both gif and gifv extensions</p>
     */
    public static final String IMGUR_GIF_REGEX = "^https://i.imgur.com/[A-Za-z0-9]{5,7}.(gif|gifv)$";

    private LinkUtils() {}


    /**
     * Converts a URL to a direct link to an image or gif so that the URL can be used
     * directly to load the image or gif.
     *
     * @param url The original URL
     * @return The converted URL. If it couldn't be converted the original URL is returned
     */
    public static String convertToDirectUrl(String url) {
        if (url.matches(IMGUR_IMAGE_REGEX)) {
            return url + ".png";
        }
        if (url.matches(IMGUR_GIF_REGEX)) {
            String u = url.replace("gifv", "mp4");
            u = u.replace("gif", "mp4");

            return u;
        }

        // TODO gfycat

        return url;
    }
}
