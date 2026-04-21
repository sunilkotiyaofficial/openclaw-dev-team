package com.example.inventory.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "inventory")
public class InventoryItem {
    @Id private String skuId;
    private String name;
    private int totalStock;
    private int reservedStock;
    private int availableStock;
    private String warehouseId;
    @Version private Long version;  // Optimistic locking — prevents oversell
    private Instant updatedAt = Instant.now();
}
