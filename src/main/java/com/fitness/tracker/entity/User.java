package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    // Google-only accounts get a random, unguessable hash here; "Forgot password" lets them set a real one.
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

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    // SHA-256 of the emailed verification token; the token itself is never stored.
    @Column(name = "email_verification_token_hash", length = 64, unique = true)
    private String emailVerificationTokenHash;

    @Column(name = "email_verification_expires_at")
    private LocalDateTime emailVerificationExpiresAt;

    // How the account was created. A LOCAL account can still be linked to Google later.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "auth_provider", nullable = false, length = 10)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    // Google's stable account id ("sub"), set once the account has signed in with Google.
    @Column(name = "google_subject", length = 64, unique = true)
    private String googleSubject;

    // Paid FitTracker Ultimate access lasts until this moment; null or in the past means the free tier.
    @Column(name = "ultimate_until")
    private LocalDateTime ultimateUntil;

    public enum Role {
        USER, ADMIN
    }

    public enum AuthProvider {
        LOCAL, GOOGLE
    }

}
