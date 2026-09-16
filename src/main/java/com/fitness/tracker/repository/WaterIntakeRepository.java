package com.fitness.tracker.repository;

import com.fitness.tracker.entity.WaterIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WaterIntakeRepository extends JpaRepository<WaterIntake, Long> {
    List<WaterIntake> findByUserIdOrderByDateDesc(Long userId);
    Optional<WaterIntake> findByUserIdAndDate(Long userId, LocalDate date);

    @Query("SELECT COALESCE(SUM(w.amountMl), 0) FROM WaterIntake w WHERE w.user.id = :userId AND w.date = :date")
    long totalForDay(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(DISTINCT w.date) FROM WaterIntake w WHERE w.user.id = :userId")
    long countLoggedDays(@Param("userId") Long userId);
}
