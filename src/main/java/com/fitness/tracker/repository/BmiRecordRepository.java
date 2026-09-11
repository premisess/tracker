package com.fitness.tracker.repository;

import com.fitness.tracker.entity.BmiRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BmiRecordRepository extends JpaRepository<BmiRecord, Long> {
    List<BmiRecord> findByUserIdOrderByDateDesc(Long userId);
}