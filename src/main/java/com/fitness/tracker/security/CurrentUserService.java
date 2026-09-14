package com.fitness.tracker.security;

import com.fitness.tracker.entity.User;
import com.fitness.tracker.exception.UnauthorizedException;
import com.fitness.tracker.repository.UserRepository;
import org.springframework.stereotype.Component;

/** Resolves the signed-in user. Newer services use this instead of their own private lookup. */
@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get() {
        return userRepository.findByEmail(SecurityUtil.getCurrentUserEmail())
                .orElseThrow(() -> new UnauthorizedException("Your session has expired. Please sign in again."));
    }
}
