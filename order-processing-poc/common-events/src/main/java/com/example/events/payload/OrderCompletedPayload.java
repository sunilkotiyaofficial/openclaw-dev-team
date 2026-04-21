package com.example.events.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OrderCompletedPayload(
        @NotBlank String orderId,
        @NotNull Instant completedAt,
        @NotBlank String trackingNumber,
        @NotBlank String carrier,
        @NotNull LocalDate estimatedDeliveryDate,
        @NotNull @Positive BigDecimal finalAmount
) {}
