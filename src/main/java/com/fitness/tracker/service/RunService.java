package com.fitness.tracker.service;

import com.fitness.tracker.dto.LocationSettingsResponse;
import com.fitness.tracker.dto.RunDTO;
import com.fitness.tracker.dto.RunDetailResponse;
import com.fitness.tracker.dto.RunPointDTO;
import com.fitness.tracker.dto.RunSummaryResponse;
import com.fitness.tracker.entity.Profile;
import com.fitness.tracker.entity.RunSplit;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.entity.Workout;
import com.fitness.tracker.entity.WorkoutRoute;
import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.exception.ForbiddenException;
import com.fitness.tracker.exception.NotFoundException;
import com.fitness.tracker.repository.ProfileRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.repository.WorkoutRouteRepository;
import com.fitness.tracker.security.CurrentUserService;
import com.fitness.tracker.service.RunMetricsCalculator.ElevationSample;
import com.fitness.tracker.service.RunMetricsCalculator.Point;
import com.fitness.tracker.service.RunMetricsCalculator.Result;
import com.fitness.tracker.service.RunMetricsCalculator.Split;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Saving and reading GPS activities, and the location consent / privacy settings that govern them. */
@Service
public class RunService {

    public static final Set<String> GPS_TYPES = Set.of("Running", "Walking", "Hiking", "Cycling");
    public static final Set<Integer> PRIVACY_RADII = Set.of(0, 200, 500, 1000);

    // Simplifying the drawn line to 3 m keeps the shape while shrinking long routes a lot.
    private static final double DISPLAY_TOLERANCE_M = 3.0;
    private static final double DEFAULT_WEIGHT_KG = 70.0;

    private final WorkoutRepository workoutRepository;
    private final WorkoutRouteRepository workoutRouteRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final GoalService goalService;
    private final CurrentUserService currentUserService;
    private final JsonMapper jsonMapper;

    public RunService(WorkoutRepository workoutRepository, WorkoutRouteRepository workoutRouteRepository,
                      UserRepository userRepository, ProfileRepository profileRepository,
                      GoalService goalService, CurrentUserService currentUserService, JsonMapper jsonMapper) {
        this.workoutRepository = workoutRepository;
        this.workoutRouteRepository = workoutRouteRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.goalService = goalService;
        this.currentUserService = currentUserService;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public RunDetailResponse saveRun(RunDTO dto) {
        User user = currentUserService.get();
        if (user.getLocationConsentAt() == null) {
            throw new ForbiddenException("Turn on location tracking in Account Settings before saving a GPS activity");
        }
        if (!GPS_TYPES.contains(dto.getType())) {
            throw new BadRequestException("GPS tracking supports Running, Walking, Hiking and Cycling");
        }

        List<Point> points = dto.getPoints().stream().map(RunService::toPoint).toList();
        Result metrics = RunMetricsCalculator.calculate(dto.getType(), points);
        if (metrics.track().size() < 2 || metrics.movingTimeSec() < 1) {
            throw new BadRequestException(
                    "Not enough accurate GPS data to save this activity. Try again outdoors with a clear view of the sky.");
        }

        double weightKg = profileRepository.findByUserId(user.getId())
                .map(Profile::getWeight)
                .filter(w -> w > 0)
                .orElse(DEFAULT_WEIGHT_KG);

        Workout workout = new Workout();
        workout.setUser(user);
        workout.setType(dto.getType());
        workout.setSource(Workout.ActivitySource.GPS);
        workout.setDate(dto.getDate());
        workout.setStartedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(dto.getStartedAt()), ZoneOffset.UTC));
        workout.setDuration(Math.max(1, (int) Math.round(metrics.movingTimeSec() / 60.0)));
        workout.setCaloriesBurned(RunMetricsCalculator.calories(
                dto.getType(), metrics.distanceMeters(), metrics.movingTimeSec(), weightKg));
        workout.setNotes(dto.getNotes() == null || dto.getNotes().isBlank() ? null : dto.getNotes().trim());
        workout.setTags(dto.getTags() == null || dto.getTags().isEmpty() ? null
                : dto.getTags().stream().map(String::trim).collect(Collectors.joining(",")));
        workout.setDistanceMeters((int) Math.round(metrics.distanceMeters()));
        workout.setMovingTimeSec(metrics.movingTimeSec());
        workout.setElevationGainM(metrics.elevationGainM());
        workout.setAvgPaceSecPerKm(metrics.avgPaceSecPerKm());

        WorkoutRoute route = new WorkoutRoute();
        route.setWorkout(workout);
        route.setPointsJson(jsonMapper.writeValueAsString(points.stream().map(RunService::toRow).toList()));
        applyPolylines(route, metrics.track(), user.getRoutePrivacyMeters());
        workout.setRoute(route);

        for (Split s : metrics.splits()) {
            RunSplit split = new RunSplit();
            split.setWorkout(workout);
            split.setSplitIndex(s.index());
            split.setDistanceM(s.distanceM());
            split.setDurationSec(s.durationSec());
            split.setPaceSecPerKm(s.paceSecPerKm());
            split.setElevationGainM(s.elevationGainM());
            workout.getSplits().add(split);
        }

        workout = workoutRepository.save(workout);
        goalService.autoUpdateGoalsFromWorkout(user, workout.getType(), workout.getDuration(),
                workout.getCaloriesBurned(), metrics.distanceMeters() / 1000.0);

