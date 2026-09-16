package com.fitness.tracker.controller;

import com.fitness.tracker.dto.PlanDtos.ActivePlan;
import com.fitness.tracker.dto.PlanDtos.PlanDetail;
import com.fitness.tracker.dto.PlanDtos.PlanSummary;
import com.fitness.tracker.dto.PlanSessionRequest;
import com.fitness.tracker.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<List<PlanSummary>> list() {
        return ResponseEntity.ok(planService.listPlans());
    }

    /** 204 No Content when the user isn't following a plan. */
    @GetMapping("/active")
    public ResponseEntity<ActivePlan> active() {
        return planService.active()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/active/sessions")
    public ResponseEntity<ActivePlan> logSession(@Valid @RequestBody PlanSessionRequest request) {
        return ResponseEntity.ok(planService.logSession(request));
    }

    @DeleteMapping("/active")
    public ResponseEntity<Void> quit() {
        planService.quit();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PlanDetail> get(@PathVariable String slug) {
        return ResponseEntity.ok(planService.getPlan(slug));
    }

    @PostMapping("/{slug}/start")
    public ResponseEntity<ActivePlan> start(@PathVariable String slug,
                                            @RequestParam(defaultValue = "false") boolean replace) {
        return ResponseEntity.ok(planService.start(slug, replace));
    }
}
