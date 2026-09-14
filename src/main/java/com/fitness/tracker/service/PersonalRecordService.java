package com.fitness.tracker.service;

import com.fitness.tracker.dto.ExerciseHistoryResponse;
import com.fitness.tracker.dto.ExerciseRecordSummary;
import com.fitness.tracker.dto.ExerciseSessionResponse;
import com.fitness.tracker.dto.ExerciseSetResponse;
import com.fitness.tracker.dto.PersonalRecordHit;
import com.fitness.tracker.entity.Exercise;
import com.fitness.tracker.entity.ExerciseSet;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.entity.Workout;
import com.fitness.tracker.entity.WorkoutExercise;
import com.fitness.tracker.repository.ExerciseSetRepository;
import com.fitness.tracker.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All-time bests per exercise, per-session history, and spotting new records as workouts are saved. */
@Service
@Transactional(readOnly = true)
public class PersonalRecordService {

    // Ignore floating-point noise when deciding whether a lift beat the old record.
    private static final double EPSILON = 0.05;

    private final ExerciseSetRepository exerciseSetRepository;
    private final ExerciseCatalogService exerciseCatalogService;
    private final CurrentUserService currentUserService;

    public PersonalRecordService(ExerciseSetRepository exerciseSetRepository,
                                 ExerciseCatalogService exerciseCatalogService,
                                 CurrentUserService currentUserService) {
        this.exerciseSetRepository = exerciseSetRepository;
        this.exerciseCatalogService = exerciseCatalogService;
        this.currentUserService = currentUserService;
    }

