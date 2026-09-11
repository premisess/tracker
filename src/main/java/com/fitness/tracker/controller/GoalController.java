package com.fitness.tracker.controller;

import com.fitness.tracker.dto.GoalDTO;
import com.fitness.tracker.dto.GoalResponse;
import com.fitness.tracker.service.GoalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> addGoal(@RequestBody GoalDTO dto) {
        return ResponseEntity.ok(goalService.addGoal(dto));
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getMyGoals() {
        return ResponseEntity.ok(goalService.getMyGoals());
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(@PathVariable Long id, @RequestBody GoalDTO dto) {
        return ResponseEntity.ok(goalService.updateGoal(id, dto));
    }

    @PatchMapping("/{id}/progress")
    public ResponseEntity<GoalResponse> updateProgress(@PathVariable Long id, @RequestBody Map<String, Double> body) {
        return ResponseEntity.ok(goalService.updateProgress(id, body.get("progress")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }
}