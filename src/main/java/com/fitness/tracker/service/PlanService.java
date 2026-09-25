package com.fitness.tracker.service;

import com.fitness.tracker.dto.CustomPlanRequest;
import com.fitness.tracker.dto.PlanDtos.ActivePlan;
import com.fitness.tracker.dto.PlanDtos.PlanDetail;
import com.fitness.tracker.dto.PlanDtos.PlanExercise;
import com.fitness.tracker.dto.PlanDtos.PlanSession;
import com.fitness.tracker.dto.PlanDtos.PlanSummary;
import com.fitness.tracker.dto.PlanDtos.SessionLog;
import com.fitness.tracker.dto.PlanSessionRequest;
import com.fitness.tracker.entity.Exercise;
import com.fitness.tracker.entity.PlanDay;
import com.fitness.tracker.entity.PlanDayExercise;
import com.fitness.tracker.entity.PlanEnrollment;
import com.fitness.tracker.entity.PlanSessionLog;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.entity.Workout;
import com.fitness.tracker.entity.WorkoutPlan;
import com.fitness.tracker.enums.WorkoutType;
import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.exception.ConflictException;
import com.fitness.tracker.exception.NotFoundException;
import com.fitness.tracker.repository.ExerciseRepository;
import com.fitness.tracker.repository.PlanEnrollmentRepository;
import com.fitness.tracker.repository.PlanSessionLogRepository;
import com.fitness.tracker.repository.WorkoutPlanRepository;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Browsing workout plans, building your own, and following one. Sessions run in order: session n falls in
 * week (n - 1) / daysPerWeek + 1, on day (n - 1) % daysPerWeek + 1 of that week.
 */
@Service
@Transactional(readOnly = true)
public class PlanService {

    private static final String SLUG_SUFFIX_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";

    private final WorkoutPlanRepository planRepository;
    private final PlanEnrollmentRepository enrollmentRepository;
    private final PlanSessionLogRepository sessionLogRepository;
    private final WorkoutRepository workoutRepository;
    private final ExerciseRepository exerciseRepository;
    private final CurrentUserService currentUserService;
    private final ExerciseCatalogService exerciseCatalogService;
    private final SecureRandom random = new SecureRandom();

    public PlanService(WorkoutPlanRepository planRepository, PlanEnrollmentRepository enrollmentRepository,
                       PlanSessionLogRepository sessionLogRepository, WorkoutRepository workoutRepository,
                       ExerciseRepository exerciseRepository, CurrentUserService currentUserService,
                       ExerciseCatalogService exerciseCatalogService) {
        this.planRepository = planRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.sessionLogRepository = sessionLogRepository;
        this.workoutRepository = workoutRepository;
        this.exerciseRepository = exerciseRepository;
        this.currentUserService = currentUserService;
        this.exerciseCatalogService = exerciseCatalogService;
    }

    public List<PlanSummary> listPlans() {
        User user = currentUserService.get();
        Long activePlanId = activePlanId(user);
        return planRepository.findVisibleTo(user.getId()).stream()
                .map(plan -> toSummary(plan, plan.getId().equals(activePlanId)))
                .toList();
    }

    public PlanDetail getPlan(String slug) {
        User user = currentUserService.get();
        WorkoutPlan plan = findPlan(slug, user);
        return toDetail(plan, plan.getId().equals(activePlanId(user)));
    }

    /** Builds a new plan owned by the user. */
    @Transactional
    public PlanDetail createCustom(CustomPlanRequest request) {
        User user = currentUserService.get();

        WorkoutPlan plan = new WorkoutPlan();
        plan.setOwner(user);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setSlug(newSlug(request.getName()));
        applyRequest(plan, request);
        return toDetail(planRepository.save(plan), false);
    }

    /** Replaces a custom plan's details and sessions. Plans that have been started keep their history, so they can't be edited. */
    @Transactional
    public PlanDetail updateCustom(String slug, CustomPlanRequest request) {
        User user = currentUserService.get();
        WorkoutPlan plan = ownedPlan(slug, user);
        if (enrollmentRepository.existsByPlanId(plan.getId())) {
            throw new ConflictException("You've already started this plan, so it can't be changed. Make a copy instead.");
        }
        applyRequest(plan, request);
        return toDetail(planRepository.save(plan), false);
    }

    /** Deletes a custom plan, including any time the user followed it. Logged workouts stay. */
    @Transactional
    public void deleteCustom(String slug) {
        planRepository.delete(ownedPlan(slug, currentUserService.get()));
    }

