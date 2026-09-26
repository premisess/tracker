package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One night's sleep. {@code sleepDate} is the morning the user woke up, so each night has one entry. */
@Entity
@Table(name = "sleep_logs")
@Getter
@Setter
public class SleepLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sleep_date", nullable = false)
    private LocalDate sleepDate;

    @Column(name = "bed_time", nullable = false)
    private LocalDateTime bedTime;

    @Column(name = "wake_time", nullable = false)
    private LocalDateTime wakeTime;

    // 1 (poor) to 5 (great); optional.
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer quality;

    @Column(length = 280)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
