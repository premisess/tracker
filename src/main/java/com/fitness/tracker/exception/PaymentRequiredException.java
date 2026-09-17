package com.fitness.tracker.exception;

import org.springframework.http.HttpStatus;

/** The feature is part of FitTracker Ultimate and the user doesn't have it. The website shows an upgrade prompt. */
public class PaymentRequiredException extends ApiException {

    public PaymentRequiredException(String message) {
        super(HttpStatus.PAYMENT_REQUIRED, message);
    }
}
