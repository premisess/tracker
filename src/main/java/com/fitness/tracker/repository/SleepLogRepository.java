package com.fitness.tracker.repository;

import com.fitness.tracker.entity.SleepLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SleepLogRepository extends JpaRepository<SleepLog, Long> {

    List<SleepLog> findByUserIdAndSleepDateGreaterThanEqualOrderBySleepDateDesc(Long userId, LocalDate from);

    Optional<SleepLog> findByUserIdAndSleepDate(Long userId, LocalDate sleepDate);

    Optional<SleepLog> findByIdAndUserId(Long id, Long userId);
}
