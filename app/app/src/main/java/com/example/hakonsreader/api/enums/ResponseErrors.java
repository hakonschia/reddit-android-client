package com.example.hakonsreader.api.enums;

/**
 * Enum representing errors that can be returned from API requests
 */
public enum ResponseErrors {
    /**
     * Error for when trying to do an action on a thread that has been locked
     */
    THREAD_LOCKED("THREAD_LOCKED"),

    /**
     * Error for when an action has been repeated too many times in a short time span
     */
    RATE_LIMIT("RATELIMIT");


    private final String value;

    ResponseErrors(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
