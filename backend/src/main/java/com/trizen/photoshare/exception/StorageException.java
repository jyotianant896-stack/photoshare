package com.trizen.photoshare.exception;

import org.springframework.http.HttpStatus;

public class StorageException extends ApiException {
    public StorageException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
