package com.fitness.tracker.repository;

import com.fitness.tracker.entity.Food;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food, Long> {

    // Catalog foods plus the user's own; names starting with the term rank first, then shorter names.
    // Patterns arrive already lower-cased, with LIKE wildcards escaped by "!".
    @Query("""
            SELECT f FROM Food f
            WHERE (f.owner IS NULL OR f.owner.id = :userId)
              AND LOWER(f.name) LIKE :contains ESCAPE '!'
            ORDER BY CASE WHEN LOWER(f.name) LIKE :startsWith ESCAPE '!' THEN 0 ELSE 1 END, LENGTH(f.name), f.name
            """)
    List<Food> search(@Param("userId") Long userId,
                      @Param("contains") String contains,
                      @Param("startsWith") String startsWith,
                      Pageable pageable);

    @Query("SELECT f FROM Food f WHERE f.id = :id AND (f.owner IS NULL OR f.owner.id = :userId)")
    Optional<Food> findVisible(@Param("id") Long id, @Param("userId") Long userId);

    List<Food> findByOwnerIdOrderByNameAsc(Long ownerId);

    long countByOwnerIsNull();
}
