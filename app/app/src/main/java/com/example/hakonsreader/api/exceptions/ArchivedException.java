package com.example.hakonsreader.api.exceptions;

public class ArchivedException extends Exception {

    public ArchivedException() {
    }

    public ArchivedException(String message) {
        super(message);
    }

    public ArchivedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchivedException(Throwable cause) {
        super(cause);
    }

}