    /** Starts a plan. Switching from another active plan needs {@code replace}, which quits the old one. */
    @Transactional
    public ActivePlan start(String slug, boolean replace) {
        User user = currentUserService.get();
        WorkoutPlan plan = findPlan(slug, user);

        Optional<PlanEnrollment> current = enrollmentRepository.findFirstByUserIdAndStatus(user.getId(), PlanEnrollment.Status.ACTIVE);
        if (current.isPresent()) {
            PlanEnrollment old = current.get();
            if (!replace) {
                throw new ConflictException("You're already following " + old.getPlan().getName()
                        + ". Quit it first, or choose to switch plans.");
            }
            old.setStatus(PlanEnrollment.Status.CANCELLED);
            old.setFinishedAt(LocalDateTime.now());
            enrollmentRepository.saveAndFlush(old);
        }

        PlanEnrollment enrollment = new PlanEnrollment();
        enrollment.setUser(user);
        enrollment.setPlan(plan);
        enrollment.setStatus(PlanEnrollment.Status.ACTIVE);
        enrollment.setStartedOn(LocalDate.now());
        enrollment.setCompletedSessions(0);
        return toActivePlan(enrollmentRepository.save(enrollment));
    }

    public Optional<ActivePlan> active() {
        return activeFor(currentUserService.get());
    }

    public Optional<ActivePlan> activeFor(User user) {
        return enrollmentRepository.findFirstByUserIdAndStatus(user.getId(), PlanEnrollment.Status.ACTIVE)
                .map(this::toActivePlan);
    }

    /** Counts the next session as done by the given workout, or skips it. Finishing the last session completes the plan. */
    @Transactional
    public ActivePlan logSession(PlanSessionRequest request) {
        User user = currentUserService.get();
        PlanEnrollment enrollment = enrollmentRepository.findFirstByUserIdAndStatus(user.getId(), PlanEnrollment.Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("You aren't following a workout plan right now"));

        Workout workout = null;
        if (!request.isSkip()) {
            workout = workoutRepository.findById(request.getWorkoutId())
                    .filter(w -> w.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new NotFoundException("Workout not found"));
            if (sessionLogRepository.existsByWorkoutId(workout.getId())) {
                throw new ConflictException("That workout already counts toward a plan session");
            }
        }

        WorkoutPlan plan = enrollment.getPlan();
        int done = enrollment.getCompletedSessions();
        PlanSessionLog log = new PlanSessionLog();
        log.setEnrollment(enrollment);
        log.setPlanDay(dayFor(plan, done));
        log.setSessionNumber(done + 1);
        log.setWorkout(workout);
        log.setSkipped(request.isSkip());
        log.setLoggedAt(LocalDateTime.now());
        sessionLogRepository.save(log);

        enrollment.setCompletedSessions(done + 1);
        if (done + 1 >= plan.totalSessions()) {
            enrollment.setStatus(PlanEnrollment.Status.COMPLETED);
            enrollment.setFinishedAt(LocalDateTime.now());
        }
        return toActivePlan(enrollmentRepository.save(enrollment));
    }

    @Transactional
    public void quit() {
        User user = currentUserService.get();
        PlanEnrollment enrollment = enrollmentRepository.findFirstByUserIdAndStatus(user.getId(), PlanEnrollment.Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("You aren't following a workout plan right now"));
        enrollment.setStatus(PlanEnrollment.Status.CANCELLED);
        enrollment.setFinishedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
    }

    /** The session at zero-based position {@code index}: a week-specific day if the plan has one, else the repeating day. */
    static PlanDay dayFor(WorkoutPlan plan, int index) {
        int week = index / plan.getDaysPerWeek() + 1;
        int day = index % plan.getDaysPerWeek() + 1;
        return plan.getDays().stream()
                .filter(d -> d.getDayNumber() == day && Objects.equals(d.getWeekNumber(), week))
                .findFirst()
                .or(() -> plan.getDays().stream()
                        .filter(d -> d.getDayNumber() == day && d.getWeekNumber() == null)
                        .findFirst())
                .orElseThrow(() -> new IllegalStateException(
                        "Plan " + plan.getSlug() + " has no session for week " + week + ", day " + day));
    }

