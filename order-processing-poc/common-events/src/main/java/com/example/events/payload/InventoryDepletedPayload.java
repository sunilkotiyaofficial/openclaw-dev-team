package com.example.events.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InventoryDepletedPayload(
        @NotBlank String orderId,
        @NotEmpty List<DepletedItem> depletedItems,
        boolean compensationRequired
) {
    public record DepletedItem(
            @NotBlank String skuId,
            int requested,
            int available
    ) {}
}
