package com.fitness.tracker.controller;

import com.fitness.tracker.dto.ExerciseHistoryResponse;
import com.fitness.tracker.dto.ExerciseRecordSummary;
import com.fitness.tracker.service.PersonalRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
public class RecordsController {

    private final PersonalRecordService personalRecordService;

    public RecordsController(PersonalRecordService personalRecordService) {
        this.personalRecordService = personalRecordService;
    }

    @GetMapping
    public ResponseEntity<List<ExerciseRecordSummary>> myRecords() {
        return ResponseEntity.ok(personalRecordService.myRecords());
    }

    @GetMapping("/{exerciseId}")
    public ResponseEntity<ExerciseHistoryResponse> history(@PathVariable Long exerciseId) {
        return ResponseEntity.ok(personalRecordService.history(exerciseId));
    }
}
