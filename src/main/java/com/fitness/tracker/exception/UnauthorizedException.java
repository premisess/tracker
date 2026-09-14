package com.fitness.tracker.exception;

import org.springframework.http.HttpStatus;

/** The caller isn't signed in, or their session points at a user that no longer exists. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
