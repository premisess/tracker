package com.fitness.tracker.repository;

import com.fitness.tracker.entity.WaterIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WaterIntakeRepository extends JpaRepository<WaterIntake, Long> {
    List<WaterIntake> findByUserIdOrderByDateDesc(Long userId);
    Optional<WaterIntake> findByUserIdAndDate(Long userId, LocalDate date);
}