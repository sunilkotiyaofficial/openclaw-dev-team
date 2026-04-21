package com.example.payment.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Document(collection = "payments")
public class PaymentAttempt {
    @Id private String id;          // paymentAttemptId
    private String orderId;
    private PaymentStatus status = PaymentStatus.PENDING;
    private BigDecimal amount;
    private String currency;
    private String paymentMethodId;
    private String transactionId;
    @Indexed(unique = true) private String idempotencyKey;
    private String failureReason;
    private int retryCount = 0;
    private String refundId;
    private Instant createdAt = Instant.now();
    private Instant processedAt;
}
