package com.fitness.tracker.service;

import com.fitness.tracker.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * ClickPesa's collection API (docs.clickpesa.com): a USSD push asks the customer to approve the payment
 * with their PIN on M-Pesa, Mixx by Yas (Tigo Pesa), Airtel Money or HaloPesa. There is no sandbox, so
 * every call moves real money; test with small amounts.
 */
@Component
public class ClickPesaClient {

    private static final Logger log = LoggerFactory.getLogger(ClickPesaClient.class);
    // Tokens last an hour; renew a little early.
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(55);
    private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> JSON_ARRAY = new TypeReference<>() {};

    public record PushResult(String transactionId, String status, String channel) {
    }

    public record PaymentStatus(String status, String channel, String message) {
    }

    private final String baseUrl;
    private final String clientId;
    private final String apiKey;
    private final String checksumKey;
    private final JsonMapper jsonMapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private String token;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public ClickPesaClient(@Value("${app.billing.clickpesa.base-url}") String baseUrl,
                           @Value("${app.billing.clickpesa.client-id:}") String clientId,
                           @Value("${app.billing.clickpesa.api-key:}") String apiKey,
                           @Value("${app.billing.clickpesa.checksum-key:}") String checksumKey,
                           JsonMapper jsonMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.clientId = clientId.trim();
        this.apiKey = apiKey.trim();
        this.checksumKey = checksumKey.trim();
        this.jsonMapper = jsonMapper;
    }

    public boolean isConfigured() {
        return !clientId.isEmpty() && !apiKey.isEmpty();
    }

    /** Sends the PIN prompt to the customer's phone. amountTzs is whole shillings; phone is 255XXXXXXXXX. */
    public PushResult initiateUssdPush(int amountTzs, String orderReference, String phoneNumber) {
        Map<String, Object> body = new TreeMap<>();
        body.put("amount", String.valueOf(amountTzs));
        body.put("currency", "TZS");
        body.put("orderReference", orderReference);
        body.put("phoneNumber", phoneNumber);
        if (!checksumKey.isEmpty()) {
            body.put("checksum", checksum(checksumKey, body, jsonMapper));
        }

        HttpRequest request = authorized(baseUrl + "/payments/initiate-ussd-push-request")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() / 100 != 2) {
            throw providerError("start the payment", response);
        }
        Map<String, Object> result = jsonMapper.readValue(response.body(), JSON_OBJECT);
        return new PushResult(text(result.get("id")), text(result.get("status")), text(result.get("channel")));
    }

    /** The latest state of a payment, or empty if ClickPesa has no payment with that reference yet. */
    public Optional<PaymentStatus> queryPayment(String orderReference) {
        HttpRequest request = authorized(baseUrl + "/payments/" + URLEncoder.encode(orderReference, StandardCharsets.UTF_8))
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        if (response.statusCode() / 100 != 2) {
            throw providerError("check the payment", response);
        }
        List<Map<String, Object>> payments = jsonMapper.readValue(response.body(), JSON_ARRAY);
        return payments.stream().findFirst()
                .map(p -> new PaymentStatus(text(p.get("status")), text(p.get("channel")), text(p.get("message"))));
    }

    /**
     * ClickPesa's payload checksum: keys sorted alphabetically (TreeMap), compact JSON, HMAC-SHA256, hex.
     * Only flat payloads are sent, so no nested sorting is needed.
     */
    static String checksum(String key, Map<String, Object> sortedPayload, JsonMapper jsonMapper) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(jsonMapper.writeValueAsString(sortedPayload).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Could not compute the ClickPesa checksum", e);
        }
    }

    private HttpRequest.Builder authorized(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", token());
    }

    private synchronized String token() {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Mobile money payments aren't set up on this server yet.");
        }
        if (token != null && Instant.now().isBefore(tokenExpiresAt)) {
            return token;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/generate-token"))
                .timeout(Duration.ofSeconds(30))
                .header("client-id", clientId)
                .header("api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() / 100 != 2) {
            throw providerError("sign in to the payment provider", response);
        }
        String value = text(jsonMapper.readValue(response.body(), JSON_OBJECT).get("token"));
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment provider didn't accept our credentials.");
        }
        // The token already starts with "Bearer ".
        token = value.startsWith("Bearer ") ? value : "Bearer " + value;
        tokenExpiresAt = Instant.now().plus(TOKEN_LIFETIME);
        return token;
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment provider didn't respond. Please try again.");
        } catch (Exception e) {
            log.warn("ClickPesa request to {} failed: {}", request.uri().getPath(), e.toString());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment provider didn't respond. Please try again.");
        }
    }

    private ApiException providerError(String action, HttpResponse<String> response) {
        if (response.statusCode() == 401) {
            synchronized (this) {
                token = null;
            }
        }
        String detail = null;
        try {
            detail = text(jsonMapper.readValue(response.body(), JSON_OBJECT).get("message"));
        } catch (Exception ignored) {
            // Not JSON; fall back to a generic message.
        }
        log.warn("ClickPesa could not {}: HTTP {} {}", action, response.statusCode(), response.body());
        return new ApiException(HttpStatus.BAD_GATEWAY,
                "Couldn't " + action + (detail != null && !detail.isBlank() ? ": " + detail : ". Please try again."));
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
