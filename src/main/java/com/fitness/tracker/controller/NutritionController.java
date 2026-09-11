package com.fitness.tracker.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/nutrition")
public class NutritionController {

    @GetMapping("/{goalType}")
    public ResponseEntity<Map<String, Object>> getNutrition(@PathVariable String goalType) {
        Map<String, Object> plan = new HashMap<>();

        switch (goalType.toUpperCase()) {
            case "LOSE_WEIGHT":
                plan.put("dailyCalories", 1800);
                plan.put("protein", "High");
                plan.put("carbs", "Low");
                plan.put("fats", "Moderate");
                plan.put("tip", "Eat smaller portions, avoid sugary drinks, drink 2.5L water daily");
                plan.put("breakfast", List.of(
                        Map.of("food", "Oatmeal with fruits", "calories", 300),
                        Map.of("food", "Boiled eggs (2)", "calories", 140),
                        Map.of("food", "Green tea", "calories", 5)
                ));
                plan.put("lunch", List.of(
                        Map.of("food", "Grilled chicken breast", "calories", 250),
                        Map.of("food", "Brown rice (small)", "calories", 200),
                        Map.of("food", "Mixed vegetables salad", "calories", 80)
                ));
                plan.put("dinner", List.of(
                        Map.of("food", "Grilled fish", "calories", 200),
                        Map.of("food", "Steamed broccoli", "calories", 55),
                        Map.of("food", "Lemon water", "calories", 10)
                ));
                plan.put("snacks", List.of(
                        Map.of("food", "Apple", "calories", 95),
                        Map.of("food", "Greek yogurt", "calories", 100),
                        Map.of("food", "Handful of almonds", "calories", 160)
                ));
                break;

            case "GAIN_WEIGHT":
                plan.put("dailyCalories", 3000);
                plan.put("protein", "Very High");
                plan.put("carbs", "High");
                plan.put("fats", "High");
                plan.put("tip", "Eat every 3 hours, add healthy calorie-dense foods, drink milk daily");
                plan.put("breakfast", List.of(
                        Map.of("food", "Scrambled eggs (4) with toast", "calories", 450),
                        Map.of("food", "Whole milk (1 glass)", "calories", 150),
                        Map.of("food", "Banana", "calories", 105)
                ));
                plan.put("lunch", List.of(
                        Map.of("food", "Rice with beef stew", "calories", 600),
                        Map.of("food", "Avocado", "calories", 200),
                        Map.of("food", "Fresh juice", "calories", 120)
                ));
                plan.put("dinner", List.of(
                        Map.of("food", "Pasta with chicken", "calories", 550),
                        Map.of("food", "Bread (2 slices)", "calories", 160),
                        Map.of("food", "Whole milk", "calories", 150)
                ));
                plan.put("snacks", List.of(
                        Map.of("food", "Peanut butter sandwich", "calories", 350),
                        Map.of("food", "Mixed nuts", "calories", 200),
                        Map.of("food", "Protein shake", "calories", 300)
                ));
                break;

            case "BUILD_STRENGTH":
                plan.put("dailyCalories", 2500);
                plan.put("protein", "Very High");
                plan.put("carbs", "Moderate");
                plan.put("fats", "Moderate");
                plan.put("tip", "Eat protein within 30 minutes after workout, stay hydrated, rest well");
                plan.put("breakfast", List.of(
                        Map.of("food", "Eggs (3) with whole wheat toast", "calories", 400),
                        Map.of("food", "Milk (1 glass)", "calories", 150),
                        Map.of("food", "Orange juice", "calories", 110)
                ));
                plan.put("lunch", List.of(
                        Map.of("food", "Grilled chicken with rice", "calories", 500),
                        Map.of("food", "Lentil soup", "calories", 200),
                        Map.of("food", "Water", "calories", 0)
                ));
                plan.put("dinner", List.of(
                        Map.of("food", "Beef or tuna with sweet potato", "calories", 450),
                        Map.of("food", "Steamed vegetables", "calories", 80),
                        Map.of("food", "Milk", "calories", 150)
                ));
                plan.put("snacks", List.of(
                        Map.of("food", "Protein shake", "calories", 300),
                        Map.of("food", "Boiled eggs (2)", "calories", 140),
                        Map.of("food", "Banana", "calories", 105)
                ));
                break;

            case "RUN_MORE":
                plan.put("dailyCalories", 2200);
                plan.put("protein", "Moderate");
                plan.put("carbs", "High");
                plan.put("fats", "Low");
                plan.put("tip", "Carb-load before long runs, eat banana before running, hydrate well");
                plan.put("breakfast", List.of(
                        Map.of("food", "Oatmeal with honey", "calories", 350),
                        Map.of("food", "Banana", "calories", 105),
                        Map.of("food", "Water (500ml)", "calories", 0)
                ));
                plan.put("lunch", List.of(
                        Map.of("food", "Pasta with tomato sauce", "calories", 400),
                        Map.of("food", "Chicken breast", "calories", 250),
                        Map.of("food", "Fresh salad", "calories", 80)
                ));
                plan.put("dinner", List.of(
                        Map.of("food", "Brown rice with fish", "calories", 400),
                        Map.of("food", "Steamed vegetables", "calories", 80),
                        Map.of("food", "Lemon water", "calories", 10)
                ));
                plan.put("snacks", List.of(
                        Map.of("food", "Energy bar", "calories", 200),
                        Map.of("food", "Apple with peanut butter", "calories", 200),
                        Map.of("food", "Sports drink", "calories", 150)
                ));
                break;

            case "STAY_ACTIVE":
                plan.put("dailyCalories", 2000);
                plan.put("protein", "Moderate");
                plan.put("carbs", "Moderate");
                plan.put("fats", "Moderate");
                plan.put("tip", "Eat balanced meals, stay hydrated, avoid processed foods");
                plan.put("breakfast", List.of(
                        Map.of("food", "Whole grain cereal with milk", "calories", 300),
                        Map.of("food", "Boiled egg", "calories", 70),
                        Map.of("food", "Orange juice", "calories", 110)
                ));
                plan.put("lunch", List.of(
                        Map.of("food", "Rice with beans and vegetables", "calories", 450),
                        Map.of("food", "Grilled chicken", "calories", 250),
                        Map.of("food", "Water", "calories", 0)
                ));
                plan.put("dinner", List.of(
                        Map.of("food", "Fish with ugali", "calories", 400),
                        Map.of("food", "Mixed greens salad", "calories", 80),
                        Map.of("food", "Water", "calories", 0)
                ));
                plan.put("snacks", List.of(
                        Map.of("food", "Fruit salad", "calories", 120),
                        Map.of("food", "Yogurt", "calories", 100),
                        Map.of("food", "Nuts", "calories", 160)
                ));
                break;

            default:
                plan.put("message", "No nutrition plan available for this goal type");
        }

        return ResponseEntity.ok(plan);
    }
}