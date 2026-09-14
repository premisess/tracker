package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String resetToken;

    private LocalDateTime resetTokenExpiry;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    // Set explicitly in AuthService.register() rather than via a generation annotation,
    // since Hibernate's @CreationTimestamp forces NOT NULL DDL that breaks the ALTER TABLE
    // against existing rows.
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // When the user agreed to GPS route recording. Null means no consent, and no routes are accepted.
    @Column(name = "location_consent_at")
    private LocalDateTime locationConsentAt;

    // Radius around a route's start and finish that is hidden whenever the route is shared.
    @Column(name = "route_privacy_meters", nullable = false)
    private Integer routePrivacyMeters = 200;

    public enum Role {
        USER, ADMIN
    }

}