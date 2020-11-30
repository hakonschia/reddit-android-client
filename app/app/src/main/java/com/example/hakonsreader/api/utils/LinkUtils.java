package com.example.hakonsreader.api.utils;


/**
 * Utility class that contains various regex matchers and functions to deal with common problems for
 * links from Reddit
 */
public final class LinkUtils {

    /**
     * Basic subreddit regex that only matches "{@code < r/<allowed_subreddit_characters>}". This
     * does not match a preceding or trailing slash
     */
    public static final String BASE_SUBREDDIT_REGEX = "(r|R)/[A-Za-z0-9_]+";

    /**
     * Regex for matching a subreddit URL. Matches either a full URL (only https, www optional) or only "r/...."
     *
     * <p>Examples:</p>
     * <ol>
     * <li>https://www.reddit.com/r/test</li>
     * <li>https://reddit.com/r/test</li>
     * </ol>
     *
     * @see LinkUtils#SUBREDDIT_REGEX_NO_HTTPS
     * @see LinkUtils#SUBREDDIT_REGEX_COMBINED
     */
    // TODO instead of www. match anything (since it can be old.reddit.com etc.)
    public static final String SUBREDDIT_REGEX_WITH_HTTPS = "(^|\\s)https://(www.)?reddit.com/" + BASE_SUBREDDIT_REGEX + "+/?(\\s|$)";

    /**
     * Regex for matching a subreddit string with only the "/r/..." part (no https://reddit.com).
     * For a full URL matcher use {@link LinkUtils#SUBREDDIT_REGEX_WITH_HTTPS}.
     *
     * <p>The slashes at the beginning (/r..) and the end (r/globaloffensive/) are optional</p>
     *
     * <p>Examples:</p>
     * <ol>
     * <li>r/GlobalOffensive</li>
     * <li>/r/instant_karma</li>
     * <li>R/GlobalOffensive/</li>
     * <li>/R/Hello</li>
     * </ol>
     *
     * @see LinkUtils#SUBREDDIT_REGEX_WITH_HTTPS
     * @see LinkUtils#SUBREDDIT_REGEX_COMBINED
     */
    public static final String SUBREDDIT_REGEX_NO_HTTPS = "(^|\\s)/?" + BASE_SUBREDDIT_REGEX + "+/?(\\s|$)";

    /**
     * Regex that matches either {@link LinkUtils#SUBREDDIT_REGEX_WITH_HTTPS} or {@link LinkUtils#SUBREDDIT_REGEX_NO_HTTPS}
     *
     * @see LinkUtils#SUBREDDIT_REGEX_WITH_HTTPS
     * @see LinkUtils#SUBREDDIT_REGEX_NO_HTTPS
     */
    public static final String SUBREDDIT_REGEX_COMBINED = String.format("(%s)|(%s)", SUBREDDIT_REGEX_WITH_HTTPS, SUBREDDIT_REGEX_NO_HTTPS);

    /**
     * Basic subreddit regex that only matches "{@code < u(ser)/<allowed_subreddit_characters>}". This
     * does not match a preceding or trailing slash
     */
    public static final String BASE_USER_REGEX = "u(ser)?/[A-Za-z0-9_-]+";

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
    public static final String USER_REGEX = "(^|\\s)(https://(.*)?reddit.com)?/?u(ser)?/[A-Za-z0-9_-]+/?(\\s|$)";

    /**
     * Regex matching a post URL
     *
     * <p>Examples:
     * <ol>
     *     <li>r/GlobalOffensive/comments/55ter</li>
     *     <li>R/GlobalOffensive/comments/55ter/</li>
     *     <li>/R/GlobalOffensive/comments/55ter/FaZe_Wins_major</li>
     *     <li>/R/GlobalOffensive/comments/55ter/FaZe_Wins_major</li>
     * </ol>
     * </p>
     */
    public static final String POST_REGEX = "http(s)://(.*)?reddit.com/" + BASE_SUBREDDIT_REGEX + "+/comments/.+/?(\\s|$)";

    /**
     * Matches a post URL with only {@code /comments/<postId>}
     *
     * <p>Examples:
     * <ol>
     *     <li>comments/55ter</li>
     *     <li>comments/55ter/FaZe_wins_major</li>
     * </ol>
     * </p>
     */
    public static final String POST_REGEX_NO_SUBREDDIT = ".*/comments/.+";

    /**
     * Matches a shortened URL for a post.
     *
     * <p>For these links the first (and only) path segment is the ID of the post</p>
     *
     * <p>Example: https://redd.it/jtpvml</p>
     */
    public static final String POST_SHORTENED_URL_REGEX = "http(s)?://redd.it/.+";

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
    public static final String GIF_REGEX = "^.*(gif(v)?)$";

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

        return url;
    }
}
