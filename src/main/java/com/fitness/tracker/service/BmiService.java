package com.fitness.tracker.service;

import com.fitness.tracker.dto.BmiDTO;
import com.fitness.tracker.dto.BmiResponse;
import com.fitness.tracker.entity.BmiRecord;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.repository.BmiRecordRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.SecurityUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BmiService {

    private final BmiRecordRepository bmiRecordRepository;
    private final UserRepository userRepository;

    public BmiService(BmiRecordRepository bmiRecordRepository, UserRepository userRepository) {
        this.bmiRecordRepository = bmiRecordRepository;
        this.userRepository = userRepository;
    }

    public BmiResponse calculateAndSave(BmiDTO dto) {
        User user = getCurrentUser();

        double heightInMeters = dto.getHeight() / 100;
        double bmi = dto.getWeight() / (heightInMeters * heightInMeters);
        bmi = Math.round(bmi * 10) / 10.0; // round to 1 decimal

        String category = getCategory(bmi);

        BmiRecord record = new BmiRecord();
        record.setUser(user);
        record.setWeight(dto.getWeight());
        record.setHeight(dto.getHeight());
        record.setBmiValue(bmi);
        record.setCategory(category);
        record.setDate(dto.getDate());

        record = bmiRecordRepository.save(record);
        return toResponse(record);
    }

    public List<BmiResponse> getMyBmiHistory() {
        User user = getCurrentUser();
        return bmiRecordRepository.findByUserIdOrderByDateDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private String getCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Overweight";
        return "Obese";
    }

    private BmiResponse toResponse(BmiRecord record) {
        return new BmiResponse(
                record.getId(),
                record.getWeight(),
                record.getHeight(),
                record.getBmiValue(),
                record.getCategory(),
                record.getDate()
        );
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}