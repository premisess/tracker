package com.fitness.tracker.security;

import com.fitness.tracker.exception.ApiException;
import com.fitness.tracker.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks the ID token Google Identity Services gives the website, without extra libraries:
 * the RS256 signature against Google's published keys, then issuer, audience (our client ID) and expiry.
 */
@Component
public class GoogleIdTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifier.class);

    private static final URI GOOGLE_CERTS = URI.create("https://www.googleapis.com/oauth2/v3/certs");
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final long CLOCK_SKEW_SECONDS = 300;
    // Google rotates its keys every few days. An unknown key id triggers a refetch, at most once a minute.
    private static final Duration KEY_CACHE_TTL = Duration.ofHours(1);
    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofMinutes(1);
    private static final String FAILED = "Google sign-in failed. Please try again.";
    private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {
    }

    /** Where the signing keys come from, by key id. Tests supply their own. */
    @FunctionalInterface
    interface KeySource {
        Map<String, PublicKey> fetch() throws Exception;
    }

    private final String clientId;
    private final JsonMapper jsonMapper;
    private final KeySource keySource;
    private final Clock clock;

    private Map<String, PublicKey> keys = Map.of();
    private Instant keysFetchedAt = Instant.EPOCH;

    @Autowired
    public GoogleIdTokenVerifier(@Value("${app.google.client-id:}") String clientId, JsonMapper jsonMapper) {
        this(clientId, jsonMapper, null, Clock.systemUTC());
    }

    GoogleIdTokenVerifier(String clientId, JsonMapper jsonMapper, KeySource keySource, Clock clock) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.jsonMapper = jsonMapper;
        this.keySource = keySource != null ? keySource : this::fetchGoogleKeys;
        this.clock = clock;
    }

    /** The OAuth client ID the website should use, or null when Google sign-in isn't configured. */
    public String clientId() {
        return clientId.isEmpty() ? null : clientId;
    }

    public GoogleIdentity verify(String idToken) {
        if (clientId.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in isn't set up on this server yet.");
        }
        String[] parts = idToken == null ? new String[0] : idToken.split("\\.", -1);
        if (parts.length != 3) {
            throw new UnauthorizedException(FAILED);
        }
        try {
            Map<String, Object> header = jsonMapper.readValue(decode(parts[0]), JSON_OBJECT);
            Map<String, Object> claims = jsonMapper.readValue(decode(parts[1]), JSON_OBJECT);
            if (!"RS256".equals(header.get("alg")) || !(header.get("kid") instanceof String kid)) {
                throw new UnauthorizedException(FAILED);
            }
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(keyFor(kid));
            signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!signature.verify(decode(parts[2]))) {
                throw new UnauthorizedException(FAILED);
            }
            checkClaims(claims);
            return new GoogleIdentity(
                    (String) claims.get("sub"),
                    (String) claims.get("email"),
                    isTrue(claims.get("email_verified")),
                    claims.get("name") instanceof String name && !name.isBlank() ? name.trim() : null);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Rejected a Google ID token: {}", e.toString());
            throw new UnauthorizedException(FAILED);
        }
    }

    private void checkClaims(Map<String, Object> claims) {
        long now = clock.instant().getEpochSecond();
        boolean audienceMatches = switch (claims.get("aud")) {
            case String aud -> aud.equals(clientId);
            case Collection<?> audiences -> audiences.contains(clientId);
            case null, default -> false;
        };
        if (!ISSUERS.contains(claims.get("iss"))
                || !audienceMatches
                || !(claims.get("exp") instanceof Number exp) || now > exp.longValue() + CLOCK_SKEW_SECONDS
                || (claims.get("iat") instanceof Number iat && iat.longValue() > now + CLOCK_SKEW_SECONDS)
                || !(claims.get("sub") instanceof String subject) || subject.isBlank()
                || !(claims.get("email") instanceof String email) || email.isBlank()) {
            throw new UnauthorizedException(FAILED);
        }
    }

    private synchronized PublicKey keyFor(String kid) throws Exception {
        Instant now = clock.instant();
        if (keys.isEmpty() || now.isAfter(keysFetchedAt.plus(KEY_CACHE_TTL))) {
            refreshKeys(now);
        }
        PublicKey key = keys.get(kid);
        if (key == null && now.isAfter(keysFetchedAt.plus(MIN_REFRESH_INTERVAL))) {
            refreshKeys(now);
            key = keys.get(kid);
        }
        if (key == null) {
            throw new UnauthorizedException(FAILED);
        }
        return key;
    }

    private void refreshKeys(Instant now) throws Exception {
        keys = Map.copyOf(keySource.fetch());
        keysFetchedAt = now;
    }

    private Map<String, PublicKey> fetchGoogleKeys() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(GOOGLE_CERTS).timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Google's key endpoint returned HTTP " + response.statusCode());
        }
        Map<String, Object> body = jsonMapper.readValue(response.body(), JSON_OBJECT);
        Map<String, PublicKey> result = new HashMap<>();
        if (body.get("keys") instanceof List<?> jwks) {
            for (Object item : jwks) {
                if (item instanceof Map<?, ?> jwk && "RSA".equals(jwk.get("kty"))
                        && jwk.get("kid") instanceof String kid
                        && jwk.get("n") instanceof String modulus
                        && jwk.get("e") instanceof String exponent) {
                    result.put(kid, KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                            new BigInteger(1, decode(modulus)), new BigInteger(1, decode(exponent)))));
                }
            }
        }
        return result;
    }

    private static byte[] decode(String base64Url) {
        return Base64.getUrlDecoder().decode(base64Url);
    }

    private static boolean isTrue(Object value) {
        return value instanceof Boolean b ? b : value instanceof String s && "true".equalsIgnoreCase(s);
    }
}
