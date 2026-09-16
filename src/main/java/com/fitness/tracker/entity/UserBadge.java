package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** A badge a user has earned. What each badge means lives in BadgeDefinition. */
@Entity
@Table(name = "user_badges")
@Getter
@Setter
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "badge_code", nullable = false, length = 40)
    private String badgeCode;

    @Column(name = "earned_at", nullable = false)
    private LocalDateTime earnedAt;

    // Whether the website has already celebrated this badge with the user.
    @Column(nullable = false)
    private boolean notified;
}
