package com.fitness.tracker.service;

import com.fitness.tracker.exception.ConflictException;
import com.fitness.tracker.exception.UnauthorizedException;
import com.fitness.tracker.dto.*;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.CustomUserDetailsService;
import com.fitness.tracker.security.GoogleIdTokenVerifier;
import com.fitness.tracker.security.GoogleIdTokenVerifier.GoogleIdentity;
import com.fitness.tracker.security.SecurityUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final NotificationService notificationService;
    private final EmailVerificationService emailVerificationService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final CustomUserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        SecurityContextRepository securityContextRepository,
                        NotificationService notificationService,
                        EmailVerificationService emailVerificationService,
                        GoogleIdTokenVerifier googleIdTokenVerifier,
                        CustomUserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.notificationService = notificationService;
        this.emailVerificationService = emailVerificationService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.userDetailsService = userDetailsService;
    }

    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already in use");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(roleForNewUser());

        userRepository.save(user);
        // The welcome email carries the verification link.
        emailVerificationService.sendLink(user, true);

        // Log the new user straight into a session, same as login.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
        );
        establishSession(authentication, httpRequest, httpResponse);

        return AuthResponse.of(user);
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        establishSession(authentication, httpRequest, httpResponse);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Your session has expired. Please sign in again."));

        return AuthResponse.of(user);
    }

    /**
     * Signs in with a Google ID token. Finds the account by its Google id, else by email (linking the two,
     * since Google has confirmed the address), else creates a new one.
     */
    public AuthResponse signInWithGoogle(String credential, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        GoogleIdentity identity = googleIdTokenVerifier.verify(credential);
        if (!identity.emailVerified()) {
            throw new UnauthorizedException("Your Google account's email address isn't verified yet. Verify it with Google, or sign up with email instead.");
        }

        User user = userRepository.findByGoogleSubject(identity.subject())
                .or(() -> userRepository.findByEmail(identity.email()))
                .orElse(null);
        boolean created = user == null;

        if (created) {
            user = new User();
            user.setName(identity.name() != null ? identity.name() : identity.email().substring(0, identity.email().indexOf('@')));
            user.setEmail(identity.email());
            user.setPassword(passwordEncoder.encode(UUID.randomUUID() + "-" + UUID.randomUUID()));
            user.setCreatedAt(LocalDateTime.now());
            user.setRole(roleForNewUser());
            user.setAuthProvider(User.AuthProvider.GOOGLE);
        } else if (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(identity.subject())) {
            throw new ConflictException("This email is already linked to a different Google account.");
        }
        user.setGoogleSubject(identity.subject());
        user.setEmailVerified(true);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);

        if (created) {
            notificationService.sendWelcomeEmail(user);
        }

        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());
        establishSession(UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities()),
                httpRequest, httpResponse);
        return AuthResponse.of(user);
    }

    public AuthProvidersResponse providers() {
        return new AuthProvidersResponse(googleIdTokenVerifier.clientId());
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
                .orElseThrow(() -> new UnauthorizedException("Your session has expired. Please sign in again."));
        return AuthResponse.of(user);
    }

    private User.Role roleForNewUser() {
        return userRepository.count() == 0 ? User.Role.ADMIN : User.Role.USER;
    }

    private void establishSession(Authentication authentication, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // A session that existed before sign-in gets a new id, so a planted session id can't be reused.
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }
}
