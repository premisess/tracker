package com.fitness.tracker.dto;

/** A food and its nutrition for one serving. {@code custom} is true for foods the user added themselves. */
public record FoodResponse(
        Long id,
        String name,
        String servingLabel,
        Double servingGrams,
        double calories,
        double proteinG,
        double carbsG,
        double fatG,
        Double fiberG,
        boolean custom) {
}
