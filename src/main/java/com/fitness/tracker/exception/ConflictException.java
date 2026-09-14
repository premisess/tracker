package com.fitness.tracker.exception;

import org.springframework.http.HttpStatus;

/** The change clashes with existing data, e.g. an email that's already registered. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
