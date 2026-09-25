package com.fitness.tracker.service;

import com.fitness.tracker.dto.FoodDiaryResponse;
import com.fitness.tracker.dto.FoodLogEntryResponse;
import com.fitness.tracker.dto.FoodLogRequest;
import com.fitness.tracker.dto.FoodLogUpdateRequest;
import com.fitness.tracker.dto.FoodRequest;
import com.fitness.tracker.dto.FoodResponse;
import com.fitness.tracker.dto.MacroTotals;
import com.fitness.tracker.dto.NutritionHistoryResponse;
import com.fitness.tracker.dto.NutritionTargets;
import com.fitness.tracker.entity.Food;
import com.fitness.tracker.entity.FoodLogEntry;
import com.fitness.tracker.entity.Goal;
import com.fitness.tracker.entity.Profile;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.exception.NotFoundException;
import com.fitness.tracker.repository.FoodLogEntryRepository;
import com.fitness.tracker.repository.FoodRepository;
import com.fitness.tracker.repository.GoalRepository;
import com.fitness.tracker.repository.ProfileRepository;
import com.fitness.tracker.repository.WaterIntakeRepository;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.security.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The food diary: finding foods, logging what was eaten, and daily targets from the profile and main goal. */
@Service
@Transactional(readOnly = true)
public class NutritionService {

    private static final int SEARCH_LIMIT = 25;
    private static final int RECENT_LIMIT = 12;
    // When several goals are active, the one that changes calorie needs most decides the targets.
    private static final List<Goal.GoalType> GOAL_PRIORITY = List.of(
            Goal.GoalType.LOSE_WEIGHT, Goal.GoalType.GAIN_WEIGHT, Goal.GoalType.BUILD_STRENGTH,
            Goal.GoalType.RUN_MORE, Goal.GoalType.STAY_ACTIVE);

    private final FoodRepository foodRepository;
    private final FoodLogEntryRepository foodLogEntryRepository;
    private final ProfileRepository profileRepository;
    private final GoalRepository goalRepository;
    private final WorkoutRepository workoutRepository;
    private final WaterIntakeRepository waterIntakeRepository;
    private final CurrentUserService currentUserService;

    public NutritionService(FoodRepository foodRepository, FoodLogEntryRepository foodLogEntryRepository,
                            ProfileRepository profileRepository, GoalRepository goalRepository,
                            WorkoutRepository workoutRepository, WaterIntakeRepository waterIntakeRepository,
                            CurrentUserService currentUserService) {
        this.foodRepository = foodRepository;
        this.foodLogEntryRepository = foodLogEntryRepository;
        this.profileRepository = profileRepository;
        this.goalRepository = goalRepository;
        this.workoutRepository = workoutRepository;
        this.waterIntakeRepository = waterIntakeRepository;
        this.currentUserService = currentUserService;
    }

