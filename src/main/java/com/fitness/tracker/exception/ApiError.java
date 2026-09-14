package com.fitness.tracker.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * The one error shape every endpoint returns. The frontend shows {@code message};
 * {@code fieldErrors} lets a form highlight the individual inputs that failed validation.
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private int status;
    private String error;
    private String message;
    private Map<String, String> fieldErrors;
    private String timestamp;
}
