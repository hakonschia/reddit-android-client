package com.example.hakonsreader.api.exceptions;

/**
 * Exception for when there has been an attempt to get information about a subreddit that
 * doesn't have, and never will have, information (such as front page)
 */
public class NoSubredditInfoException extends Exception {

    public NoSubredditInfoException() {
    }

    public NoSubredditInfoException(String message) {
        super(message);
    }

    public NoSubredditInfoException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSubredditInfoException(Throwable cause) {
        super(cause);
    }
}
