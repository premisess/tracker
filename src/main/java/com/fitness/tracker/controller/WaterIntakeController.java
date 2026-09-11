package com.fitness.tracker.controller;

import com.fitness.tracker.dto.WaterIntakeDTO;
import com.fitness.tracker.dto.WaterIntakeResponse;
import com.fitness.tracker.service.WaterIntakeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/water-intake")
public class WaterIntakeController {

    private final WaterIntakeService waterIntakeService;

    public WaterIntakeController(WaterIntakeService waterIntakeService) {
        this.waterIntakeService = waterIntakeService;
    }

    @PostMapping
    public ResponseEntity<WaterIntakeResponse> logIntake(@RequestBody WaterIntakeDTO dto) {
        return ResponseEntity.ok(waterIntakeService.logIntake(dto));
    }

    @GetMapping
    public ResponseEntity<List<WaterIntakeResponse>> getMyIntake() {
        return ResponseEntity.ok(waterIntakeService.getMyIntake());
    }
}