package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** A food with its nutrition per serving. Shared catalog foods have no owner; custom foods belong to one user. */
@Entity
@Table(name = "foods")
@Getter
@Setter
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    // What one serving is, e.g. "1 cup (158 g)" or "1 medium".
    @Column(name = "serving_label", nullable = false, length = 60)
    private String servingLabel;

    @Column(name = "serving_grams")
    private Double servingGrams;

    @Column(nullable = false)
    private Double calories;

    @Column(name = "protein_g", nullable = false)
    private Double proteinG;

    @Column(name = "carbs_g", nullable = false)
    private Double carbsG;

    @Column(name = "fat_g", nullable = false)
    private Double fatG;

    @Column(name = "fiber_g")
    private Double fiberG;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
