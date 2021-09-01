package com.example.hakonsreader.api.utils;


/**
 * Utility class that contains various regex matchers and functions to deal with common problems for
 * links from Reddit
 */
public final class LinkUtils {

    /**
     * Basic subreddit regex that only matches "{@code r/<allowed_subreddit_characters>}". This
     * does not match a preceding or trailing slash
     */
    public static final String BASE_SUBREDDIT_REGEX = "(r|R)/[A-Za-z0-9_]+";

    /**
     * Regex for matching a subreddit URL. Matches a full URL (https or http, www optional)
     *
     * <p>Examples:</p>
     * <ol>
     * <li>http://www.reddit.com/r/test</li>
     * <li>https://www.reddit.com/r/test</li>
     * <li>https://old.reddit.com/r/test</li>
     * </ol>
     *
     * @see LinkUtils#SUBREDDIT_REGEX_NO_HTTPS
     * @see LinkUtils#SUBREDDIT_REGEX_COMBINED
     */
    public static final String SUBREDDIT_REGEX_WITH_HTTPS = "(^|\\s)http(s)?://([A-Za-z0-9]+\\.)?reddit.com/" + BASE_SUBREDDIT_REGEX + "/?(\\s|$)";

    /**
     * Regex for matching a subreddit URL with sorting. This will match with subpaths "hot", "new", "top", and "controversial"
     * with query parameter support
     *
     * <p>Examples:</p>
     * <ol>
     * <li>http://www.reddit.com/r/test</li>
     * <li>https://www.reddit.com/r/test</li>
     * <li>https://old.reddit.com/r/test</li>
     * </ol>
     */
    public static final String SUBREDDIT_SORT_REGEX_WITH_HTTPS = "(^|\\s)http(s)?://([A-Za-z0-9]+\\.)?reddit.com/(r|R)/[A-Za-z0-9_]+/(hot|top|new|controversial)(/)?(\\?([A-Za-z0-9&=])*)?(\\s|$)";

    /**
     * Regex for matching a subreddit URL linking to the subreddit rules
     *
     * <p>Examples:</p>
     * <ol>
     * <li>http://www.reddit.com/r/test/about/rules</li>
     * <li>https://www.reddit.com/r/test/about/rules/</li>
     * <li>https://old.reddit.com/r/test/about/rules</li>
     * </ol>
     */
    public static final String SUBREDDIT_RULES_REGEX_WITH_HTTPS = "(^|\\s)http(s)?://([A-Za-z0-9]+\\.)?reddit.com/(r|R)/[A-Za-z0-9_]+/about/rules(\\/)?(\\s|$)";

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
    public static final String SUBREDDIT_REGEX_NO_HTTPS = "(^|\\s)/?" + BASE_SUBREDDIT_REGEX + "/?(\\s|$)";

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
    public static final String USER_REGEX = "(^|\\s)(https://([A-Za-z0-9]+\\.)?reddit.com)?/?u(ser)?/[A-Za-z0-9_-]+/?(\\s|$)";

    /**
     * Regex matching a post URL
     *
     * <p>Examples:
     * <ol>
     *     <li>https://reddit.com/r/GlobalOffensive/comments/55ter</li>
     *     <li>>https://reddit.com/R/GlobalOffensive/comments/55ter/</li>
     *     <li>>https://np.reddit.com/R/GlobalOffensive/comments/55ter/FaZe_Wins_major</li>
     *     <li>>https://old.reddit.com/R/GlobalOffensive/comments/55ter/FaZe_Wins_major</li>
     * </ol>
     * </p>
     */
    public static final String POST_REGEX = "http(s)://([A-Za-z0-9]+\\.)?reddit.com/" + BASE_SUBREDDIT_REGEX + "/comments/.+/?(\\s|$)";

    /**
     * Matches a post URL with only {@code https://reddit.com/comments/<postId>}
     *
     * <p>Examples:
     * <ol>
     *     <li>comments/55ter</li>
     *     <li>comments/55ter/FaZe_wins_major</li>
     * </ol>
     * </p>
     */
    public static final String POST_REGEX_NO_SUBREDDIT = "http(s)://([A-Za-z0-9]+\\.)?reddit.com/comments/.+";

    /**
     * Matches a shortened URL for a post.
     *
     * <p>For these links the first (and only) path segment is the ID of the post</p>
     *
     * <p>Example: https://redd.it/jtpvml</p>
     */
    public static final String POST_SHORTENED_URL_REGEX = "http(s)?://(www.)?redd.it/.+";

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
