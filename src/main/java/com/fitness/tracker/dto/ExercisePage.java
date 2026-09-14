package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExercisePage {
    private List<ExerciseResponse> items;
    private long total;
    private int page;
    private int size;
    private int totalPages;
}
