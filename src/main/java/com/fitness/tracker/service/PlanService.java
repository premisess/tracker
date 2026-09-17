package com.fitness.tracker.service;

import com.fitness.tracker.dto.PlanDtos.ActivePlan;
import com.fitness.tracker.dto.PlanDtos.PlanDetail;
import com.fitness.tracker.dto.PlanDtos.PlanExercise;
import com.fitness.tracker.dto.PlanDtos.PlanSession;
import com.fitness.tracker.dto.PlanDtos.PlanSummary;
import com.fitness.tracker.dto.PlanDtos.SessionLog;
import com.fitness.tracker.dto.PlanSessionRequest;
import com.fitness.tracker.entity.Exercise;
import com.fitness.tracker.entity.PlanDay;
import com.fitness.tracker.entity.PlanEnrollment;
import com.fitness.tracker.entity.PlanSessionLog;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.entity.Workout;
import com.fitness.tracker.entity.WorkoutPlan;
import com.fitness.tracker.exception.ConflictException;
import com.fitness.tracker.exception.NotFoundException;
import com.fitness.tracker.repository.PlanEnrollmentRepository;
import com.fitness.tracker.repository.PlanSessionLogRepository;
import com.fitness.tracker.repository.WorkoutPlanRepository;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Browsing workout plans and following one. Sessions run in order: session n falls in week
 * (n - 1) / daysPerWeek + 1, on day (n - 1) % daysPerWeek + 1 of that week.
 */
@Service
@Transactional(readOnly = true)
public class PlanService {

    private final WorkoutPlanRepository planRepository;
    private final PlanEnrollmentRepository enrollmentRepository;
    private final PlanSessionLogRepository sessionLogRepository;
    private final WorkoutRepository workoutRepository;
    private final CurrentUserService currentUserService;
    private final ExerciseCatalogService exerciseCatalogService;
    private final UltimateGuard ultimateGuard;

    public PlanService(WorkoutPlanRepository planRepository, PlanEnrollmentRepository enrollmentRepository,
                       PlanSessionLogRepository sessionLogRepository, WorkoutRepository workoutRepository,
                       CurrentUserService currentUserService, ExerciseCatalogService exerciseCatalogService,
                       UltimateGuard ultimateGuard) {
        this.planRepository = planRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.sessionLogRepository = sessionLogRepository;
        this.workoutRepository = workoutRepository;
        this.currentUserService = currentUserService;
        this.exerciseCatalogService = exerciseCatalogService;
        this.ultimateGuard = ultimateGuard;
    }

    public List<PlanSummary> listPlans() {
        Long activePlanId = activePlanId(currentUserService.get());
        return planRepository.findAllByOrderByIdAsc().stream()
                .map(plan -> toSummary(plan, plan.getId().equals(activePlanId)))
                .toList();
    }

    public PlanDetail getPlan(String slug) {
        WorkoutPlan plan = findPlan(slug);
        boolean active = plan.getId().equals(activePlanId(currentUserService.get()));
        return new PlanDetail(toSummary(plan, active), plan.getDescription(),
                plan.getDays().stream().map(this::toSession).toList());
    }

    /** Starts a plan. Switching from another active plan needs {@code replace}, which quits the old one. */
    @Transactional
    public ActivePlan start(String slug, boolean replace) {
        User user = currentUserService.get();
        ultimateGuard.require(user, "Following workout plans");
        WorkoutPlan plan = findPlan(slug);

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
        ultimateGuard.require(user, "Following workout plans");
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

    private PlanSummary toSummary(WorkoutPlan plan, boolean active) {
        return new PlanSummary(plan.getId(), plan.getSlug(), plan.getName(), plan.getSummary(), plan.getGoal().name(),
                plan.getLevel(), plan.getEquipment(), plan.getDurationWeeks(), plan.getDaysPerWeek(),
                plan.totalSessions(), active);
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

    private WorkoutPlan findPlan(String slug) {
        return planRepository.findBySlug(slug).orElseThrow(() -> new NotFoundException("Workout plan not found"));
    }
}
