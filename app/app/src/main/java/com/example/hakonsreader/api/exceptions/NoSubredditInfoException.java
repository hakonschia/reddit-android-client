package com.example.hakonsreader.api.exceptions;

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
