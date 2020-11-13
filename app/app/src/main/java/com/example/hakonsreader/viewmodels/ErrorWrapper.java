package com.example.hakonsreader.viewmodels;

import com.example.hakonsreader.api.responses.GenericError;

/**
 * Wrapper for errors given by the API to communicate from a ViewModel to its fragment/activity
 */
public class ErrorWrapper {

    private final GenericError error;
    private final Throwable throwable;

    public ErrorWrapper(GenericError error, Throwable throwable) {
        this.error = error;
        this.throwable = throwable;
    }

    /**
     * @return The GenericError
     */
    public GenericError getError() {
        return error;
    }

    /**
     * @return The throwable for the error
     */
    public Throwable getThrowable() {
        return throwable;
    }
}
