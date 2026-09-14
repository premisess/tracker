package com.fitness.tracker.exception;

import org.springframework.http.HttpStatus;

/**
 * An error whose message is safe to show the user, paired with the HTTP status it maps to.
 * GlobalExceptionHandler turns these into the standard ApiError JSON body.
 */
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
