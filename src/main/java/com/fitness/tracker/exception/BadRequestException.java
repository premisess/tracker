package com.fitness.tracker.exception;

import org.springframework.http.HttpStatus;

/** The request is invalid, e.g. a wrong current password or an unknown workout type. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
