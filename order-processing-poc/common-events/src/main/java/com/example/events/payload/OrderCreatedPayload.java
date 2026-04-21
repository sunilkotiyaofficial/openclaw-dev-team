package com.example.events.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedPayload(
        @NotBlank String orderId,
        @NotBlank String customerId,
        @NotEmpty List<OrderItem> items,
        @NotNull @Positive BigDecimal totalAmount,
        @NotBlank String currency,
        @NotNull ShippingAddress shippingAddress,
        @NotBlank String paymentMethodId
) {
    public record OrderItem(
            @NotBlank String skuId,
            @Positive int quantity,
            @NotNull @Positive BigDecimal unitPrice
    ) {}

    public record ShippingAddress(
            @NotBlank String street,
            @NotBlank String city,
            @NotBlank String state,
            @NotBlank String zip,
            @NotBlank String country
    ) {}
}
