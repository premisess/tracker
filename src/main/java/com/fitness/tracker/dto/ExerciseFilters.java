package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** The values the library's filter chips can offer, taken from what's actually in the catalog. */
@Data
@AllArgsConstructor
public class ExerciseFilters {
    private List<String> categories;
    private List<String> muscles;
    private List<String> equipment;
}
