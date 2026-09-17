package com.fitness.tracker.repository;

import com.fitness.tracker.entity.PlanEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanEnrollmentRepository extends JpaRepository<PlanEnrollment, Long> {

    Optional<PlanEnrollment> findFirstByUserIdAndStatus(Long userId, PlanEnrollment.Status status);

    long countByUserIdAndStatus(Long userId, PlanEnrollment.Status status);

    boolean existsByPlanId(Long planId);
}
