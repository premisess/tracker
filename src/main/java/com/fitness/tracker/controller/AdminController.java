package com.fitness.tracker.controller;

import com.fitness.tracker.dto.RegisterRequest;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.entity.Workout;
import com.fitness.tracker.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final WorkoutRepository workoutRepository;
    private final GoalRepository goalRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           WorkoutRepository workoutRepository,
                           GoalRepository goalRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.workoutRepository = workoutRepository;
        this.goalRepository = goalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("name", user.getName());
                    map.put("email", user.getEmail());
                    map.put("role", user.getRole());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == User.Role.ADMIN) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.ADMIN)
                    .count();
            if (adminCount <= 1) {
                return ResponseEntity.badRequest().body("Cannot delete the last admin. Create another admin first!");
            }
        }

        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalWorkouts", workoutRepository.count());
        stats.put("totalGoals", goalRepository.count());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getReports() {
        Map<String, Object> report = new HashMap<>();

        List<Workout> allWorkouts = workoutRepository.findAll();
        Map<String, Long> workoutsByType = allWorkouts.stream()
                .collect(Collectors.groupingBy(Workout::getType, Collectors.counting()));
        report.put("workoutsByType", workoutsByType);

        Map<String, Long> usersByRole = userRepository.findAll().stream()
                .collect(Collectors.groupingBy(u -> u.getRole().name(), Collectors.counting()));
        report.put("usersByRole", usersByRole);

        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> userGrowthByMonth = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null)
                .collect(Collectors.groupingBy(u -> u.getCreatedAt().format(monthFormat), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, java.util.LinkedHashMap::new));
        report.put("userGrowthByMonth", userGrowthByMonth);

        Map<String, Object> mostActiveType = workoutsByType.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .<Map<String, Object>>map(e -> Map.of("type", e.getKey(), "count", e.getValue()))
                .orElse(Map.of());
        report.put("mostPopularWorkoutType", mostActiveType);

        return ResponseEntity.ok(report);
    }

    @PostMapping("/create-admin")
    public ResponseEntity<String> createAdmin(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        User admin = new User();
        admin.setName(request.getName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRole(User.Role.ADMIN);
        admin.setCreatedAt(java.time.LocalDateTime.now());

        userRepository.save(admin);
        return ResponseEntity.ok("Admin created successfully");
    }
}
