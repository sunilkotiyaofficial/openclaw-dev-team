package com.example.events.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PaymentRequestedPayload(
        @NotBlank String orderId,
        @NotBlank String paymentAttemptId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String paymentMethodId,
        @NotBlank String idempotencyKey
) {}
