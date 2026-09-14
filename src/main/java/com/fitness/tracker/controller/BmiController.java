package com.fitness.tracker.controller;

import jakarta.validation.Valid;
import com.fitness.tracker.dto.BmiDTO;
import com.fitness.tracker.dto.BmiResponse;
import com.fitness.tracker.service.BmiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bmi")
public class BmiController {

    private final BmiService bmiService;

    public BmiController(BmiService bmiService) {
        this.bmiService = bmiService;
    }

    @PostMapping
    public ResponseEntity<BmiResponse> calculate(@Valid @RequestBody BmiDTO dto) {
        return ResponseEntity.ok(bmiService.calculateAndSave(dto));
    }

    @GetMapping
    public ResponseEntity<List<BmiResponse>> getHistory() {
        return ResponseEntity.ok(bmiService.getMyBmiHistory());
    }
}