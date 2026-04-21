package com.example.order.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "orders")
@CompoundIndexes({
    @CompoundIndex(name = "customer_date", def = "{'customerId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "status_date",   def = "{'status': 1, 'createdAt': -1}")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Order {

    @Id
    private String id;

    private String customerId;
    private OrderStatus status = OrderStatus.PENDING;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private String currency = "USD";
    private ShippingAddress shippingAddress;
    private String paymentMethodId;
    private SagaState sagaState = new SagaState();

    @Indexed(unique = true, sparse = true)
    private String idempotencyKey;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    @Data
    public static class OrderItem {
        private String skuId;
        private int quantity;
        private BigDecimal unitPrice;
        private String name;
    }

    @Data
    public static class ShippingAddress {
        private String street;
        private String city;
        private String state;
        private String zip;
        private String country;
    }

    @Data
    public static class SagaState {
        private String paymentAttemptId;
        private String transactionId;
        private String reservationId;
        private String shipmentId;
        private String trackingNumber;
        private String failureReason;
    }
}
