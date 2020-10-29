package com.example.hakonsreader.viewmodels;

import com.example.hakonsreader.api.responses.GenericError;

public class ErrorWrapper {

    private GenericError error;
    private Throwable throwable;

    public ErrorWrapper(GenericError error, Throwable throwable) {
        this.error = error;
        this.throwable = throwable;
    }


    public GenericError getError() {
        return error;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
