package com.example.shipping.service;

import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Mock carrier assignment for Phase 1 POC. */
@Component
public class MockCarrierService {
    private static final List<String> CARRIERS = List.of("UPS", "FEDEX", "USPS");
    private static final Random RNG = new Random();

    public String assignCarrier() {
        return CARRIERS.get(RNG.nextInt(CARRIERS.size()));
    }

    public String generateTrackingNumber(String carrier) {
        return switch (carrier) {
            case "UPS"   -> "1Z" + UUID.randomUUID().toString().replace("-","").substring(0,16).toUpperCase();
            case "FEDEX" -> UUID.randomUUID().toString().replace("-","").substring(0,15).toUpperCase();
            default      -> "9400" + UUID.randomUUID().toString().replace("-","").substring(0,16);
        };
    }

    public LocalDate estimateDelivery() {
        return LocalDate.now().plusDays(2 + RNG.nextInt(4)); // 2-5 business days
    }
}
