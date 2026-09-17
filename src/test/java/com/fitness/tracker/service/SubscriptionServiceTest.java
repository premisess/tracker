package com.fitness.tracker.service;

import com.fitness.tracker.entity.Payment;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 17, 10, 0);

    @Test
    void tanzanianNumbersAreNormalisedInEveryCommonFormat() {
        assertEquals(Optional.of("255712345678"), SubscriptionService.normalizePhone("0712 345 678"));
        assertEquals(Optional.of("255712345678"), SubscriptionService.normalizePhone("+255 712-345-678"));
        assertEquals(Optional.of("255712345678"), SubscriptionService.normalizePhone("712345678"));
        assertEquals(Optional.of("255622345678"), SubscriptionService.normalizePhone("255622345678"));
    }

    @Test
    void nonMobileOrForeignNumbersAreRejected() {
        assertTrue(SubscriptionService.normalizePhone("0222 123 456").isEmpty());   // landline
        assertTrue(SubscriptionService.normalizePhone("254712345678").isEmpty());   // Kenya
        assertTrue(SubscriptionService.normalizePhone("07123").isEmpty());
        assertTrue(SubscriptionService.normalizePhone(null).isEmpty());
    }

    @Test
    void newSubscriptionStartsNow() {
        assertEquals(NOW.plusMonths(1), SubscriptionService.extend(null, NOW, Payment.Plan.MONTHLY));
        assertEquals(NOW.plusYears(1), SubscriptionService.extend(NOW.minusDays(3), NOW, Payment.Plan.YEARLY));
    }

    @Test
    void renewingEarlyAddsToRemainingTime() {
        LocalDateTime current = NOW.plusDays(10);
        assertEquals(current.plusMonths(1), SubscriptionService.extend(current, NOW, Payment.Plan.MONTHLY));
    }

    @Test
    void checksumUsesSortedCompactJsonAndHmacSha256() {
        Map<String, Object> payload = new TreeMap<>(Map.of(
                "phoneNumber", "255712345678", "amount", "10000", "currency", "TZS", "orderReference", "FTABC"));

        String checksum = ClickPesaClient.checksum("secret", payload, JsonMapper.builder().build());

        // HMAC-SHA256("secret", {"amount":"10000","currency":"TZS","orderReference":"FTABC","phoneNumber":"255712345678"})
        assertEquals(64, checksum.length());
        assertEquals(checksum, ClickPesaClient.checksum("secret", new TreeMap<>(payload), JsonMapper.builder().build()));
        assertNotEquals(checksum, ClickPesaClient.checksum("other", payload, JsonMapper.builder().build()));
    }
}
