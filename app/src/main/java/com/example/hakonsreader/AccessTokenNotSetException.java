package com.example.hakonsreader;

/**
 * Exception used to indicate that an access token isn't set
 */
public class AccessTokenNotSetException extends Exception {

    public AccessTokenNotSetException(String message) {
        super(message);
    }
}
