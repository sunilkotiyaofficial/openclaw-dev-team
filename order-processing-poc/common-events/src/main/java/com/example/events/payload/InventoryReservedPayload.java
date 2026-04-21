package com.example.events.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record InventoryReservedPayload(
        @NotBlank String orderId,
        @NotBlank String reservationId,
        @NotEmpty List<ReservedItem> items,
        @NotNull Instant reservationExpiresAt
) {
    public record ReservedItem(
            @NotBlank String skuId,
            int quantity,
            @NotBlank String warehouseId
    ) {}
}
