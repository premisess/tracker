package com.fitness.tracker.controller;

import com.fitness.tracker.dto.FoodRequest;
import com.fitness.tracker.dto.FoodResponse;
import com.fitness.tracker.service.NutritionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final NutritionService nutritionService;

    public FoodController(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    @GetMapping
    public ResponseEntity<List<FoodResponse>> search(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(nutritionService.searchFoods(q));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<FoodResponse>> recent() {
        return ResponseEntity.ok(nutritionService.recentFoods());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<FoodResponse>> mine() {
        return ResponseEntity.ok(nutritionService.myFoods());
    }

    @PostMapping
    public ResponseEntity<FoodResponse> create(@Valid @RequestBody FoodRequest request) {
        return ResponseEntity.ok(nutritionService.createFood(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        nutritionService.deleteFood(id);
        return ResponseEntity.noContent().build();
    }
}