    /** "My Push Day!" becomes "my-my-push-day-k3x9qp": readable, unique, and never clashing with built-in slugs. */
    String newSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (base.length() > 40) {
            base = base.substring(0, 40).replaceAll("-+$", "");
        }
        if (base.isEmpty()) {
            base = "plan";
        }
        String slug;
        do {
            StringBuilder suffix = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                suffix.append(SLUG_SUFFIX_ALPHABET.charAt(random.nextInt(SLUG_SUFFIX_ALPHABET.length())));
            }
            slug = "my-" + base + "-" + suffix;
        } while (planRepository.existsBySlug(slug));
        return slug;
    }

    private void applyRequest(WorkoutPlan plan, CustomPlanRequest request) {
        plan.setName(request.getName().trim());
        plan.setSummary(blankToNull(request.getSummary()) != null
                ? request.getSummary().trim()
                : request.getDaysPerWeek() + " sessions a week for " + request.getDurationWeeks() + " weeks");
        plan.setDescription(blankToNull(request.getDescription()));
        plan.setGoal(request.getGoal());
        plan.setLevel(request.getLevel());
        plan.setEquipment(blankToNull(request.getEquipment()) != null ? request.getEquipment().trim() : "Your choice");
        plan.setDurationWeeks(request.getDurationWeeks());
        plan.setDaysPerWeek(request.getDaysPerWeek());

        plan.getDays().clear();
        int dayNumber = 1;
        for (CustomPlanRequest.Session session : request.getSessions()) {
            WorkoutType type = WorkoutType.fromLabel(session.getWorkoutType())
                    .orElseThrow(() -> new BadRequestException("Unknown workout type '" + session.getWorkoutType() + "'"));
            boolean run = session.getActivity() == PlanDay.Activity.RUN;

            PlanDay day = new PlanDay();
            day.setPlan(plan);
            day.setWeekNumber(null);
            day.setDayNumber(dayNumber++);
            day.setTitle(session.getTitle().trim());
            day.setFocus(blankToNull(session.getFocus()));
            day.setActivity(session.getActivity());
            day.setWorkoutType(type.getLabel());
            day.setTargetMinutes(session.getTargetMinutes());
            day.setTargetDistanceM(run ? session.getTargetDistanceM() : null);
            day.setInstructions(blankToNull(session.getInstructions()));

            int order = 0;
            for (CustomPlanRequest.SessionExercise item : session.getExercises() != null ? session.getExercises() : List.<CustomPlanRequest.SessionExercise>of()) {
                Exercise exercise = exerciseRepository.findById(item.getExerciseId())
                        .orElseThrow(() -> new BadRequestException("Exercise " + item.getExerciseId() + " doesn't exist"));
                PlanDayExercise planExercise = new PlanDayExercise();
                planExercise.setPlanDay(day);
                planExercise.setExercise(exercise);
                planExercise.setSortOrder(order++);
                planExercise.setSets(item.getSets());
                planExercise.setReps(item.getReps().trim());
                planExercise.setRestSec(item.getRestSec());
                day.getExercises().add(planExercise);
            }
            plan.getDays().add(day);
        }
    }

    private ActivePlan toActivePlan(PlanEnrollment enrollment) {
        WorkoutPlan plan = enrollment.getPlan();
        int total = plan.totalSessions();
        int done = enrollment.getCompletedSessions();
        boolean following = enrollment.getStatus() == PlanEnrollment.Status.ACTIVE && done < total;

        List<SessionLog> recent = sessionLogRepository.findTop5ByEnrollmentIdOrderBySessionNumberDesc(enrollment.getId())
                .stream()
                .map(l -> new SessionLog(
                        l.getSessionNumber(),
                        (l.getSessionNumber() - 1) / plan.getDaysPerWeek() + 1,
                        (l.getSessionNumber() - 1) % plan.getDaysPerWeek() + 1,
                        l.getPlanDay().getTitle(),
                        l.isSkipped(),
                        l.getWorkout() != null ? l.getWorkout().getId() : null,
                        l.getLoggedAt()))
                .toList();

        return new ActivePlan(
                enrollment.getId(),
                toSummary(plan, following),
                enrollment.getStatus().name(),
                enrollment.getStartedOn(),
                done,
                total,
                (int) Math.round(done * 100.0 / total),
                following ? done / plan.getDaysPerWeek() + 1 : null,
                following ? done % plan.getDaysPerWeek() + 1 : null,
                following ? toSession(dayFor(plan, done)) : null,
                recent);
    }

    private PlanDetail toDetail(WorkoutPlan plan, boolean active) {
        return new PlanDetail(toSummary(plan, active), plan.getDescription(),
                plan.getDays().stream().map(this::toSession).toList());
    }

    private PlanSummary toSummary(WorkoutPlan plan, boolean active) {
        boolean custom = plan.getOwner() != null;
        boolean editable = custom && (plan.getId() == null || !enrollmentRepository.existsByPlanId(plan.getId()));
        return new PlanSummary(plan.getId(), plan.getSlug(), plan.getName(), plan.getSummary(), plan.getGoal().name(),
                plan.getLevel(), plan.getEquipment(), plan.getDurationWeeks(), plan.getDaysPerWeek(),
                plan.totalSessions(), active, custom, editable);
    }

    private PlanSession toSession(PlanDay day) {
        return new PlanSession(day.getId(), day.getWeekNumber(), day.getDayNumber(), day.getTitle(), day.getFocus(),
                day.getActivity().name(), day.getWorkoutType(), day.getTargetMinutes(), day.getTargetDistanceM(),
                day.getInstructions(),
                day.getExercises().stream().map(pe -> {
                    Exercise e = pe.getExercise();
                    return new PlanExercise(e.getId(), e.getName(), exerciseCatalogService.thumbnailUrl(e),
                            e.getTrackingType().name(), pe.getSets(), pe.getReps(), pe.getRestSec());
                }).toList());
    }

    private Long activePlanId(User user) {
        return enrollmentRepository.findFirstByUserIdAndStatus(user.getId(), PlanEnrollment.Status.ACTIVE)
                .map(e -> e.getPlan().getId())
                .orElse(null);
    }

    private WorkoutPlan findPlan(String slug, User user) {
        return planRepository.findBySlug(slug)
                .filter(plan -> plan.isVisibleTo(user))
                .orElseThrow(() -> new NotFoundException("Workout plan not found"));
    }

    private WorkoutPlan ownedPlan(String slug, User user) {
        return planRepository.findBySlug(slug)
                .filter(plan -> plan.getOwner() != null && plan.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Custom plan not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