        return toDetail(workout, metrics.elevationProfile());
    }

    @Transactional(readOnly = true)
    public List<RunSummaryResponse> getMyRuns() {
        User user = currentUserService.get();
        return workoutRepository.findByUserIdOrderByDateDesc(user.getId()).stream()
                .filter(w -> w.getSource() == Workout.ActivitySource.GPS)
                .sorted(Comparator.comparing(Workout::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public RunDetailResponse getRun(Long workoutId) {
        User user = currentUserService.get();
        Workout workout = workoutRepository.findById(workoutId)
                .filter(w -> w.getUser().getId().equals(user.getId()) && w.getSource() == Workout.ActivitySource.GPS)
                .orElseThrow(() -> new NotFoundException("GPS activity not found"));
        List<ElevationSample> profile = workout.getRoute() == null
                ? List.of()
                : RunMetricsCalculator.calculate(workout.getType(), readPoints(workout.getRoute())).elevationProfile();
        return toDetail(workout, profile);
    }

    @Transactional(readOnly = true)
    public LocationSettingsResponse settings() {
        return toSettings(currentUserService.get());
    }

    @Transactional
    public LocationSettingsResponse setConsent(boolean consent) {
        User user = currentUserService.get();
        if (consent && user.getLocationConsentAt() == null) {
            user.setLocationConsentAt(LocalDateTime.now());
        } else if (!consent) {
            user.setLocationConsentAt(null);
        }
        return toSettings(userRepository.save(user));
    }

    @Transactional
    public LocationSettingsResponse setRoutePrivacy(int meters) {
        if (!PRIVACY_RADII.contains(meters)) {
            throw new BadRequestException("Choose a privacy zone of 0, 200, 500 or 1000 metres");
        }
        User user = currentUserService.get();
        user.setRoutePrivacyMeters(meters);
        userRepository.save(user);

        // Redraw the shared version of every existing route with the new zone.
        for (Workout workout : workoutRepository.findByUserIdOrderByDateDesc(user.getId())) {
            WorkoutRoute route = workout.getRoute();
            if (route != null) {
                applyPolylines(route, RunMetricsCalculator.calculate(workout.getType(), readPoints(route)).track(), meters);
            }
        }
        return toSettings(user);
    }

    /** Removes every stored route (the map data) but keeps the activities, distances and splits. */
    @Transactional
    public int deleteMyRoutes() {
        return workoutRouteRepository.deleteAllForUser(currentUserService.get().getId());
    }

    private void applyPolylines(WorkoutRoute route, List<Point> track, int privacyMeters) {
        route.setPolyline(PolylineCodec.encode(RunMetricsCalculator.simplify(track, DISPLAY_TOLERANCE_M)));
        List<Point> shared = RunMetricsCalculator.hideEnds(track, privacyMeters);
        route.setSharePolyline(shared.size() >= 2
                ? PolylineCodec.encode(RunMetricsCalculator.simplify(shared, DISPLAY_TOLERANCE_M))
                : null);
        route.setPrivacyMeters(privacyMeters);
    }

    private List<Point> readPoints(WorkoutRoute route) {
        List<List<Number>> rows = jsonMapper.readValue(route.getPointsJson(), new TypeReference<List<List<Number>>>() {});
        return rows.stream()
                .map(r -> new Point(
                        r.get(0).doubleValue(),
                        r.get(1).doubleValue(),
                        r.get(2) == null ? null : r.get(2).doubleValue(),
                        r.get(3).longValue(),
                        r.get(4) == null ? null : r.get(4).doubleValue(),
                        r.get(5).intValue()))
                .toList();
    }

    private RunSummaryResponse toSummary(Workout w) {
        return new RunSummaryResponse(
                w.getId(), w.getType(), w.getDate(), w.getStartedAt(),
                w.getDistanceMeters(), w.getMovingTimeSec(), w.getAvgPaceSecPerKm(), w.getElevationGainM(),
                w.getCaloriesBurned(),
                w.getRoute() == null ? null : w.getRoute().getPolyline());
    }

    private RunDetailResponse toDetail(Workout w, List<ElevationSample> profile) {
        WorkoutRoute route = w.getRoute();
        return new RunDetailResponse(
                toSummary(w),
                w.getNotes(),
                w.getTags() == null || w.getTags().isBlank() ? List.of() : Arrays.asList(w.getTags().split(",")),
                route == null ? null : route.getSharePolyline(),
                route == null ? null : route.getPrivacyMeters(),
                w.getSplits().stream()
                        .map(s -> new Split(s.getSplitIndex(), s.getDistanceM(), s.getDurationSec(),
                                s.getPaceSecPerKm(), s.getElevationGainM()))
                        .toList(),
                profile);
    }

    private static LocationSettingsResponse toSettings(User user) {
        return new LocationSettingsResponse(
                user.getLocationConsentAt() != null, user.getLocationConsentAt(), user.getRoutePrivacyMeters());
    }

    private static Point toPoint(RunPointDTO p) {
        return new Point(p.getLat(), p.getLng(), p.getAlt(), p.getT(), p.getAcc(), p.getSeg());
    }

    private static List<Object> toRow(Point p) {
        return Arrays.asList(p.lat(), p.lng(), p.alt(), p.t(), p.acc(), p.seg());
    }
}
