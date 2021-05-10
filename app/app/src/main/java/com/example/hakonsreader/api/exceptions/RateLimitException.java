package com.example.hakonsreader.api.exceptions;

/**
 * Exception for when a user has been rate limited by Reddit
 */
public class RateLimitException extends Exception {

    public RateLimitException() {
    }

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }

    public RateLimitException(Throwable cause) {
        super(cause);
    }

}
