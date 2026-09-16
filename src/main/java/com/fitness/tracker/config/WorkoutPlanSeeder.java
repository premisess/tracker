package com.fitness.tracker.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fitness.tracker.entity.Exercise;
import com.fitness.tracker.entity.Goal;
import com.fitness.tracker.entity.PlanDay;
import com.fitness.tracker.entity.PlanDayExercise;
import com.fitness.tracker.entity.WorkoutPlan;
import com.fitness.tracker.enums.WorkoutType;
import com.fitness.tracker.repository.ExerciseRepository;
import com.fitness.tracker.repository.WorkoutPlanRepository;
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
import java.util.Optional;

/**
 * Adds the built-in workout plans from data/plans.json. Runs after the exercise library is seeded and
 * inserts any plan whose slug isn't in the database yet, so new plans can ship without touching old ones.
 */
@Component
@Order(2)
public class WorkoutPlanSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkoutPlanSeeder.class);

    private final WorkoutPlanRepository planRepository;
    private final ExerciseRepository exerciseRepository;
    private final JsonMapper jsonMapper;

    public WorkoutPlanSeeder(WorkoutPlanRepository planRepository, ExerciseRepository exerciseRepository,
                             JsonMapper jsonMapper) {
        this.planRepository = planRepository;
        this.exerciseRepository = exerciseRepository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        List<PlanSeed> seeds;
        try (InputStream in = new ClassPathResource("data/plans.json").getInputStream()) {
            seeds = jsonMapper.readValue(in, new TypeReference<List<PlanSeed>>() {});
        }
        int added = 0;
        for (PlanSeed seed : seeds) {
            if (planRepository.existsBySlug(seed.slug())) {
                continue;
            }
            Optional<WorkoutPlan> plan = build(seed);
            if (plan.isPresent()) {
                planRepository.save(plan.get());
                added++;
            }
        }
        if (added > 0) {
            log.info("Added {} workout plans", added);
        }
    }

    private Optional<WorkoutPlan> build(PlanSeed seed) {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setSlug(seed.slug());
        plan.setName(seed.name());
        plan.setSummary(seed.summary());
        plan.setDescription(seed.description());
        plan.setGoal(Goal.GoalType.valueOf(seed.goal()));
        plan.setLevel(seed.level());
        plan.setEquipment(seed.equipment());
        plan.setDurationWeeks(seed.weeks());
        plan.setDaysPerWeek(seed.daysPerWeek());

        for (DaySeed d : seed.days() != null ? seed.days() : List.<DaySeed>of()) {
            PlanDay day = newDay(plan, null, d.day(), d.title(), d.focus(), d.activity(), d.workoutType(),
                    d.minutes(), d.distance(), d.instructions());
            int order = 0;
            for (ExerciseSeed e : d.exercises() != null ? d.exercises() : List.<ExerciseSeed>of()) {
                Optional<Exercise> exercise = exerciseRepository.findBySlug(e.slug());
                if (exercise.isEmpty()) {
                    log.warn("Skipping plan {}: exercise {} isn't in the library", seed.slug(), e.slug());
                    return Optional.empty();
                }
                PlanDayExercise pe = new PlanDayExercise();
                pe.setPlanDay(day);
                pe.setExercise(exercise.get());
                pe.setSortOrder(order++);
                pe.setSets(e.sets());
                pe.setReps(e.reps());
                pe.setRestSec(e.rest());
                day.getExercises().add(pe);
            }
            plan.getDays().add(day);
        }

        // Progressive running plans: the same session on each training day of a week, changing week by week.
        for (WeekSeed w : seed.runWeeks() != null ? seed.runWeeks() : List.<WeekSeed>of()) {
            for (int dayNumber = 1; dayNumber <= seed.daysPerWeek(); dayNumber++) {
                plan.getDays().add(newDay(plan, w.week(), dayNumber, w.title(), "Week " + w.week() + ", run " + dayNumber,
                        "RUN", "Running", w.minutes(), w.distance(), w.instructions()));
            }
        }

        for (PlanDay day : plan.getDays()) {
            if (WorkoutType.fromLabel(day.getWorkoutType()).isEmpty()) {
                log.warn("Skipping plan {}: unknown workout type {}", seed.slug(), day.getWorkoutType());
                return Optional.empty();
            }
        }
        return Optional.of(plan);
    }

    private static PlanDay newDay(WorkoutPlan plan, Integer week, int dayNumber, String title, String focus,
                                  String activity, String workoutType, int minutes, Integer distance, String instructions) {
        PlanDay day = new PlanDay();
        day.setPlan(plan);
        day.setWeekNumber(week);
        day.setDayNumber(dayNumber);
        day.setTitle(title);
        day.setFocus(focus);
        day.setActivity(PlanDay.Activity.valueOf(activity));
        day.setWorkoutType(workoutType);
        day.setTargetMinutes(minutes);
        day.setTargetDistanceM(distance);
        day.setInstructions(instructions);
        return day;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlanSeed(String slug, String name, String summary, String description, String goal, String level,
                    String equipment, int weeks, int daysPerWeek, List<DaySeed> days, List<WeekSeed> runWeeks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DaySeed(int day, String title, String focus, String activity, String workoutType, int minutes,
                   Integer distance, String instructions, List<ExerciseSeed> exercises) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExerciseSeed(String slug, int sets, String reps, int rest) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WeekSeed(int week, String title, int minutes, Integer distance, String instructions) {
    }
}
