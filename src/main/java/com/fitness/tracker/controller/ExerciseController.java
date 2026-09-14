package com.fitness.tracker.controller;

import com.fitness.tracker.dto.ExerciseFilters;
import com.fitness.tracker.dto.ExercisePage;
import com.fitness.tracker.dto.ExerciseResponse;
import com.fitness.tracker.service.ExerciseCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseCatalogService catalog;

    public ExerciseController(ExerciseCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public ResponseEntity<ExercisePage> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String muscle,
            @RequestParam(required = false) String equipment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return ResponseEntity.ok(catalog.search(q, category, muscle, equipment, page, size));
    }

    @GetMapping("/filters")
    public ResponseEntity<ExerciseFilters> filters() {
        return ResponseEntity.ok(catalog.filters());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExerciseResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(catalog.get(id));
    }
}
