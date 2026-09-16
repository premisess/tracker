package com.fitness.tracker.security;

import com.fitness.tracker.exception.ApiException;
import com.fitness.tracker.exception.UnauthorizedException;
import com.fitness.tracker.security.GoogleIdTokenVerifier.GoogleIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GoogleIdTokenVerifierTest {

    private static final String CLIENT_ID = "1234-test.apps.googleusercontent.com";
    private static final Instant NOW = Instant.parse("2026-09-15T12:00:00Z");

    private final JsonMapper json = JsonMapper.builder().build();
    private final AtomicInteger keyFetches = new AtomicInteger();
    private KeyPair googleKey;
    private GoogleIdTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        googleKey = newKeyPair();
        verifier = new GoogleIdTokenVerifier(CLIENT_ID, json, () -> {
            keyFetches.incrementAndGet();
            return Map.of("key-1", googleKey.getPublic());
        }, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void validTokenGivesTheGoogleIdentity() throws Exception {
        GoogleIdentity identity = verifier.verify(token(claims(), "key-1", googleKey.getPrivate()));

        assertEquals("109876543210", identity.subject());
        assertEquals("runner@example.com", identity.email());
        assertTrue(identity.emailVerified());
        assertEquals("Test Runner", identity.name());
    }

    @Test
    void tokenForAnotherAppIsRejected() throws Exception {
        Map<String, Object> claims = claims();
        claims.put("aud", "someone-else.apps.googleusercontent.com");

        assertThrows(UnauthorizedException.class, () -> verifier.verify(token(claims, "key-1", googleKey.getPrivate())));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Map<String, Object> claims = claims();
        claims.put("exp", NOW.minusSeconds(3600).getEpochSecond());

        assertThrows(UnauthorizedException.class, () -> verifier.verify(token(claims, "key-1", googleKey.getPrivate())));
    }

    @Test
    void wrongIssuerIsRejected() throws Exception {
        Map<String, Object> claims = claims();
        claims.put("iss", "https://evil.example.com");

        assertThrows(UnauthorizedException.class, () -> verifier.verify(token(claims, "key-1", googleKey.getPrivate())));
    }

    @Test
    void editedPayloadFailsTheSignatureCheck() throws Exception {
        String[] parts = token(claims(), "key-1", googleKey.getPrivate()).split("\\.");
        Map<String, Object> forged = claims();
        forged.put("email", "victim@example.com");
        String tampered = parts[0] + "." + base64(json.writeValueAsBytes(forged)) + "." + parts[2];

        assertThrows(UnauthorizedException.class, () -> verifier.verify(tampered));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        PrivateKey attackerKey = newKeyPair().getPrivate();

        assertThrows(UnauthorizedException.class, () -> verifier.verify(token(claims(), "key-1", attackerKey)));
    }

    @Test
    void unknownKeyIdIsRejected() throws Exception {
        assertThrows(UnauthorizedException.class,
                () -> verifier.verify(token(claims(), "key-that-does-not-exist", googleKey.getPrivate())));
    }

    @Test
    void garbageIsRejected() {
        assertThrows(UnauthorizedException.class, () -> verifier.verify("not-a-jwt"));
        assertThrows(UnauthorizedException.class, () -> verifier.verify("a.b.c"));
    }

    @Test
    void signingKeysAreCachedBetweenSignIns() throws Exception {
        verifier.verify(token(claims(), "key-1", googleKey.getPrivate()));
        verifier.verify(token(claims(), "key-1", googleKey.getPrivate()));

        assertEquals(1, keyFetches.get());
    }

    @Test
    void withoutAClientIdGoogleSignInIsUnavailable() {
        GoogleIdTokenVerifier unconfigured = new GoogleIdTokenVerifier("", json, Map::of, Clock.systemUTC());

        assertNull(unconfigured.clientId());
        ApiException e = assertThrows(ApiException.class, () -> unconfigured.verify("a.b.c"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
    }

    private Map<String, Object> claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", "https://accounts.google.com");
        claims.put("aud", CLIENT_ID);
        claims.put("sub", "109876543210");
        claims.put("email", "runner@example.com");
        claims.put("email_verified", true);
        claims.put("name", "Test Runner");
        claims.put("iat", NOW.minusSeconds(60).getEpochSecond());
        claims.put("exp", NOW.plusSeconds(3600).getEpochSecond());
        return claims;
    }

    private String token(Map<String, Object> claims, String kid, PrivateKey key) throws Exception {
        String header = base64(json.writeValueAsBytes(Map.of("alg", "RS256", "kid", kid, "typ", "JWT")));
        String payload = base64(json.writeValueAsBytes(claims));
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update((header + "." + payload).getBytes(StandardCharsets.US_ASCII));
        return header + "." + payload + "." + base64(signer.sign());
    }

    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String base64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
