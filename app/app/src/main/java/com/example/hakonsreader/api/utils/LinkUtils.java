package com.example.hakonsreader.api.utils;


/**
 * Utility class that contains various regex matchers and functions to deal with common problems for
 * links from Reddit
 */
public final class LinkUtils {

    /**
     * Regex for matching a subreddit URL. Matches either a full URL (only https, www optional) or only "r/...."
     *
     * <p>Examples:</p>
     * <ol>
     * <li>r/GlobalOffensive</li>
     * <li>/r/instant_karma</li>
     * <li>R/GlobalOffensive</li>
     * <li>/R/Hello</li>
     * <li>https://www.reddit.com/r/test</li>
     * <li>https://reddit.com/r/test</li>
     * </ol>
     */
    // TODO this isn't 100 % as it will match "..reddit.comr/subreddit" since the first slash is optional
    //  it has to be there for the full link, but if it's added to the actual link part that is optional, adding
    //  as an optional to the rest will make it so you can match two (ie ".com//r/subreddit")
    public static final String SUBREDDIT_REGEX = "(https://(www.)?reddit.com)?/?(r|R)/[A-Za-z0-9_]+/?";

    /**
     * Regex for matching a URL to a user. Matches either a full URL (only https, www optional) or only "u/...."
     *
     * <p>Examples:</p>
     * <ol>
     * <li>u/hakonschia</li>
     * <li>/u/hakonschia</li>
     * <li>user/hakonschia</li>
     * <li>/user/hakonschia_two</li>
     * <li>/user/hakonschia-three</li>
     * </ol>
     */
    public static final String USER_REGEX = "(https://(www.)?reddit.com)?/?u(ser)?/[A-Za-z0-9_-]+/?";

    /**
     * Regex matching a post URL
     *
     * <p>Note that this doesn't check for the start of the string (^) so it can be used for
     * both stand-alone subreddit references (only r/...) and full URLs (reddit.com/r/...)</p>
     *
     * <p>Example: r/GlobalOffensive/comments/55ter/FaZe_Wins_major</p>
     */
    public static final String POST_REGEX = ".*/?(r|R)/[A-Za-z0-9]+/comments/.+/.+/?$";

    /**
     * Regex matching imgur image URLs
     */
    public static final String IMGUR_IMAGE_REGEX = "^https://imgur.com/[A-Za-z0-9]{5,7}$";

    /**
     * Regex matching imgur GIF URLs.
     * <p>Matches both gif and gifv extensions</p>
     */
    public static final String IMGUR_GIF_REGEX = "^https://i.imgur.com/[A-Za-z0-9]{5,7}.(gif|gifv)$";

    /**
     * Regex for GIF URLs
     */
    public static final String GIF_REGEX = "^.*(gif(v){0,1})$";

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

        // Replace .gif or .gifv with .mp4
        if (url.matches(GIF_REGEX)) {
            String u = url.replace("gifv", "mp4");
            u = u.replace("gif", "mp4");

            return u;
        }

        // TODO gfycat

        return url;
    }
}
