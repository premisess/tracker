package com.fitness.tracker.exception;

import org.springframework.http.HttpStatus;

/** The resource doesn't exist, or belongs to another user (never reveal which). */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
