package com.fitness.tracker.service;

import com.fitness.tracker.exception.UnauthorizedException;
import com.fitness.tracker.dto.WaterIntakeDTO;
import com.fitness.tracker.dto.WaterIntakeResponse;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.entity.WaterIntake;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.repository.WaterIntakeRepository;
import com.fitness.tracker.security.SecurityUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WaterIntakeService {

    private final WaterIntakeRepository waterIntakeRepository;
    private final UserRepository userRepository;

    public WaterIntakeService(WaterIntakeRepository waterIntakeRepository, UserRepository userRepository) {
        this.waterIntakeRepository = waterIntakeRepository;
        this.userRepository = userRepository;
    }

    public WaterIntakeResponse logIntake(WaterIntakeDTO dto) {
        User user = getCurrentUser();

        // If entry for that date exists, add to it; otherwise create new
        WaterIntake intake = waterIntakeRepository.findByUserIdAndDate(user.getId(), dto.getDate())
                .orElseGet(() -> {
                    WaterIntake w = new WaterIntake();
                    w.setUser(user);
                    w.setDate(dto.getDate());
                    w.setAmountMl(0);
                    return w;
                });

        intake.setAmountMl(intake.getAmountMl() + dto.getAmountMl());
        intake = waterIntakeRepository.save(intake);

        return toResponse(intake);
    }

    public List<WaterIntakeResponse> getMyIntake() {
        User user = getCurrentUser();
        return waterIntakeRepository.findByUserIdOrderByDateDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private WaterIntakeResponse toResponse(WaterIntake intake) {
        return new WaterIntakeResponse(intake.getId(), intake.getAmountMl(), intake.getDate());
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Your session has expired. Please sign in again."));
    }
}