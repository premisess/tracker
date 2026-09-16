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

    long countByUserId(Long userId);

    long countByUserIdAndSource(Long userId, Workout.ActivitySource source);

    @Query("SELECT COALESCE(MAX(w.distanceMeters), 0) FROM Workout w WHERE w.user.id = :userId AND w.distanceMeters IS NOT NULL AND w.type = :type")
    long longestGpsDistance(@Param("userId") Long userId, @Param("type") String type);

    @Query("SELECT COALESCE(SUM(w.distanceMeters), 0) FROM Workout w WHERE w.user.id = :userId AND w.distanceMeters IS NOT NULL")
    long totalGpsDistance(@Param("userId") Long userId);

    @Query("SELECT COUNT(w) FROM Workout w WHERE w.user.id = :userId AND SIZE(w.exercises) > 0")
    long countWithExercises(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(w.caloriesBurned), 0) FROM Workout w WHERE w.user.id = :userId AND w.date = :date")
    long caloriesBurnedOn(@Param("userId") Long userId, @Param("date") LocalDate date);

    // Rows of [date, calories burned].
    @Query("SELECT w.date, SUM(w.caloriesBurned) FROM Workout w WHERE w.user.id = :userId AND w.date BETWEEN :from AND :to GROUP BY w.date")
    List<Object[]> caloriesBurnedByDay(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
