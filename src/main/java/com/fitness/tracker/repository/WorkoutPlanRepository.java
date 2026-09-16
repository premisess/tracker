package com.fitness.tracker.repository;

import com.fitness.tracker.entity.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {

    Optional<WorkoutPlan> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<WorkoutPlan> findAllByOrderByIdAsc();
}
