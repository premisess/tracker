package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExerciseResponse {
    private Long id;
    private String slug;
    private String name;
    private String category;
    private String equipment;
    private String level;
    private String force;
    private String mechanic;
    private List<String> primaryMuscles;
    private List<String> secondaryMuscles;
    private List<String> instructions;
    // Start and end positions of the movement; the frontend alternates them as a demo.
    private List<String> imageUrls;
    private String trackingType;
}
