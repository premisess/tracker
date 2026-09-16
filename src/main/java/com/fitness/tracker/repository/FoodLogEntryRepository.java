package com.fitness.tracker.repository;

import com.fitness.tracker.entity.FoodLogEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FoodLogEntryRepository extends JpaRepository<FoodLogEntry, Long> {

    List<FoodLogEntry> findByUserIdAndLogDateOrderByCreatedAtAsc(Long userId, LocalDate logDate);

    @Query("SELECT e FROM FoodLogEntry e LEFT JOIN FETCH e.food WHERE e.user.id = :userId ORDER BY e.createdAt DESC")
    List<FoodLogEntry> findRecent(@Param("userId") Long userId, Pageable pageable);

    // Rows of [date, calories, protein, carbs, fat].
    @Query("""
            SELECT e.logDate, SUM(e.calories), SUM(e.proteinG), SUM(e.carbsG), SUM(e.fatG)
            FROM FoodLogEntry e
            WHERE e.user.id = :userId AND e.logDate BETWEEN :from AND :to
            GROUP BY e.logDate
            """)
    List<Object[]> dailyTotals(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(e.calories), 0) FROM FoodLogEntry e WHERE e.user.id = :userId AND e.logDate = :date")
    double caloriesOn(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(DISTINCT e.logDate) FROM FoodLogEntry e WHERE e.user.id = :userId")
    long countLoggedDays(@Param("userId") Long userId);
}
