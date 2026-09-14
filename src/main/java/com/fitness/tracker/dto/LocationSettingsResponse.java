package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LocationSettingsResponse {
    private boolean locationConsent;
    private LocalDateTime locationConsentAt;
    private int routePrivacyMeters;
}
