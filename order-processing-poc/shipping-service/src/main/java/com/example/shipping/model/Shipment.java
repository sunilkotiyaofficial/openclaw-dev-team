package com.example.shipping.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Document(collection = "shipments")
public class Shipment {
    @Id private String id;
    @Indexed(unique = true) private String orderId;
    private ShipmentStatus status = ShipmentStatus.SCHEDULED;
    private String carrier;
    @Indexed(sparse = true) private String trackingNumber;
    private String pickupWarehouseId;
    private ShippingAddress shippingAddress;
    private LocalDate estimatedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private Instant cancelledAt;
    private Instant createdAt = Instant.now();

    @Data
    public static class ShippingAddress {
        private String street, city, state, zip, country;
    }
    public enum ShipmentStatus { SCHEDULED, IN_TRANSIT, DELIVERED, CANCELLED }
}
