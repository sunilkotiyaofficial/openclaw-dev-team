package com.example.inventory.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "reservations")
public class Reservation {
    @Id private String id;
    @Indexed(unique = true) private String orderId;
    private List<ReservedItem> items;
    private ReservationStatus status = ReservationStatus.ACTIVE;
    private Instant expiresAt;   // TTL — MongoDB TTL index will auto-expire
    private Instant createdAt = Instant.now();

    public record ReservedItem(String skuId, int quantity) {}
    public enum ReservationStatus { ACTIVE, CONFIRMED, RELEASED, EXPIRED }
}
