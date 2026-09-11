package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "bmi_records")
@Data
public class BmiRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double weight;

    private Double height;

    private Double bmiValue;

    private String category;

    private LocalDate date;
}