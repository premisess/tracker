package com.fitness.tracker.repository;

import com.fitness.tracker.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findByUserId(Long userId);

    List<UserBadge> findByUserIdAndNotifiedFalseOrderByEarnedAtAsc(Long userId);

    long countByUserId(Long userId);
}