    /** Foods matching the search; with no search term, the foods the user logged most recently. */
    public List<FoodResponse> searchFoods(String query) {
        User user = currentUserService.get();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return recentFoods(user);
        }
        String escaped = q.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return foodRepository.search(user.getId(), "%" + escaped + "%", escaped + "%", PageRequest.of(0, SEARCH_LIMIT))
                .stream()
                .map(NutritionService::toFoodResponse)
                .toList();
    }

    public List<FoodResponse> recentFoods() {
        return recentFoods(currentUserService.get());
    }

    public List<FoodResponse> myFoods() {
        return foodRepository.findByOwnerIdOrderByNameAsc(currentUserService.get().getId()).stream()
                .map(NutritionService::toFoodResponse)
                .toList();
    }

    @Transactional
    public FoodResponse createFood(FoodRequest request) {
        User user = currentUserService.get();
        return toFoodResponse(foodRepository.save(newFood(request, user)));
    }

    /** Deletes one of the user's own foods. Diary entries keep their copied name and numbers. */
    @Transactional
    public void deleteFood(Long id) {
        User user = currentUserService.get();
        Food food = foodRepository.findById(id)
                .filter(f -> f.getOwner() != null && f.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Food not found"));
        foodRepository.delete(food);
    }

    public NutritionTargets targets() {
        return targetsFor(currentUserService.get());
    }

    public NutritionTargets targetsFor(User user) {
        Goal.GoalType goal = goalRepository.findByUserId(user.getId()).stream()
                .filter(g -> g.getStatus() == Goal.Status.IN_PROGRESS && g.getGoalType() != null)
                .map(Goal::getGoalType)
                .min(Comparator.comparingInt(GOAL_PRIORITY::indexOf))
                .orElse(null);
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            return NutritionTargetCalculator.calculate(null, null, null, null, goal);
        }
        return NutritionTargetCalculator.calculate(
                profile.getWeight(), profile.getHeight(), profile.getAge(), profile.getGender(), goal);
    }

    public FoodDiaryResponse diary(LocalDate date) {
        User user = currentUserService.get();
        LocalDate day = date != null ? date : LocalDate.now();

        Map<String, List<FoodLogEntryResponse>> meals = new LinkedHashMap<>();
        for (FoodLogEntry.Meal meal : FoodLogEntry.Meal.values()) {
            meals.put(meal.name(), new ArrayList<>());
        }
        double calories = 0, protein = 0, carbs = 0, fat = 0;
        for (FoodLogEntry entry : foodLogEntryRepository.findByUserIdAndLogDateOrderByCreatedAtAsc(user.getId(), day)) {
            meals.get(entry.getMeal().name()).add(toEntryResponse(entry));
            calories += entry.getCalories();
            protein += entry.getProteinG();
            carbs += entry.getCarbsG();
            fat += entry.getFatG();
        }

        return new FoodDiaryResponse(
                day,
                targetsFor(user),
                new MacroTotals(round1(calories), round1(protein), round1(carbs), round1(fat)),
                (int) workoutRepository.caloriesBurnedOn(user.getId(), day),
                (int) waterIntakeRepository.totalForDay(user.getId(), day),
                meals);
    }

    @Transactional
    public FoodLogEntryResponse addEntry(FoodLogRequest request) {
        User user = currentUserService.get();
        Food food;
        if (request.getFoodId() != null) {
            food = foodRepository.findVisible(request.getFoodId(), user.getId())
                    .orElseThrow(() -> new NotFoundException("Food not found"));
        } else {
            food = newFood(request.getCustomFood(), user);
            if (Boolean.TRUE.equals(request.getSaveCustomFood())) {
                food = foodRepository.save(food);
            }
        }

        double servings = request.getServings();
        FoodLogEntry entry = new FoodLogEntry();
        entry.setUser(user);
        entry.setLogDate(request.getDate());
        entry.setMeal(request.getMeal());
        entry.setFood(food.getId() != null ? food : null);
        entry.setName(food.getName());
        entry.setServingLabel(food.getServingLabel());
        entry.setServings(servings);
        entry.setCalories(round1(food.getCalories() * servings));
        entry.setProteinG(round1(food.getProteinG() * servings));
        entry.setCarbsG(round1(food.getCarbsG() * servings));
        entry.setFatG(round1(food.getFatG() * servings));
        entry.setCreatedAt(LocalDateTime.now());
        return toEntryResponse(foodLogEntryRepository.save(entry));
    }

    @Transactional
    public FoodLogEntryResponse updateEntry(Long id, FoodLogUpdateRequest request) {
        FoodLogEntry entry = ownedEntry(id);
        if (request.getServings() != null && !request.getServings().equals(entry.getServings())) {
            double factor = request.getServings() / entry.getServings();
            entry.setCalories(round1(entry.getCalories() * factor));
            entry.setProteinG(round1(entry.getProteinG() * factor));
            entry.setCarbsG(round1(entry.getCarbsG() * factor));
            entry.setFatG(round1(entry.getFatG() * factor));
            entry.setServings(request.getServings());
        }
        if (request.getMeal() != null) {
            entry.setMeal(request.getMeal());
        }
        return toEntryResponse(foodLogEntryRepository.save(entry));
    }

    @Transactional
    public void deleteEntry(Long id) {
        foodLogEntryRepository.delete(ownedEntry(id));
    }

    /** Totals for the last {@code days} days (7 to 90), including days with nothing logged. */
    public NutritionHistoryResponse history(int days) {
        User user = currentUserService.get();
        int span = Math.max(7, Math.min(days, 90));
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(span - 1L);

        Map<LocalDate, Object[]> eaten = new HashMap<>();
        for (Object[] row : foodLogEntryRepository.dailyTotals(user.getId(), from, to)) {
            eaten.put((LocalDate) row[0], row);
        }
        Map<LocalDate, Integer> burned = new HashMap<>();
        for (Object[] row : workoutRepository.caloriesBurnedByDay(user.getId(), from, to)) {
            burned.put((LocalDate) row[0], row[1] == null ? 0 : ((Number) row[1]).intValue());
        }

        List<NutritionHistoryResponse.Day> points = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Object[] row = eaten.get(d);
            points.add(new NutritionHistoryResponse.Day(d,
                    row == null ? 0 : round1(number(row[1])),
                    row == null ? 0 : round1(number(row[2])),
                    row == null ? 0 : round1(number(row[3])),
                    row == null ? 0 : round1(number(row[4])),
                    burned.getOrDefault(d, 0)));
        }
        return new NutritionHistoryResponse(targetsFor(user), points);
    }

    public int caloriesEaten(User user, LocalDate date) {
        return (int) Math.round(foodLogEntryRepository.caloriesOn(user.getId(), date));
    }

    private List<FoodResponse> recentFoods(User user) {
        Map<Long, Food> distinct = new LinkedHashMap<>();
        for (FoodLogEntry entry : foodLogEntryRepository.findRecent(user.getId(), PageRequest.of(0, 100))) {
            if (entry.getFood() != null) {
                distinct.putIfAbsent(entry.getFood().getId(), entry.getFood());
                if (distinct.size() == RECENT_LIMIT) break;
            }
        }
        return distinct.values().stream().map(NutritionService::toFoodResponse).toList();
    }

    private FoodLogEntry ownedEntry(Long id) {
        User user = currentUserService.get();
        return foodLogEntryRepository.findById(id)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Diary entry not found"));
    }

    private static Food newFood(FoodRequest request, User owner) {
        Food food = new Food();
        food.setName(request.getName().trim());
        food.setServingLabel(request.getServingLabel().trim());
        food.setServingGrams(request.getServingGrams());
        food.setCalories(request.getCalories());
        food.setProteinG(request.getProteinG());
        food.setCarbsG(request.getCarbsG());
        food.setFatG(request.getFatG());
        food.setFiberG(request.getFiberG());
        food.setOwner(owner);
        food.setCreatedAt(LocalDateTime.now());
        return food;
    }

    static FoodResponse toFoodResponse(Food f) {
        return new FoodResponse(f.getId(), f.getName(), f.getServingLabel(), f.getServingGrams(),
                f.getCalories(), f.getProteinG(), f.getCarbsG(), f.getFatG(), f.getFiberG(), f.getOwner() != null);
    }

    private static FoodLogEntryResponse toEntryResponse(FoodLogEntry e) {
        return new FoodLogEntryResponse(e.getId(), e.getLogDate(), e.getMeal().name(),
                e.getFood() != null ? e.getFood().getId() : null,
                e.getName(), e.getServingLabel(), e.getServings(),
                e.getCalories(), e.getProteinG(), e.getCarbsG(), e.getFatG());
    }

    private static double number(Object value) {
        return value == null ? 0 : ((Number) value).doubleValue();
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
