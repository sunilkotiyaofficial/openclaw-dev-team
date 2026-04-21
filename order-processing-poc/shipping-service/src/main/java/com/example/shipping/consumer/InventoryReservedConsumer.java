package com.example.shipping.consumer;

import com.example.events.envelope.EventEnvelope;
import com.example.events.payload.InventoryReservedPayload;
import com.example.events.payload.OrderCreatedPayload;
import com.example.kafka.topics.KafkaConsumerGroups;
import com.example.kafka.topics.KafkaTopicNames;
import com.example.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReservedConsumer {

    private final ShippingService shippingService;

    @KafkaListener(
            topics = KafkaTopicNames.ORDER_LIFECYCLE,
            groupId = KafkaConsumerGroups.SHIPPING_SERVICE + "-address-cache",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void cacheOrderAddress(EventEnvelope<?> envelope, Acknowledgment ack) {
        if ("OrderCreated".equals(envelope.eventType())) {
            OrderCreatedPayload p = (OrderCreatedPayload) envelope.payload();
            shippingService.cacheShippingAddress(p.orderId(), p.shippingAddress());
        }
        ack.acknowledge();
    }

    @KafkaListener(
            topics = KafkaTopicNames.INVENTORY_EVENTS,
            groupId = KafkaConsumerGroups.SHIPPING_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInventoryEvent(EventEnvelope<?> envelope, Acknowledgment ack) {
        if (!"InventoryReserved".equals(envelope.eventType())) {
            ack.acknowledge();
            return;
        }

        InventoryReservedPayload p = (InventoryReservedPayload) envelope.payload();
        log.info("ShippingService: scheduling shipment for [orderId={}]", p.orderId());

        shippingService.scheduleShipping(envelope, p)
                .doOnSuccess(v -> log.info("Shipment scheduled [orderId={}]", p.orderId()))
                .doOnError(e -> log.error("Shipment scheduling failed [orderId={}]: {}", p.orderId(), e.getMessage()))
                .subscribe();

        ack.acknowledge();
    }
}
