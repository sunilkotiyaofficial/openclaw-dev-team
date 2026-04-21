package com.example.payment.service;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Mock payment gateway for Phase 1 POC.
 * Always returns success. Phase 2: replace with real Stripe integration.
 * To simulate failure: set paymentMethodId = "pm_mock_fail"
 */
@Component
public class MockPaymentGateway {

    public record GatewayResult(boolean success, String transactionId, String failureCode, String failureReason) {}

    public Mono<GatewayResult> charge(String paymentMethodId, java.math.BigDecimal amount, String idempotencyKey) {
        if ("pm_mock_fail".equals(paymentMethodId)) {
            return Mono.just(new GatewayResult(false, null, "card_declined", "Card declined (mock)"));
        }
        if ("pm_mock_timeout".equals(paymentMethodId)) {
            return Mono.error(new RuntimeException("Gateway timeout (mock)"));
        }
        // Success path
        String txnId = "txn_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return Mono.just(new GatewayResult(true, txnId, null, null));
    }
}
