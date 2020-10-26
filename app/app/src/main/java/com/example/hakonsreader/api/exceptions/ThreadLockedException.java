package com.example.hakonsreader.api.exceptions;

/**
 * Exception for when a thread has been locked and cannot be interacted with
 */
public class ThreadLockedException extends Exception {
    public ThreadLockedException() {
    }

    public ThreadLockedException(String message) {
        super(message);
    }

    public ThreadLockedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ThreadLockedException(Throwable cause) {
        super(cause);
    }

    public ThreadLockedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
