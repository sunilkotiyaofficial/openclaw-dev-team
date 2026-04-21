package com.example.inventory.config;

import com.example.inventory.model.InventoryItem;
import com.example.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds 10 test SKUs on startup if inventory is empty.
 * Phase 1 POC only.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryDataSeeder {

    private final InventoryRepository inventoryRepository;

    private static final List<Object[]> SEED = List.of(
        new Object[]{"SKU-001","Widget A",100,"WH-Austin"},
        new Object[]{"SKU-002","Widget B",80,"WH-Austin"},
        new Object[]{"SKU-003","Gadget Pro",50,"WH-Austin"},
        new Object[]{"SKU-004","Connector Kit",200,"WH-Austin"},
        new Object[]{"SKU-005","Power Adapter",150,"WH-Austin"},
        new Object[]{"SKU-006","USB-C Cable",300,"WH-Austin"},
        new Object[]{"SKU-007","Stand Pro",40,"WH-Austin"},
        new Object[]{"SKU-008","Keyboard Mech",30,"WH-Austin"},
        new Object[]{"SKU-009","Mouse Wireless",60,"WH-Austin"},
        new Object[]{"SKU-010","Monitor 27in",15,"WH-Austin"}
    );

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        inventoryRepository.count()
            .filter(c -> c == 0)
            .flatMapMany(c -> {
                log.info("Seeding {} inventory SKUs", SEED.size());
                return inventoryRepository.saveAll(SEED.stream().map(d -> {
                    InventoryItem i = new InventoryItem();
                    i.setSkuId((String)d[0]); i.setName((String)d[1]);
                    i.setTotalStock((int)d[2]); i.setAvailableStock((int)d[2]);
                    i.setReservedStock(0); i.setWarehouseId((String)d[3]);
                    return i;
                }).toList());
            })
            .subscribe(i -> {}, e -> log.error("Seed error: {}", e.getMessage()));
    }
}
