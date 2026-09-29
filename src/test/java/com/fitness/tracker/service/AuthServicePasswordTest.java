package com.fitness.tracker.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthServicePasswordTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void bcryptRejectsPasswordsOver72Bytes() {
        // Why Google sign-up used to fail: its 73-character placeholder password was too long.
        assertThrows(IllegalArgumentException.class, () -> encoder.encode("a".repeat(73)));
        assertDoesNotThrow(() -> encoder.encode("a".repeat(72)));
    }

    @Test
    void googleAccountsGetAStrongPasswordBcryptAccepts() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            String password = AuthService.randomUnusablePassword();
            assertEquals(43, password.length());
            assertTrue(seen.add(password), "passwords must not repeat");
        }
        String password = AuthService.randomUnusablePassword();
        assertTrue(encoder.matches(password, encoder.encode(password)));
    }
}
