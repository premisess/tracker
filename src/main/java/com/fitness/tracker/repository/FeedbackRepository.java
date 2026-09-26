package com.fitness.tracker.repository;

import com.fitness.tracker.entity.Feedback;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT f FROM Feedback f JOIN FETCH f.user ORDER BY f.createdAt DESC")
    List<Feedback> findAllWithUser(Pageable page);

    long countByStatus(Feedback.Status status);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);
}
