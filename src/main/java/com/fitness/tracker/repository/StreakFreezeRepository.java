package com.fitness.tracker.repository;

import com.fitness.tracker.entity.StreakFreeze;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface StreakFreezeRepository extends JpaRepository<StreakFreeze, Long> {

    @Query("SELECT f.freezeDate FROM StreakFreeze f WHERE f.user.id = :userId")
    List<LocalDate> findDates(@Param("userId") Long userId);

    long countByUserIdAndUsedAtGreaterThanEqual(Long userId, LocalDateTime since);
}
