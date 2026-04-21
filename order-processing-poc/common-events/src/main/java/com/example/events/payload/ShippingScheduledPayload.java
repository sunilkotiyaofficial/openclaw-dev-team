package com.example.events.payload;

import com.example.events.payload.OrderCreatedPayload.ShippingAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ShippingScheduledPayload(
        @NotBlank String orderId,
        @NotBlank String shipmentId,
        @NotBlank String carrier,
        @NotBlank String trackingNumber,
        @NotNull LocalDate estimatedDeliveryDate,
        @NotBlank String pickupWarehouseId,
        @NotNull ShippingAddress shippingAddress
) {}
