package com.fitness.tracker.dto;

import com.fitness.tracker.entity.Payment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public final class BillingDtos {

    private BillingDtos() {
    }

    public record Price(String plan, int amountTzs) {
    }

    /** ultimateUntil is null for free users; paymentsEnabled is false until ClickPesa is configured. */
    public record BillingStatus(
            boolean ultimate,
            LocalDateTime ultimateUntil,
            boolean paymentsEnabled,
            List<Price> prices,
            List<PaymentView> recentPayments) {
    }

    public record PaymentView(
            String orderReference,
            String plan,
            int amountTzs,
            String phoneNumber,
            String channel,
            String status,
            String message,
            LocalDateTime createdAt,
            LocalDateTime completedAt,
            LocalDateTime ultimateUntil) {
    }

    @Data
    public static class CheckoutRequest {
        @NotNull(message = "Choose monthly or yearly")
        private Payment.Plan plan;

        @NotBlank(message = "Enter the mobile money number to pay from")
        @Size(max = 20, message = "Enter a valid phone number")
        private String phoneNumber;
    }
}
