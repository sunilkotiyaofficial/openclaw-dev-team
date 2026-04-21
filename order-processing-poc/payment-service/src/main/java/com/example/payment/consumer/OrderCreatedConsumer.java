package com.example.payment.consumer;

import com.example.events.envelope.EventEnvelope;
import com.example.events.envelope.EventType;
import com.example.events.payload.OrderCreatedPayload;
import com.example.events.payload.PaymentRequestedPayload;
import com.example.kafka.producer.EventPublisher;
import com.example.kafka.topics.KafkaConsumerGroups;
import com.example.kafka.topics.KafkaTopicNames;
import com.example.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes OrderCreated and kicks off payment processing.
 * Emits PaymentRequested immediately, then processes asynchronously.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final PaymentService paymentService;
    private final EventPublisher eventPublisher;

    @KafkaListener(
            topics = KafkaTopicNames.ORDER_LIFECYCLE,
            groupId = KafkaConsumerGroups.PAYMENT_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderLifecycleEvent(EventEnvelope<?> envelope, Acknowledgment ack) {
        if (!"OrderCreated".equals(envelope.eventType())) {
            ack.acknowledge();
            return;
        }

        OrderCreatedPayload order = (OrderCreatedPayload) envelope.payload();
        log.info("PaymentService: processing OrderCreated [orderId={}, amount={}]",
                order.orderId(), order.totalAmount());

        String attemptId = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String idempotencyKey = order.orderId() + "::attempt::1";

        var requestPayload = new PaymentRequestedPayload(
                order.orderId(), attemptId, order.totalAmount(),
                order.currency(), order.paymentMethodId(), idempotencyKey);

        // Emit PaymentRequested event (for audit trail)
        var requestEnvelope = EventEnvelope.causedBy(envelope, EventType.PaymentRequested.name(), requestPayload);
        eventPublisher.publish(KafkaTopicNames.PAYMENT_EVENTS, order.orderId(), requestEnvelope);

        // Process payment (will emit PaymentCompleted or PaymentFailed)
        paymentService.processPayment(envelope, requestPayload)
                .doOnSuccess(attempt -> log.info("Payment processed [orderId={}, status={}]",
                        order.orderId(), attempt.getStatus()))
                .doOnError(e -> log.error("Payment processing failed [orderId={}]: {}",
                        order.orderId(), e.getMessage()))
                .subscribe();

        ack.acknowledge();
    }
}
