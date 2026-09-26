package com.fitness.tracker.service;

import com.fitness.tracker.dto.SleepDtos.SleepEntry;
import com.fitness.tracker.dto.SleepDtos.SleepRequest;
import com.fitness.tracker.dto.SleepDtos.SleepSummary;
import com.fitness.tracker.entity.SleepLog;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.exception.NotFoundException;
import com.fitness.tracker.repository.SleepLogRepository;
import com.fitness.tracker.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Nightly sleep logs and the weekly numbers shown on the Sleep page. */
@Service
public class SleepService {

    // Adults are recommended 7 to 9 hours; 8 is the goal the page measures against.
    public static final int GOAL_MINUTES = 8 * 60;
    private static final int MAX_SLEEP_MINUTES = 20 * 60;

    private final SleepLogRepository sleepLogRepository;
    private final CurrentUserService currentUserService;

    public SleepService(SleepLogRepository sleepLogRepository, CurrentUserService currentUserService) {
        this.sleepLogRepository = sleepLogRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public SleepSummary summary(int days) {
        User user = currentUserService.get();
        int span = Math.max(7, Math.min(days, 90));
        LocalDate today = LocalDate.now();
        List<SleepEntry> entries = sleepLogRepository
                .findByUserIdAndSleepDateGreaterThanEqualOrderBySleepDateDesc(user.getId(), today.minusDays(span - 1L))
                .stream().map(SleepService::toEntry).toList();

        List<SleepEntry> week = entries.stream().filter(e -> !e.sleepDate().isBefore(today.minusDays(6))).toList();
        Integer average = week.isEmpty() ? null
                : (int) Math.round(week.stream().mapToInt(SleepEntry::minutes).average().orElse(0));
        int onGoal = (int) week.stream().filter(e -> e.minutes() >= GOAL_MINUTES).count();

        return new SleepSummary(average, week.size(), GOAL_MINUTES, onGoal, entries);
    }

    @Transactional
    public SleepEntry log(SleepRequest request) {
        User user = currentUserService.get();
        LocalDateTime bed = request.getBedTime();
        LocalDateTime wake = request.getWakeTime();
        long minutes = Duration.between(bed, wake).toMinutes();
        if (minutes <= 0) {
            throw new BadRequestException("Wake time has to be after bedtime.");
        }
        if (minutes > MAX_SLEEP_MINUTES) {
            throw new BadRequestException("That's more than 20 hours. Check the bedtime and wake time.");
        }
        if (wake.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new BadRequestException("You can't log sleep that hasn't happened yet.");
        }

        LocalDate night = wake.toLocalDate();
        SleepLog entry = sleepLogRepository.findByUserIdAndSleepDate(user.getId(), night).orElseGet(() -> {
            SleepLog s = new SleepLog();
            s.setUser(user);
            s.setSleepDate(night);
            return s;
        });
        entry.setBedTime(bed);
        entry.setWakeTime(wake);
        entry.setQuality(request.getQuality());
        String notes = request.getNotes();
        entry.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        entry.setCreatedAt(LocalDateTime.now());
        return toEntry(sleepLogRepository.save(entry));
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUserService.get();
        SleepLog entry = sleepLogRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Sleep entry not found"));
        sleepLogRepository.delete(entry);
    }

    private static SleepEntry toEntry(SleepLog s) {
        int minutes = (int) Duration.between(s.getBedTime(), s.getWakeTime()).toMinutes();
        return new SleepEntry(s.getId(), s.getSleepDate(), s.getBedTime(), s.getWakeTime(), minutes, s.getQuality(), s.getNotes());
    }
}
