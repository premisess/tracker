package com.fitness.tracker.repository;

import com.fitness.tracker.entity.ExerciseSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ExerciseSetRepository extends JpaRepository<ExerciseSet, Long> {

    @Query("""
            SELECT s FROM ExerciseSet s
            JOIN FETCH s.workoutExercise we
            JOIN FETCH we.workout w
            JOIN FETCH we.exercise e
            WHERE w.user.id = :userId
            ORDER BY w.date, w.id, we.sortOrder, s.setNumber
            """)
    List<ExerciseSet> findAllForUser(@Param("userId") Long userId);

    @Query("""
            SELECT s FROM ExerciseSet s
            JOIN FETCH s.workoutExercise we
            JOIN FETCH we.workout w
            WHERE w.user.id = :userId AND we.exercise.id = :exerciseId
            ORDER BY w.date, w.id, we.sortOrder, s.setNumber
            """)
    List<ExerciseSet> findForUserAndExercise(@Param("userId") Long userId, @Param("exerciseId") Long exerciseId);

    @Query("""
            SELECT s FROM ExerciseSet s
            JOIN FETCH s.workoutExercise we
            JOIN FETCH we.workout w
            WHERE w.user.id = :userId AND we.exercise.id IN :exerciseIds AND w.id <> :workoutId
            """)
    List<ExerciseSet> findOtherSetsForExercises(@Param("userId") Long userId,
                                                @Param("exerciseIds") Collection<Long> exerciseIds,
                                                @Param("workoutId") Long workoutId);
}
