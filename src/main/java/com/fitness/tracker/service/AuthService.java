package com.fitness.tracker.service;

import com.fitness.tracker.dto.*;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.SecurityUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final NotificationService notificationService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        SecurityContextRepository securityContextRepository,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.notificationService = notificationService;
    }

    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(java.time.LocalDateTime.now());
        if (userRepository.count() == 0) {
            user.setRole(User.Role.ADMIN);
        } else {
            user.setRole(User.Role.USER);
        }

        userRepository.save(user);
        notificationService.sendWelcomeEmail(user);

        // Log the new user straight into a session, same as login.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        establishSession(authentication, httpRequest, httpResponse);

        return new AuthResponse(user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        establishSession(authentication, httpRequest, httpResponse);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new AuthResponse(user.getName(), user.getEmail(), user.getRole().name());
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SecurityContextHolder.clearContext();
        var session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        // Clear the session cookie on the client.
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        httpResponse.addCookie(cookie);
    }

    public AuthResponse currentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new AuthResponse(user.getName(), user.getEmail(), user.getRole().name());
    }

    private void establishSession(Authentication authentication, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }
}
