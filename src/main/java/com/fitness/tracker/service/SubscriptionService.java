package com.fitness.tracker.service;

import com.fitness.tracker.dto.BillingDtos.BillingStatus;
import com.fitness.tracker.dto.BillingDtos.CheckoutRequest;
import com.fitness.tracker.dto.BillingDtos.PaymentView;
import com.fitness.tracker.dto.BillingDtos.Price;
import com.fitness.tracker.entity.Payment;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.exception.ApiException;
import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.exception.ConflictException;
import com.fitness.tracker.exception.NotFoundException;
import com.fitness.tracker.repository.PaymentRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.CurrentUserService;
import com.fitness.tracker.service.ClickPesaClient.PaymentStatus;
import com.fitness.tracker.service.ClickPesaClient.PushResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Buying FitTracker Ultimate with mobile money. A payment starts PENDING when the PIN prompt is sent and
 * becomes SUCCESS or FAILED once ClickPesa says so. The result is always read back from ClickPesa's API,
 * never taken from a webhook body, so a forged callback can't grant access.
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final Pattern TZ_MOBILE = Pattern.compile("^255[67]\\d{8}$");
    private static final String REFERENCE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    // A customer has roughly this long to enter their PIN; until then a second payment is refused.
    private static final int PENDING_LOCK_MINUTES = 2;

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ClickPesaClient clickPesa;
    private final CurrentUserService currentUserService;
    private final int monthlyPriceTzs;
    private final int yearlyPriceTzs;
    private final SecureRandom random = new SecureRandom();

    public SubscriptionService(PaymentRepository paymentRepository, UserRepository userRepository,
                               ClickPesaClient clickPesa, CurrentUserService currentUserService,
                               @Value("${app.billing.monthly-price-tzs}") int monthlyPriceTzs,
                               @Value("${app.billing.yearly-price-tzs}") int yearlyPriceTzs) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.clickPesa = clickPesa;
        this.currentUserService = currentUserService;
        this.monthlyPriceTzs = monthlyPriceTzs;
        this.yearlyPriceTzs = yearlyPriceTzs;
    }

    @Transactional(readOnly = true)
    public BillingStatus status() {
        User user = currentUserService.get();
        return new BillingStatus(
                UltimateGuard.hasUltimate(user),
                user.getUltimateUntil(),
                clickPesa.isConfigured(),
                List.of(new Price("MONTHLY", monthlyPriceTzs), new Price("YEARLY", yearlyPriceTzs)),
                paymentRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toView).toList());
    }

    /** Records the payment, then asks ClickPesa to send the PIN prompt to the phone. */
    @Transactional(noRollbackFor = ApiException.class)
    public PaymentView checkout(CheckoutRequest request) {
        User user = currentUserService.get();
        if (!clickPesa.isConfigured()) {
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Mobile money payments aren't set up on this server yet.");
        }
        String phone = normalizePhone(request.getPhoneNumber())
                .orElseThrow(() -> new BadRequestException(
                        "Enter a Tanzanian mobile money number, for example 0712 345 678."));
        if (paymentRepository.existsByUserIdAndStatusAndCreatedAtAfter(user.getId(), Payment.Status.PENDING,
                LocalDateTime.now().minusMinutes(PENDING_LOCK_MINUTES))) {
            throw new ConflictException("A payment is already waiting for your PIN. Check your phone, or try again in a couple of minutes.");
        }

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setOrderReference(newOrderReference());
        payment.setPlan(request.getPlan());
        payment.setAmountTzs(priceOf(request.getPlan()));
        payment.setPhoneNumber(phone);
        payment.setStatus(Payment.Status.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.saveAndFlush(payment);

        try {
            PushResult result = clickPesa.initiateUssdPush(payment.getAmountTzs(), payment.getOrderReference(), phone);
            payment.setProviderTransactionId(result.transactionId());
            payment.setChannel(result.channel());
            if ("FAILED".equalsIgnoreCase(result.status())) {
                payment.setStatus(Payment.Status.FAILED);
                payment.setMessage("The payment couldn't be started. Check the number and try again.");
            }
        } catch (ApiException e) {
            payment.setStatus(Payment.Status.FAILED);
            payment.setMessage(e.getMessage());
            paymentRepository.save(payment);
            throw e;
        }
        return toView(paymentRepository.save(payment));
    }

    /** The website polls this while the customer enters their PIN. */
    @Transactional
    public PaymentView refresh(String orderReference) {
        User user = currentUserService.get();
        Payment payment = paymentRepository.findByOrderReference(orderReference)
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        if (payment.getStatus() == Payment.Status.PENDING && clickPesa.isConfigured()) {
            clickPesa.queryPayment(orderReference).ifPresent(status -> apply(payment, status));
        }
        return toView(payment);
    }

    /**
     * ClickPesa's webhook. Only the order reference is used; the real status is fetched from ClickPesa.
     * Unknown references are ignored so the endpoint reveals nothing.
     */
    @Transactional
    public void handleWebhook(Map<String, Object> body) {
        Object data = body.get("data");
        if (!(data instanceof Map<?, ?> fields) || !(fields.get("orderReference") instanceof String reference)) {
            return;
        }
        paymentRepository.findByOrderReference(reference)
                .filter(p -> p.getStatus() == Payment.Status.PENDING)
                .ifPresent(payment -> {
                    try {
                        clickPesa.queryPayment(reference).ifPresent(status -> apply(payment, status));
                    } catch (ApiException e) {
                        log.warn("Webhook for {} couldn't be confirmed yet: {}", reference, e.getMessage());
                    }
                });
    }

    private void apply(Payment payment, PaymentStatus status) {
        String value = status.status() == null ? "" : status.status().toUpperCase();
        if (status.channel() != null) {
            payment.setChannel(status.channel());
        }
        switch (value) {
            case "SUCCESS", "SETTLED" -> markPaid(payment);
            case "FAILED" -> {
                payment.setStatus(Payment.Status.FAILED);
                payment.setMessage(status.message() != null ? status.message() : "The payment was declined or cancelled.");
                payment.setCompletedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
            default -> {
                // Still waiting for the PIN.
            }
        }
    }

    private void markPaid(Payment payment) {
        if (payment.getStatus() == Payment.Status.SUCCESS) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(Payment.Status.SUCCESS);
        payment.setMessage(null);
        payment.setCompletedAt(now);
        User user = payment.getUser();
        user.setUltimateUntil(extend(user.getUltimateUntil(), now, payment.getPlan()));
        userRepository.save(user);
        paymentRepository.save(payment);
        log.info("Payment {} succeeded; user {} has Ultimate until {}", payment.getOrderReference(), user.getId(), user.getUltimateUntil());
    }

    /** Time is added on top of any access still remaining, so paying early never loses days. */
    static LocalDateTime extend(LocalDateTime current, LocalDateTime now, Payment.Plan plan) {
        LocalDateTime base = current != null && current.isAfter(now) ? current : now;
        return plan == Payment.Plan.YEARLY ? base.plusYears(1) : base.plusMonths(1);
    }

    /** Accepts 0712 345 678, 712345678, +255 712 345 678 or 255712345678; returns 255712345678. */
    static Optional<String> normalizePhone(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String digits = raw.replaceAll("[\\s\\-()+]", "");
        if (digits.startsWith("0") && digits.length() == 10) {
            digits = "255" + digits.substring(1);
        } else if (digits.length() == 9) {
            digits = "255" + digits;
        }
        return TZ_MOBILE.matcher(digits).matches() ? Optional.of(digits) : Optional.empty();
    }

    private int priceOf(Payment.Plan plan) {
        return plan == Payment.Plan.YEARLY ? yearlyPriceTzs : monthlyPriceTzs;
    }

    private String newOrderReference() {
        StringBuilder reference = new StringBuilder("FT");
        while (reference.length() < 20) {
            reference.append(REFERENCE_ALPHABET.charAt(random.nextInt(REFERENCE_ALPHABET.length())));
        }
        return reference.toString();
    }

    private PaymentView toView(Payment p) {
        return new PaymentView(p.getOrderReference(), p.getPlan().name(), p.getAmountTzs(), p.getPhoneNumber(),
                p.getChannel(), p.getStatus().name(), p.getMessage(), p.getCreatedAt(), p.getCompletedAt(),
                p.getUser().getUltimateUntil());
    }
}
