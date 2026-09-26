package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Something a user sent the team: feedback, a feature recommendation or a problem, and the team's reply. */
@Entity
@Table(name = "feedback")
@Getter
@Setter
public class Feedback {

    public enum Kind { FEEDBACK, RECOMMENDATION, PROBLEM }

    public enum Status { NEW, REVIEWED, PLANNED, DONE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    // 1 to 5 stars; optional.
    private Integer rating;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(length = 1000)
    private String reply;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
