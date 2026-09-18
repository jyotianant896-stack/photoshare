package com.trizen.photoshare.exception;

import org.springframework.http.HttpStatus;

/** Base class for errors that map cleanly onto an HTTP status. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
