package com.fitness.tracker.controller;

import com.fitness.tracker.dto.AnalyticsSummary;
import com.fitness.tracker.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummary> getSummary() {
        return ResponseEntity.ok(analyticsService.getSummary());
    }
}
