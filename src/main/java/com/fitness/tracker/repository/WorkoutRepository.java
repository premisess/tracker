package com.fitness.tracker.repository;

import com.fitness.tracker.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {
    List<Workout> findByUserIdOrderByDateDesc(Long userId);

    @Query("SELECT w FROM Workout w WHERE w.user.id = :userId AND (:type IS NULL OR w.type = :type) AND (:startDate IS NULL OR w.date >= :startDate) AND (:endDate IS NULL OR w.date <= :endDate) ORDER BY w.date DESC")
    List<Workout> searchWorkouts(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}