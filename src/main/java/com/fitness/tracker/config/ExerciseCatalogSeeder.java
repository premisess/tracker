package com.fitness.tracker.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fitness.tracker.entity.Exercise;
import com.fitness.tracker.entity.Exercise.TrackingType;
import com.fitness.tracker.repository.ExerciseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Fills the exercise library on first startup from data/exercises.json, a pinned copy of
 * free-exercise-db (github.com/yuhonas/free-exercise-db, public domain / Unlicense).
 * Does nothing once the table has rows.
 */
@Component
@Order(1)
public class ExerciseCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExerciseCatalogSeeder.class);

    private static final Set<String> BODYWEIGHT_EQUIPMENT =
            Set.of("body only", "bands", "exercise ball", "foam roll", "medicine ball");

    private final ExerciseRepository exerciseRepository;
    private final JsonMapper jsonMapper;

    public ExerciseCatalogSeeder(ExerciseRepository exerciseRepository, JsonMapper jsonMapper) {
        this.exerciseRepository = exerciseRepository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (exerciseRepository.count() > 0) {
            return;
        }
        List<CatalogEntry> entries;
        try (InputStream in = new ClassPathResource("data/exercises.json").getInputStream()) {
            entries = jsonMapper.readValue(in, new TypeReference<List<CatalogEntry>>() {});
        }
        exerciseRepository.saveAll(entries.stream().map(ExerciseCatalogSeeder::toEntity).toList());
        log.info("Seeded {} exercises into the library", entries.size());
    }

    private static Exercise toEntity(CatalogEntry entry) {
        Exercise e = new Exercise();
        e.setSlug(entry.id());
        e.setName(entry.name());
        e.setCategory(entry.category());
        e.setEquipment(entry.equipment());
        e.setLevel(entry.level());
        e.setForceType(entry.force());
        e.setMechanic(entry.mechanic());
        e.setPrimaryMuscles(join(entry.primaryMuscles(), ","));
        e.setSecondaryMuscles(join(entry.secondaryMuscles(), ","));
        e.setInstructions(join(entry.instructions(), "\n"));
        List<String> images = entry.images() != null ? entry.images() : List.of();
        e.setImageStart(images.size() > 0 ? images.get(0) : null);
        e.setImageEnd(images.size() > 1 ? images.get(1) : null);
        e.setTrackingType(trackingFor(entry));
        return e;
    }

    // Holds, stretches and cardio are timed; bodyweight moves count reps; everything else takes a load.
    static TrackingType trackingFor(CatalogEntry entry) {
        String category = lower(entry.category());
        String force = lower(entry.force());
        String equipment = lower(entry.equipment());
        if ("stretching".equals(category) || "cardio".equals(category) || "static".equals(force)) {
            return TrackingType.DURATION;
        }
        if (equipment == null || BODYWEIGHT_EQUIPMENT.contains(equipment)) {
            return TrackingType.REPS;
        }
        return TrackingType.WEIGHT_REPS;
    }

    private static String join(List<String> values, String separator) {
        return values == null || values.isEmpty() ? null : String.join(separator, values);
    }

    private static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogEntry(
            String id,
            String name,
            String force,
            String level,
            String mechanic,
            String equipment,
            String category,
            List<String> primaryMuscles,
            List<String> secondaryMuscles,
            List<String> instructions,
            List<String> images) {
    }
}
