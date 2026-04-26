package com.example.inventory.service;

import com.example.events.envelope.EventEnvelope;
import com.example.events.envelope.EventType;
import com.example.events.payload.InventoryDepletedPayload;
import com.example.events.payload.InventoryReservedPayload;
import com.example.events.payload.OrderCreatedPayload;
import com.example.kafka.producer.EventPublisher;
import com.example.kafka.topics.KafkaTopicNames;
import com.example.inventory.model.InventoryItem;
import com.example.inventory.model.Reservation;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final ReactiveMongoTemplate mongoTemplate;
    private final EventPublisher eventPublisher;

    /** Attempt to reserve stock for all items in the order. Optimistic locking via version field. */
    public Mono<Void> reserveInventory(EventEnvelope<?> causingEnvelope, String orderId,
                                        List<OrderCreatedPayload.OrderItem> orderItems) {

        // Check if already reserved (idempotency)
        return reservationRepository.findByOrderId(orderId)
                .flatMap(existing -> {
                    log.info("Reservation already exists for order [orderId={}], skipping", orderId);
                    return Mono.<Void>empty();
                })
                .switchIfEmpty(
                    checkAndReserve(causingEnvelope, orderId, orderItems)
                );
    }

    private Mono<Void> checkAndReserve(EventEnvelope<?> cause, String orderId,
                                        List<OrderCreatedPayload.OrderItem> orderItems) {

        // Check all SKUs have sufficient stock
        List<Mono<InventoryItem>> stockChecks = orderItems.stream()
                .map(item -> inventoryRepository.findById(item.skuId())
                        .switchIfEmpty(Mono.error(new RuntimeException("SKU not found: " + item.skuId()))))
                .toList();

        return Flux.merge(stockChecks)
                .collectList()
                .flatMap(inventoryItems -> {
                    // Check for any depleted items
                    List<InventoryDepletedPayload.DepletedItem> depleted = new ArrayList<>();
                    for (var orderItem : orderItems) {
                        var invItem = inventoryItems.stream()
                                .filter(i -> i.getSkuId().equals(orderItem.skuId()))
                                .findFirst().orElseThrow();
                        if (invItem.getAvailableStock() < orderItem.quantity()) {
                            depleted.add(new InventoryDepletedPayload.DepletedItem(
                                    orderItem.skuId(), orderItem.quantity(), invItem.getAvailableStock()));
                        }
                    }

                    if (!depleted.isEmpty()) {
                        // Emit InventoryDepleted for compensation
                        var payload = new InventoryDepletedPayload(orderId, depleted, true);
                        var envelope = EventEnvelope.causedBy(cause, EventType.InventoryDepleted.name(), payload);
                        return Mono.fromFuture(
                                eventPublisher.publish(KafkaTopicNames.INVENTORY_EVENTS, orderId, envelope))
                                .then();
                    }

                    // All stock available — atomically reserve using optimistic locking
                    List<Mono<InventoryItem>> reservations = orderItems.stream().map(orderItem -> {
                        Query q = Query.query(Criteria.where("_id").is(orderItem.skuId())
                                .and("availableStock").gte(orderItem.quantity()));
                        Update u = new Update()
                                .inc("availableStock", -orderItem.quantity())
                                .inc("reservedStock", orderItem.quantity())
                                .set("updatedAt", Instant.now());
                        return mongoTemplate.findAndModify(q, u,
                                FindAndModifyOptions.options().returnNew(true), InventoryItem.class);
                    }).toList();

                    return Flux.merge(reservations)
                            .collectList()
                            .flatMap(reserved -> {
                                String reservationId = "res_" + UUID.randomUUID().toString().replace("-","").substring(0,12);

                                // Build reservation document
                                Reservation reservation = new Reservation();
                                reservation.setId(reservationId);
                                reservation.setOrderId(orderId);
                                reservation.setItems(orderItems.stream()
                                        .map(i -> new Reservation.ReservedItem(i.skuId(), i.quantity()))
                                        .toList());
                                reservation.setExpiresAt(Instant.now().plusSeconds(1800)); // 30 min TTL

                                return reservationRepository.save(reservation)
                                        .flatMap(savedRes -> {
                                            var items = reserved.stream()
                                                    .map(inv -> new InventoryReservedPayload.ReservedItem(
                                                            inv.getSkuId(),
                                                            orderItems.stream().filter(oi -> oi.skuId().equals(inv.getSkuId()))
                                                                    .findFirst().get().quantity(),
                                                            inv.getWarehouseId()))
                                                    .toList();
                                            var payload = new InventoryReservedPayload(
                                                    orderId, reservationId, items, savedRes.getExpiresAt());
                                            var envelope = EventEnvelope.causedBy(cause,
                                                    EventType.InventoryReserved.name(), payload);
                                            return Mono.fromFuture(
                                                    eventPublisher.publish(KafkaTopicNames.INVENTORY_EVENTS, orderId, envelope))
                                                    .then();
                                        });
                            });
                });
    }

    public Flux<InventoryItem> getAllInventory() {
        return inventoryRepository.findAll();
    }

    public Mono<InventoryItem> getInventory(String skuId) {
        return inventoryRepository.findById(skuId);
    }
}
