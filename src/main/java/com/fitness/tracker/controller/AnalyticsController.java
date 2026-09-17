package com.fitness.tracker.controller;

import com.fitness.tracker.dto.AnalyticsSummary;
import com.fitness.tracker.security.CurrentUserService;
import com.fitness.tracker.service.AnalyticsService;
import com.fitness.tracker.service.UltimateGuard;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;
    private final UltimateGuard ultimateGuard;

    public AnalyticsController(AnalyticsService analyticsService, CurrentUserService currentUserService,
                               UltimateGuard ultimateGuard) {
        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
        this.ultimateGuard = ultimateGuard;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummary> getSummary() {
        ultimateGuard.require(currentUserService.get(), "Advanced analytics");
        return ResponseEntity.ok(analyticsService.getSummary());
    }
}
