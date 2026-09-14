package com.fitness.tracker.repository;

import com.fitness.tracker.entity.WorkoutRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkoutRouteRepository extends JpaRepository<WorkoutRoute, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM WorkoutRoute r WHERE r.workout.id IN (SELECT w.id FROM Workout w WHERE w.user.id = :userId)")
    int deleteAllForUser(@Param("userId") Long userId);
}
