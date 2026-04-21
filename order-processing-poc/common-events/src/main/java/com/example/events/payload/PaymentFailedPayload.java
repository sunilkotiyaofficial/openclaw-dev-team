package com.example.events.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record PaymentFailedPayload(
        @NotBlank String orderId,
        @NotBlank String paymentAttemptId,
        @NotBlank String failureReason,
        @NotBlank String failureCode,
        boolean retryable,
        @NotNull Instant failedAt
) {}
