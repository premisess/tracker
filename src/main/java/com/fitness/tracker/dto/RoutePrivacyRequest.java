package com.fitness.tracker.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoutePrivacyRequest {
    // 0 (off), 200, 500 or 1000 metres.
    @NotNull(message = "Choose a privacy zone")
    private Integer routePrivacyMeters;
}
