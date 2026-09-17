package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** A mobile-money payment for FitTracker Ultimate. */
@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_reference", nullable = false, unique = true, length = 20)
    private String orderReference;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Plan plan;

    @Column(name = "amount_tzs", nullable = false)
    private Integer amountTzs;

    // International format without "+", e.g. 255712345678.
    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    // The network ClickPesa used, e.g. "M-PESA" or "TIGO-PESA".
    @Column(length = 40)
    private String channel;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(name = "provider_transaction_id", length = 80)
    private String providerTransactionId;

    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum Plan {
        MONTHLY, YEARLY
    }

    public enum Status {
        PENDING, SUCCESS, FAILED
    }
}
