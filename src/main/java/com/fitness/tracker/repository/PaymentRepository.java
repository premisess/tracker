package com.fitness.tracker.repository;

import com.fitness.tracker.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderReference(String orderReference);

    boolean existsByUserIdAndStatusAndCreatedAtAfter(Long userId, Payment.Status status, LocalDateTime after);

    List<Payment> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
