package com.fitness.tracker.repository;

import com.fitness.tracker.entity.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    // Names that start with the search term rank above names that merely contain it.
    @Query(value = """
            SELECT e FROM Exercise e
            WHERE (:q IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:category IS NULL OR e.category = :category)
              AND (:muscle IS NULL OR e.primaryMuscles LIKE CONCAT('%', :muscle, '%'))
              AND (:equipment IS NULL OR e.equipment = :equipment)
            ORDER BY CASE WHEN LOWER(e.name) LIKE LOWER(CONCAT(:q, '%')) THEN 0 ELSE 1 END, e.name
            """,
            countQuery = """
            SELECT COUNT(e) FROM Exercise e
            WHERE (:q IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:category IS NULL OR e.category = :category)
              AND (:muscle IS NULL OR e.primaryMuscles LIKE CONCAT('%', :muscle, '%'))
              AND (:equipment IS NULL OR e.equipment = :equipment)
            """)
    Page<Exercise> search(@Param("q") String q,
                          @Param("category") String category,
                          @Param("muscle") String muscle,
                          @Param("equipment") String equipment,
                          Pageable pageable);

    @Query("SELECT DISTINCT e.category FROM Exercise e ORDER BY e.category")
    List<String> findCategories();

    @Query("SELECT DISTINCT e.equipment FROM Exercise e WHERE e.equipment IS NOT NULL ORDER BY e.equipment")
    List<String> findEquipment();

    @Query("SELECT e.primaryMuscles FROM Exercise e WHERE e.primaryMuscles IS NOT NULL")
    List<String> findPrimaryMuscleLists();
}
