package com.example.hakonsreader.api.exceptions;

public class SubredditNotFoundException extends Exception {
    public SubredditNotFoundException() {
    }

    public SubredditNotFoundException(String message) {
        super(message);
    }

    public SubredditNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubredditNotFoundException(Throwable cause) {
        super(cause);
    }

    public SubredditNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
