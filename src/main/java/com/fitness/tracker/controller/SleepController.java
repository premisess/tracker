package com.fitness.tracker.controller;

import com.fitness.tracker.dto.SleepDtos.SleepEntry;
import com.fitness.tracker.dto.SleepDtos.SleepRequest;
import com.fitness.tracker.dto.SleepDtos.SleepSummary;
import com.fitness.tracker.service.SleepService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sleep")
public class SleepController {

    private final SleepService sleepService;

    public SleepController(SleepService sleepService) {
        this.sleepService = sleepService;
    }

    @GetMapping
    public ResponseEntity<SleepSummary> summary(@RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(sleepService.summary(days));
    }

    @PostMapping
    public ResponseEntity<SleepEntry> log(@Valid @RequestBody SleepRequest request) {
        return ResponseEntity.ok(sleepService.log(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sleepService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
