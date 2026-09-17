package com.fitness.tracker.service;

import com.fitness.tracker.entity.User;
import com.fitness.tracker.exception.PaymentRequiredException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Decides who can use Ultimate features: paying users until their access runs out, and admins. */
@Component
public class UltimateGuard {

    public static boolean hasUltimate(User user) {
        return user.getRole() == User.Role.ADMIN
                || (user.getUltimateUntil() != null && user.getUltimateUntil().isAfter(LocalDateTime.now()));
    }

    /** @param feature what the user tried to use, e.g. "Workout plans" */
    public void require(User user, String feature) {
        if (!hasUltimate(user)) {
            throw new PaymentRequiredException(feature + " is part of FitTracker Ultimate.");
        }
    }
}
