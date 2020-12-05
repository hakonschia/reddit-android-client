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
     * @return The code for the request. This will always be present
     */
    public int getCode() {
        return code;
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
