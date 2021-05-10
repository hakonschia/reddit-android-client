package com.example.hakonsreader.api.exceptions;

/**
 * Exception used to indicate that an access token isn't valid for a logged in user
 */
public class InvalidAccessTokenException extends Exception {

    public InvalidAccessTokenException() {

    }

    public InvalidAccessTokenException(String message) {
        super(message);
    }

    public InvalidAccessTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidAccessTokenException(Throwable cause) {
        super(cause);
    }

}
