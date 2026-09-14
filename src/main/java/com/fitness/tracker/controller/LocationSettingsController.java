package com.fitness.tracker.controller;

import com.fitness.tracker.dto.LocationConsentRequest;
import com.fitness.tracker.dto.LocationSettingsResponse;
import com.fitness.tracker.dto.RoutePrivacyRequest;
import com.fitness.tracker.service.RunService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class LocationSettingsController {

    private final RunService runService;

    public LocationSettingsController(RunService runService) {
        this.runService = runService;
    }

    @GetMapping("/location-settings")
    public ResponseEntity<LocationSettingsResponse> settings() {
        return ResponseEntity.ok(runService.settings());
    }

    @PutMapping("/location-consent")
    public ResponseEntity<LocationSettingsResponse> consent(@Valid @RequestBody LocationConsentRequest request) {
        return ResponseEntity.ok(runService.setConsent(request.getConsent()));
    }

    @PutMapping("/route-privacy")
    public ResponseEntity<LocationSettingsResponse> routePrivacy(@Valid @RequestBody RoutePrivacyRequest request) {
        return ResponseEntity.ok(runService.setRoutePrivacy(request.getRoutePrivacyMeters()));
    }

    @DeleteMapping("/routes")
    public ResponseEntity<Map<String, Integer>> deleteRoutes() {
        return ResponseEntity.ok(Map.of("deleted", runService.deleteMyRoutes()));
    }
}
