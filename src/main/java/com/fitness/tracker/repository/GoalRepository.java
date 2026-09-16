package com.fitness.tracker.repository;

import com.fitness.tracker.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, Goal.Status status);
}
