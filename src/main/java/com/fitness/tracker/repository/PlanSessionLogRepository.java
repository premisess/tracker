package com.fitness.tracker.repository;

import com.fitness.tracker.entity.PlanSessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlanSessionLogRepository extends JpaRepository<PlanSessionLog, Long> {

    List<PlanSessionLog> findTop5ByEnrollmentIdOrderBySessionNumberDesc(Long enrollmentId);

    boolean existsByWorkoutId(Long workoutId);

    @Query("SELECT COUNT(l) FROM PlanSessionLog l WHERE l.enrollment.user.id = :userId AND l.skipped = false")
    long countCompletedForUser(@Param("userId") Long userId);
}
