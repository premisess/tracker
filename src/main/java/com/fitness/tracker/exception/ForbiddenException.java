package com.fitness.tracker.exception;

import org.springframework.http.HttpStatus;

/** The caller is signed in but not allowed to do this. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
