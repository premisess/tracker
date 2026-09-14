package com.fitness.tracker.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationConsentRequest {
    @NotNull(message = "Say whether location tracking should be on or off")
    private Boolean consent;
}
