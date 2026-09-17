package com.fitness.tracker.repository;

import com.fitness.tracker.entity.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {

    Optional<WorkoutPlan> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Built-in plans first, then the user's own plans, newest last.
    @Query("""
            SELECT p FROM WorkoutPlan p
            WHERE p.owner IS NULL OR p.owner.id = :userId
            ORDER BY CASE WHEN p.owner IS NULL THEN 0 ELSE 1 END, p.id
            """)
    List<WorkoutPlan> findVisibleTo(@Param("userId") Long userId);
}
