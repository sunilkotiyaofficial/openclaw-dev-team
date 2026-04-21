package com.example.events.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCompletedPayload(
        @NotBlank String orderId,
        @NotBlank String paymentAttemptId,
        @NotBlank String transactionId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotNull Instant processedAt
) {}
