package com.fitness.tracker.service;

import com.fitness.tracker.dto.ExerciseFilters;
import com.fitness.tracker.dto.ExercisePage;
import com.fitness.tracker.dto.ExerciseResponse;
import com.fitness.tracker.entity.Exercise;
import com.fitness.tracker.exception.NotFoundException;
import com.fitness.tracker.repository.ExerciseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class ExerciseCatalogService {

    private static final int MAX_PAGE_SIZE = 60;

    private final ExerciseRepository exerciseRepository;
    private final String imageBaseUrl;

    public ExerciseCatalogService(ExerciseRepository exerciseRepository,
                                  @Value("${app.exercise-images.base-url}") String imageBaseUrl) {
        this.exerciseRepository = exerciseRepository;
        this.imageBaseUrl = imageBaseUrl.endsWith("/") ? imageBaseUrl : imageBaseUrl + "/";
    }

    public ExercisePage search(String q, String category, String muscle, String equipment, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int safePage = Math.max(0, page);
        Page<Exercise> result = exerciseRepository.search(
                blankToNull(q), blankToNull(category), blankToNull(muscle), blankToNull(equipment),
                PageRequest.of(safePage, safeSize));
        return new ExercisePage(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getTotalElements(),
                safePage,
                safeSize,
                result.getTotalPages());
    }

    public ExerciseResponse get(Long id) {
        return toResponse(find(id));
    }

    public Exercise find(Long id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exercise not found"));
    }

    public ExerciseFilters filters() {
        TreeSet<String> muscles = new TreeSet<>();
        for (String list : exerciseRepository.findPrimaryMuscleLists()) {
            muscles.addAll(split(list, ","));
        }
        return new ExerciseFilters(
                exerciseRepository.findCategories(),
                List.copyOf(muscles),
                exerciseRepository.findEquipment());
    }

    public ExerciseResponse toResponse(Exercise e) {
        return new ExerciseResponse(
                e.getId(),
                e.getSlug(),
                e.getName(),
                e.getCategory(),
                e.getEquipment(),
                e.getLevel(),
                e.getForceType(),
                e.getMechanic(),
                split(e.getPrimaryMuscles(), ","),
                split(e.getSecondaryMuscles(), ","),
                split(e.getInstructions(), "\n"),
                Stream.of(e.getImageStart(), e.getImageEnd())
                        .filter(path -> path != null && !path.isBlank())
                        .map(path -> imageBaseUrl + path)
                        .toList(),
                e.getTrackingType().name());
    }

    /** First demo frame, used as a thumbnail next to logged sets and records. */
    public String thumbnailUrl(Exercise e) {
        return e.getImageStart() != null ? imageBaseUrl + e.getImageStart() : null;
    }

    private static List<String> split(String value, String separator) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(separator))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
