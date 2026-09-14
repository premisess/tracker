package com.fitness.tracker.controller;

import com.fitness.tracker.dto.RunDTO;
import com.fitness.tracker.dto.RunDetailResponse;
import com.fitness.tracker.dto.RunSummaryResponse;
import com.fitness.tracker.service.RunService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @PostMapping
    public ResponseEntity<RunDetailResponse> save(@Valid @RequestBody RunDTO dto) {
        return ResponseEntity.ok(runService.saveRun(dto));
    }

    @GetMapping
    public ResponseEntity<List<RunSummaryResponse>> myRuns() {
        return ResponseEntity.ok(runService.getMyRuns());
    }

    @GetMapping("/{workoutId}")
    public ResponseEntity<RunDetailResponse> get(@PathVariable Long workoutId) {
        return ResponseEntity.ok(runService.getRun(workoutId));
    }
}
