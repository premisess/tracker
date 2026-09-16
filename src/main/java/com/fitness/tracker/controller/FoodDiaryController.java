package com.fitness.tracker.controller;

import com.fitness.tracker.dto.FoodDiaryResponse;
import com.fitness.tracker.dto.FoodLogEntryResponse;
import com.fitness.tracker.dto.FoodLogRequest;
import com.fitness.tracker.dto.FoodLogUpdateRequest;
import com.fitness.tracker.dto.NutritionHistoryResponse;
import com.fitness.tracker.dto.NutritionTargets;
import com.fitness.tracker.service.NutritionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** The food diary. The older meal-idea endpoint, GET /api/nutrition/{goalType}, lives in NutritionController. */
@RestController
@RequestMapping("/api/nutrition")
public class FoodDiaryController {

    private final NutritionService nutritionService;

    public FoodDiaryController(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    @GetMapping("/diary")
    public ResponseEntity<FoodDiaryResponse> diary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(nutritionService.diary(date));
    }

    @PostMapping("/diary")
    public ResponseEntity<FoodLogEntryResponse> addEntry(@Valid @RequestBody FoodLogRequest request) {
        return ResponseEntity.ok(nutritionService.addEntry(request));
    }

    @PatchMapping("/diary/{id}")
    public ResponseEntity<FoodLogEntryResponse> updateEntry(@PathVariable Long id,
                                                            @Valid @RequestBody FoodLogUpdateRequest request) {
        return ResponseEntity.ok(nutritionService.updateEntry(id, request));
    }

    @DeleteMapping("/diary/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        nutritionService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/targets")
    public ResponseEntity<NutritionTargets> targets() {
        return ResponseEntity.ok(nutritionService.targets());
    }

    @GetMapping("/history")
    public ResponseEntity<NutritionHistoryResponse> history(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(nutritionService.history(days));
    }
}
