package com.example.order.consumer;

import com.example.events.envelope.EventEnvelope;
import com.example.events.payload.*;
import com.example.kafka.topics.KafkaConsumerGroups;
import com.example.kafka.topics.KafkaTopicNames;
import com.example.order.model.OrderStatus;
import com.example.order.service.OrderService;
import com.example.order.service.OrderService.SagaStateUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Listens to all saga events that affect order status.
 * order-service is the authoritative state machine owner.
 *
 * NOTE: For Phase 1 (POC), we use simple @KafkaListener with inline idempotency guard.
 * Phase 2 will refactor to IdempotentEventHandler base class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = KafkaTopicNames.PAYMENT_EVENTS,
            groupId = KafkaConsumerGroups.ORDER_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentEvent(EventEnvelope<?> envelope, Acknowledgment ack) {
        String eventType = envelope.eventType();
        log.info("OrderService received payment event [eventType={}, eventId={}]", eventType, envelope.eventId());

        switch (eventType) {
            case "PaymentCompleted" -> {
                PaymentCompletedPayload p = (PaymentCompletedPayload) envelope.payload();
                orderService.updateSagaState(p.orderId(), OrderStatus.PAYMENT_PROCESSING,
                        new SagaStateUpdate(p.paymentAttemptId(), p.transactionId(), null, null, null, null))
                        .subscribe();
            }
            case "PaymentFailed" -> {
                PaymentFailedPayload p = (PaymentFailedPayload) envelope.payload();
                orderService.updateSagaState(p.orderId(), OrderStatus.CANCELLED,
                        new SagaStateUpdate(p.paymentAttemptId(), null, null, null, null, p.failureReason()))
                        .subscribe();
            }
            case "PaymentRefunded" -> {
                // Already cancelled — just log for audit
                log.info("Payment refunded for order (already cancelled) [event={}]", envelope.eventId());
            }
        }
        ack.acknowledge();
    }

    @KafkaListener(
            topics = KafkaTopicNames.INVENTORY_EVENTS,
            groupId = KafkaConsumerGroups.ORDER_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInventoryEvent(EventEnvelope<?> envelope, Acknowledgment ack) {
        String eventType = envelope.eventType();
        log.info("OrderService received inventory event [eventType={}, eventId={}]", eventType, envelope.eventId());

        switch (eventType) {
            case "InventoryReserved" -> {
                InventoryReservedPayload p = (InventoryReservedPayload) envelope.payload();
                orderService.updateSagaState(p.orderId(), OrderStatus.INVENTORY_RESERVING,
                        new SagaStateUpdate(null, null, p.reservationId(), null, null, null))
                        .subscribe();
            }
            case "InventoryDepleted" -> {
                InventoryDepletedPayload p = (InventoryDepletedPayload) envelope.payload();
                orderService.updateSagaState(p.orderId(), OrderStatus.INVENTORY_FAILED,
                        new SagaStateUpdate(null, null, null, null, null, "Inventory depleted for order"))
                        .subscribe();
            }
        }
        ack.acknowledge();
    }

    @KafkaListener(
            topics = KafkaTopicNames.SHIPPING_EVENTS,
            groupId = KafkaConsumerGroups.ORDER_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onShippingEvent(EventEnvelope<?> envelope, Acknowledgment ack) {
        String eventType = envelope.eventType();
        log.info("OrderService received shipping event [eventType={}, eventId={}]", eventType, envelope.eventId());

        if ("ShippingScheduled".equals(eventType)) {
            ShippingScheduledPayload p = (ShippingScheduledPayload) envelope.payload();
            orderService.updateSagaState(p.orderId(), OrderStatus.SHIPPING_SCHEDULED,
                    new SagaStateUpdate(null, null, null, p.shipmentId(), p.trackingNumber(), null))
                    .subscribe();
        }
        ack.acknowledge();
    }
}
