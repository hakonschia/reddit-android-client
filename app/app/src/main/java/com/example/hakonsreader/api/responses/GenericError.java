package com.example.hakonsreader.api.responses;

import com.google.gson.annotations.SerializedName;


/**
 * Represents an error returned by Reddit. These errors are given in cases such as
 * a subreddit has been banned or is private.
 *
 * <p>The class contains three fields: {@code code}, {@code message}, and {@code reason}. {@code code} will always be
 * present, but {@code message} and {@code reason} might not be.</p>
 */
public class GenericError {

    /**
     * Possible value for {@link GenericError#getReason()} that represents that a subreddit
     * is private
     */
    public static final String SUBREDDIT_PRIVATE = "private";

    /**
     * Possible value for {@link GenericError#getReason()} that represents that a subreddit
     * has been banned
     */
    public static final String SUBREDDIT_BANNED = "banned";

    /**
     * Possible value for {@link GenericError#getReason()} that represents that an action is attempted
     * that requires the user to have Reddit premium
     */
    public static final String REQUIRES_REDDIT_PREMIUM = "gold_only";

    /**
     * Error for when a wiki page hasn't been created. This error will occur when a moderator of the subreddit
     * the wiki is in tries to access a non-existing wiki page, with a 404 error code.
     * When a non-moderator user access this page the error will be {@link #WIKI_DISABLED} with a 403 code
     */
    public static final String WIKI_PAGE_NOT_CREATED = "PAGE_NOT_CREATED";

    /**
     * Error for when a wiki page is disabled (does not exist). This error will occur when a non-moderator of the subreddit
     * the wiki is in tries to access a non-existing wiki page, with a 403 error code. When a moderator user access this page
     * the error will be {@link #WIKI_PAGE_NOT_CREATED} with a 404 error code
     */
    public static final String WIKI_DISABLED = "WIKI_DISABLED";



    @SerializedName("error")
    private int code;

    @SerializedName("message")
    private String message;

    @SerializedName("reason")
    private String reason;


    public GenericError(int code) {
        this.code = code;
    }

    /**
     * Retrieves the code for the request. This will generally be an HTTP status code
     *
     * @return The code for the request
     */
    public int getCode() {
        return code;
    }

    /**
     * Sets the code for the request. This should only be used in cases where the error returned from
     * the API doesn't give an explicit code
     *
     * @param code The code for the request
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * Retrieves the reason the request failed. See constants in the class for some known reasons
     *
     * @return The reason for the request fail (this might ne null)
     */
    public String getMessage() {
        return message;
    }

    /**
     * Retrieves the reason the request failed. See constants in the class for some known reasons
     *
     * @return The reason for the request fail (this might ne null)
     */
    public String getReason() {
        return reason;
    }

}