    /** Every exercise the user has logged, most recently performed first. */
    public List<ExerciseRecordSummary> myRecords() {
        User user = currentUserService.get();
        Map<Long, List<ExerciseSet>> byExercise = new LinkedHashMap<>();
        for (ExerciseSet set : exerciseSetRepository.findAllForUser(user.getId())) {
            byExercise.computeIfAbsent(exerciseOf(set).getId(), id -> new ArrayList<>()).add(set);
        }
        return byExercise.values().stream()
                .map(sets -> summarize(exerciseOf(sets.get(0)), sets))
                .sorted(Comparator.comparing(ExerciseRecordSummary::getLastPerformed,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public ExerciseHistoryResponse history(Long exerciseId) {
        User user = currentUserService.get();
        Exercise exercise = exerciseCatalogService.find(exerciseId);
        List<ExerciseSet> sets = exerciseSetRepository.findForUserAndExercise(user.getId(), exerciseId);

        Map<Long, List<ExerciseSet>> byWorkout = new LinkedHashMap<>();
        for (ExerciseSet set : sets) {
            byWorkout.computeIfAbsent(workoutOf(set).getId(), id -> new ArrayList<>()).add(set);
        }
        List<ExerciseSessionResponse> sessions = byWorkout.values().stream()
                .map(this::toSession)
                .toList();

        return new ExerciseHistoryResponse(
                exerciseCatalogService.toResponse(exercise),
                sets.isEmpty() ? null : summarize(exercise, sets),
                sessions);
    }

    /**
     * Records this workout beat compared with every other workout the user has logged.
     * An exercise done for the first time sets a baseline, not a record.
     */
    public List<PersonalRecordHit> newRecordsIn(Workout workout) {
        if (workout.getExercises().isEmpty()) {
            return List.of();
        }
        Map<Long, List<ExerciseSet>> current = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        for (WorkoutExercise we : workout.getExercises()) {
            current.computeIfAbsent(we.getExercise().getId(), id -> new ArrayList<>()).addAll(we.getSets());
            names.put(we.getExercise().getId(), we.getExercise().getName());
        }

        Map<Long, List<ExerciseSet>> previous = new LinkedHashMap<>();
        for (ExerciseSet set : exerciseSetRepository.findOtherSetsForExercises(
                workout.getUser().getId(), current.keySet(), workout.getId())) {
            previous.computeIfAbsent(exerciseOf(set).getId(), id -> new ArrayList<>()).add(set);
        }

        List<PersonalRecordHit> hits = new ArrayList<>();
        for (Map.Entry<Long, List<ExerciseSet>> entry : current.entrySet()) {
            List<ExerciseSet> before = previous.get(entry.getKey());
            if (before == null || before.isEmpty()) {
                continue;
            }
            Bests now = Bests.of(entry.getValue());
            Bests then = Bests.of(before);
            Long id = entry.getKey();
            String name = names.get(id);

            if (beats(now.e1rm, then.e1rm)) {
                hits.add(new PersonalRecordHit(id, name, "E1RM", now.e1rm.value(), valueOf(then.e1rm),
                        "Estimated 1RM " + kg(now.e1rm.value()) + " (" + describe(now.e1rm.set()) + ")"));
            }
            if (beats(now.heaviest, then.heaviest)) {
                hits.add(new PersonalRecordHit(id, name, "HEAVIEST", now.heaviest.value(), valueOf(then.heaviest),
                        "Heaviest set: " + describe(now.heaviest.set())));
            }
            if (beats(now.maxReps, then.maxReps)) {
                hits.add(new PersonalRecordHit(id, name, "MOST_REPS", now.maxReps.value(), valueOf(then.maxReps),
                        "Most reps in a set: " + (int) now.maxReps.value()));
            }
            if (beats(now.longest, then.longest)) {
                hits.add(new PersonalRecordHit(id, name, "LONGEST", now.longest.value(), valueOf(then.longest),
                        "Longest set: " + clock((int) now.longest.value())));
            }
        }
        return hits;
    }

    public static ExerciseSetResponse toSetResponse(ExerciseSet set) {
        return new ExerciseSetResponse(set.getSetNumber(), set.getReps(), set.getWeightKg(),
                set.getDurationSec(), set.getRpe(), set.isWarmup());
    }

    private ExerciseRecordSummary summarize(Exercise exercise, List<ExerciseSet> sets) {
        Bests bests = Bests.of(sets);
        ExerciseRecordSummary s = new ExerciseRecordSummary();
        s.setExerciseId(exercise.getId());
        s.setExerciseName(exercise.getName());
        s.setCategory(exercise.getCategory());
        s.setThumbnailUrl(exerciseCatalogService.thumbnailUrl(exercise));
        s.setTrackingType(exercise.getTrackingType().name());
        if (bests.e1rm != null) {
            s.setBestE1rmKg(bests.e1rm.value());
            s.setBestE1rmWeightKg(bests.e1rm.set().getWeightKg());
            s.setBestE1rmReps(bests.e1rm.set().getReps());
            s.setBestE1rmDate(dateOf(bests.e1rm.set()));
        }
        if (bests.heaviest != null) {
            s.setHeaviestKg(bests.heaviest.value());
            s.setHeaviestReps(bests.heaviest.set().getReps());
            s.setHeaviestDate(dateOf(bests.heaviest.set()));
        }
        if (bests.maxReps != null) {
            s.setMaxReps((int) bests.maxReps.value());
            s.setMaxRepsDate(dateOf(bests.maxReps.set()));
        }
        if (bests.longest != null) {
            s.setLongestSec((int) bests.longest.value());
            s.setLongestDate(dateOf(bests.longest.set()));
        }
        s.setTotalSets(bests.workingSets);
        s.setTotalVolumeKg(StrengthMath.volume(sets));
        s.setSessions((int) sets.stream().map(set -> workoutOf(set).getId()).distinct().count());
        s.setLastPerformed(sets.stream().map(PersonalRecordService::dateOf)
                .filter(d -> d != null).max(Comparator.naturalOrder()).orElse(null));
        return s;
    }

    private ExerciseSessionResponse toSession(List<ExerciseSet> sets) {
        Bests bests = Bests.of(sets);
        Workout workout = workoutOf(sets.get(0));
        return new ExerciseSessionResponse(
                workout.getId(),
                workout.getDate(),
                sets.stream().map(PersonalRecordService::toSetResponse).toList(),
                valueOf(bests.e1rm),
                valueOf(bests.heaviest),
                bests.maxReps != null ? (int) bests.maxReps.value() : null,
                bests.longest != null ? (int) bests.longest.value() : null,
                StrengthMath.volume(sets));
    }

    private static boolean beats(SetValue now, SetValue then) {
        return now != null && (then == null || now.value() > then.value() + EPSILON);
    }

    private static Double valueOf(SetValue v) {
        return v == null ? null : v.value();
    }

    private static Exercise exerciseOf(ExerciseSet set) {
        return set.getWorkoutExercise().getExercise();
    }

    private static Workout workoutOf(ExerciseSet set) {
        return set.getWorkoutExercise().getWorkout();
    }

    private static LocalDate dateOf(ExerciseSet set) {
        return workoutOf(set).getDate();
    }

    private static String describe(ExerciseSet set) {
        if (set.getWeightKg() != null && set.getWeightKg() > 0 && set.getReps() != null) {
            return kg(set.getWeightKg()) + " × " + set.getReps();
        }
        if (set.getReps() != null) {
            return set.getReps() + " reps";
        }
        return set.getDurationSec() != null ? clock(set.getDurationSec()) : "";
    }

    private static String kg(double value) {
        return (value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(StrengthMath.round1(value))) + " kg";
    }

    private static String clock(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private record SetValue(double value, ExerciseSet set) {
    }

    /** The best working set on each measure within a group of sets. */
    private static final class Bests {
        SetValue e1rm;
        SetValue heaviest;
        SetValue maxReps;
        SetValue longest;
        int workingSets;

        static Bests of(List<ExerciseSet> sets) {
            Bests b = new Bests();
            for (ExerciseSet set : sets) {
                if (set.isWarmup()) {
                    continue;
                }
                b.workingSets++;
                Double estimate = StrengthMath.estimatedOneRepMax(set.getWeightKg(), set.getReps());
                if (estimate != null && (b.e1rm == null || estimate > b.e1rm.value())) {
                    b.e1rm = new SetValue(estimate, set);
                }
                if (set.getWeightKg() != null && set.getWeightKg() > 0 && set.getReps() != null && set.getReps() > 0
                        && (b.heaviest == null || set.getWeightKg() > b.heaviest.value())) {
                    b.heaviest = new SetValue(set.getWeightKg(), set);
                }
                if (set.getReps() != null && set.getReps() > 0
                        && (b.maxReps == null || set.getReps() > b.maxReps.value())) {
                    b.maxReps = new SetValue(set.getReps(), set);
                }
                if (set.getDurationSec() != null && set.getDurationSec() > 0
                        && (b.longest == null || set.getDurationSec() > b.longest.value())) {
                    b.longest = new SetValue(set.getDurationSec(), set);
                }
            }
            return b;
        }
    }
}
