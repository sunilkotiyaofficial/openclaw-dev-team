package com.example.camel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Order DTO — flowing through Camel routes.
 *
 * <p>Java 21 record with Jakarta Bean Validation. Camel's
 * {@code bean-validator} component validates this on entry to the route.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Order(
        @NotBlank String orderId,
        @NotBlank String customerId,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be positive")
        BigDecimal amount,
        @NotBlank @Pattern(regexp = "US|EU|APAC", message = "Region must be US, EU, or APAC")
        String region,
        Instant timestamp,
        String enrichment        // populated by EnrichmentProcessor
) {
    public Order {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
