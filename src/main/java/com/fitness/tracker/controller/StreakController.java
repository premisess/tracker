package com.fitness.tracker.controller;

import com.fitness.tracker.dto.StreakDtos.FreezeRequest;
import com.fitness.tracker.dto.StreakDtos.StreakStatus;
import com.fitness.tracker.service.StreakService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streak")
public class StreakController {

    private final StreakService streakService;

    public StreakController(StreakService streakService) {
        this.streakService = streakService;
    }

    @GetMapping
    public ResponseEntity<StreakStatus> getStreak() {
        return ResponseEntity.ok(streakService.status());
    }

    @PostMapping("/freeze")
    public ResponseEntity<StreakStatus> freeze(@Valid @RequestBody FreezeRequest request) {
        return ResponseEntity.ok(streakService.freeze(request.getDate()));
    }
}
