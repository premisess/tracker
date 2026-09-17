package com.fitness.tracker.controller;

import com.fitness.tracker.dto.BillingDtos.BillingStatus;
import com.fitness.tracker.dto.BillingDtos.CheckoutRequest;
import com.fitness.tracker.dto.BillingDtos.PaymentView;
import com.fitness.tracker.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final SubscriptionService subscriptionService;

    public BillingController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/status")
    public ResponseEntity<BillingStatus> status() {
        return ResponseEntity.ok(subscriptionService.status());
    }

    @PostMapping("/checkout")
    public ResponseEntity<PaymentView> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(subscriptionService.checkout(request));
    }

    @GetMapping("/payments/{orderReference}")
    public ResponseEntity<PaymentView> payment(@PathVariable String orderReference) {
        return ResponseEntity.ok(subscriptionService.refresh(orderReference));
    }

    /** Public: ClickPesa calls this. Always answers 200; the payment is verified against ClickPesa's API. */
    @PostMapping("/clickpesa/webhook")
    public ResponseEntity<Void> clickPesaWebhook(@RequestBody Map<String, Object> body) {
        subscriptionService.handleWebhook(body);
        return ResponseEntity.ok().build();
    }
}
