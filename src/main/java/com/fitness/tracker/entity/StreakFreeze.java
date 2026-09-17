package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A missed day covered by a streak freeze: it doesn't add to the streak, but doesn't break it either. */
@Entity
@Table(name = "streak_freezes")
@Getter
@Setter
public class StreakFreeze {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "freeze_date", nullable = false)
    private LocalDate freezeDate;

    // Freezes are rationed per calendar month of use.
    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;
}
