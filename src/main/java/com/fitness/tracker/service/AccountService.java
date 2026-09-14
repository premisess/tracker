package com.fitness.tracker.service;

import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.exception.ConflictException;
import com.fitness.tracker.exception.UnauthorizedException;
import com.fitness.tracker.dto.ChangeEmailRequest;
import com.fitness.tracker.dto.ChangePasswordRequest;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.repository.BmiRecordRepository;
import com.fitness.tracker.repository.GoalRepository;
import com.fitness.tracker.repository.ProfileRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.repository.WaterIntakeRepository;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.security.CustomUserDetailsService;
import com.fitness.tracker.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final GoalRepository goalRepository;
    private final WorkoutRepository workoutRepository;
    private final BmiRecordRepository bmiRecordRepository;
    private final WaterIntakeRepository waterIntakeRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;

    public AccountService(UserRepository userRepository, ProfileRepository profileRepository,
                           GoalRepository goalRepository, WorkoutRepository workoutRepository,
                           BmiRecordRepository bmiRecordRepository, WaterIntakeRepository waterIntakeRepository,
                           PasswordEncoder passwordEncoder,
                           CustomUserDetailsService userDetailsService,
                           SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.goalRepository = goalRepository;
        this.workoutRepository = workoutRepository;
        this.bmiRecordRepository = bmiRecordRepository;
        this.waterIntakeRepository = waterIntakeRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public User changeEmail(ChangeEmailRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        String newEmail = request.getNewEmail() != null ? request.getNewEmail().trim() : null;
        if (newEmail == null || newEmail.isBlank()) {
            throw new BadRequestException("Email cannot be empty");
        }
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("Email already in use");
        }

        user.setEmail(newEmail);
        userRepository.save(user);

        // The session's Authentication still carries the old email as its principal name;
        // re-authenticate under the new one so later requests can still resolve the user.
        UserDetails userDetails = userDetailsService.loadUserByUsername(newEmail);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return user;
    }

    public void updateName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BadRequestException("Name cannot be empty");
        }
        User user = getCurrentUser();
        user.setName(newName.trim());
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = getCurrentUser();

        if (user.getRole() == User.Role.ADMIN) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.ADMIN)
                    .count();
            if (adminCount <= 1) {
                throw new BadRequestException("Cannot delete the last admin. Create another admin first!");
            }
        }

        workoutRepository.deleteAll(workoutRepository.findByUserIdOrderByDateDesc(user.getId()));
        goalRepository.deleteAll(goalRepository.findByUserId(user.getId()));
        bmiRecordRepository.deleteAll(bmiRecordRepository.findByUserIdOrderByDateDesc(user.getId()));
        waterIntakeRepository.deleteAll(waterIntakeRepository.findByUserIdOrderByDateDesc(user.getId()));
        profileRepository.findByUserId(user.getId()).ifPresent(profileRepository::delete);
        userRepository.delete(user);

        SecurityContextHolder.clearContext();
        var session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("JSESSIONID", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        httpResponse.addCookie(cookie);
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Your session has expired. Please sign in again."));
    }
}
