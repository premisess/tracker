package com.fitness.tracker.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String WGER_BASE_URL = "https://wger.de/api/v2";

    @GetMapping
    public ResponseEntity<?> getExercises(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "2") int language) {
        String url = WGER_BASE_URL + "/exercise/?format=json&language=" + language + "&limit=" + limit + "&offset=" + ((page - 1) * limit);
        ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/category")
    public ResponseEntity<?> getCategories() {
        String url = WGER_BASE_URL + "/exercisecategory/?format=json";
        ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchExercises(@RequestParam String term) {
        String url = WGER_BASE_URL + "/exercise/search/?term=" + term + "&language=english&format=json";
        ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getExerciseDetail(@PathVariable int id) {
        String url = WGER_BASE_URL + "/exerciseinfo/" + id + "/?format=json";
        ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
        return ResponseEntity.ok(response.getBody());
    }


}