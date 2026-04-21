package com.example.inventory.consumer;

import com.example.events.envelope.EventEnvelope;
import com.example.events.payload.OrderCreatedPayload;
import com.example.events.payload.PaymentCompletedPayload;
import com.example.inventory.service.InventoryService;
import com.example.kafka.topics.KafkaConsumerGroups;
import com.example.kafka.topics.KafkaTopicNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Triggers inventory reservation when payment is confirmed.
 * NOTE: In a real system we'd carry the order items in the PaymentCompleted event
 * or look them up from order-service. For POC simplicity, we store a local cache
 * of order items from OrderCreated events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {

    private final InventoryService inventoryService;
    // Simple in-memory cache: orderId -> items (Phase 2: use local MongoDB collection)
    private final java.util.concurrent.ConcurrentHashMap<String, List<OrderCreatedPayload.OrderItem>>
            orderItemsCache = new java.util.concurrent.ConcurrentHashMap<>();

    @KafkaListener(
            topics = KafkaTopicNames.ORDER_LIFECYCLE,
            groupId = KafkaConsumerGroups.INVENTORY_SERVICE + "-order-cache",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void cacheOrderItems(EventEnvelope<?> envelope, Acknowledgment ack) {
        if ("OrderCreated".equals(envelope.eventType())) {
            OrderCreatedPayload p = (OrderCreatedPayload) envelope.payload();
            orderItemsCache.put(p.orderId(), p.items());
        }
        ack.acknowledge();
    }

    @KafkaListener(
            topics = KafkaTopicNames.PAYMENT_EVENTS,
            groupId = KafkaConsumerGroups.INVENTORY_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentEvent(EventEnvelope<?> envelope, Acknowledgment ack) {
        if (!"PaymentCompleted".equals(envelope.eventType())) {
            ack.acknowledge();
            return;
        }

        PaymentCompletedPayload p = (PaymentCompletedPayload) envelope.payload();
        List<OrderCreatedPayload.OrderItem> items = orderItemsCache.get(p.orderId());

        if (items == null) {
            log.warn("No cached order items for [orderId={}] — cannot reserve inventory", p.orderId());
            ack.acknowledge();
            return;
        }

        log.info("InventoryService: reserving stock for order [orderId={}]", p.orderId());
        inventoryService.reserveInventory(envelope, p.orderId(), items)
                .doOnSuccess(v -> log.info("Inventory reservation complete [orderId={}]", p.orderId()))
                .doOnError(e -> log.error("Inventory reservation failed [orderId={}]: {}", p.orderId(), e.getMessage()))
                .subscribe();

        ack.acknowledge();
    }
}
