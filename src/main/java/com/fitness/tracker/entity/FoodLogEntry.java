package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One line in the food diary. The name and nutrition are copied from the food when it's logged
 * (already multiplied by the servings), so later edits to that food don't change past days.
 */
@Entity
@Table(name = "food_log_entries")
@Getter
@Setter
public class FoodLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Meal meal;

    // Null for one-off entries, or when the food has since been deleted.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id")
    private Food food;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "serving_label", nullable = false, length = 60)
    private String servingLabel;

    @Column(nullable = false)
    private Double servings;

    @Column(nullable = false)
    private Double calories;

    @Column(name = "protein_g", nullable = false)
    private Double proteinG;

    @Column(name = "carbs_g", nullable = false)
    private Double carbsG;

    @Column(name = "fat_g", nullable = false)
    private Double fatG;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum Meal {
        BREAKFAST, LUNCH, DINNER, SNACK
    }
}
