package com.fitness.tracker.controller;

import com.fitness.tracker.dto.WorkoutDTO;
import com.fitness.tracker.dto.WorkoutResponse;
import com.fitness.tracker.enums.WorkoutType;
import com.fitness.tracker.service.WorkoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @PostMapping
    public ResponseEntity<WorkoutResponse> addWorkout(@RequestBody WorkoutDTO dto) {
        return ResponseEntity.ok(workoutService.addWorkout(dto));
    }

    @GetMapping
    public ResponseEntity<List<WorkoutResponse>> getMyWorkouts() {
        return ResponseEntity.ok(workoutService.getMyWorkouts());
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkoutResponse> updateWorkout(@PathVariable Long id, @RequestBody WorkoutDTO dto) {
        return ResponseEntity.ok(workoutService.updateWorkout(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkout(@PathVariable Long id) {
        workoutService.deleteWorkout(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<WorkoutResponse>> searchWorkouts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String tag) {
        return ResponseEntity.ok(workoutService.searchWorkouts(type, startDate, endDate, tag));
    }

    @GetMapping("/types")
    public ResponseEntity<List<WorkoutType>> getWorkoutTypes() {
        return ResponseEntity.ok(workoutService.getWorkoutTypes());
    }
}

