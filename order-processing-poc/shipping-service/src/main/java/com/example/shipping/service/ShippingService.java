package com.example.shipping.service;

import com.example.events.envelope.EventEnvelope;
import com.example.events.envelope.EventType;
import com.example.events.payload.InventoryReservedPayload;
import com.example.events.payload.OrderCreatedPayload;
import com.example.events.payload.ShippingScheduledPayload;
import com.example.kafka.producer.EventPublisher;
import com.example.kafka.topics.KafkaTopicNames;
import com.example.shipping.model.Shipment;
import com.example.shipping.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final MockCarrierService carrierService;
    private final EventPublisher eventPublisher;

    // Cache shipping addresses from OrderCreated (Phase 2: query order-service via API)
    private final ConcurrentHashMap<String, OrderCreatedPayload.ShippingAddress>
            addressCache = new ConcurrentHashMap<>();

    public void cacheShippingAddress(String orderId, OrderCreatedPayload.ShippingAddress address) {
        addressCache.put(orderId, address);
    }

    public Mono<Void> scheduleShipping(EventEnvelope<?> cause, InventoryReservedPayload reservation) {
        return shipmentRepository.findByOrderId(reservation.orderId())
                .flatMap(existing -> {
                    log.info("Shipment already exists for [orderId={}], skipping", reservation.orderId());
                    return Mono.<Void>empty();
                })
                .switchIfEmpty(createShipment(cause, reservation));
    }

    private Mono<Void> createShipment(EventEnvelope<?> cause, InventoryReservedPayload reservation) {
        String carrier = carrierService.assignCarrier();
        String trackingNumber = carrierService.generateTrackingNumber(carrier);
        String shipmentId = "ship_" + UUID.randomUUID().toString().replace("-","").substring(0,12);

        OrderCreatedPayload.ShippingAddress addr = addressCache.get(reservation.orderId());

        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrderId(reservation.orderId());
        shipment.setCarrier(carrier);
        shipment.setTrackingNumber(trackingNumber);
        shipment.setEstimatedDeliveryDate(carrierService.estimateDelivery());
        if (reservation.items() != null && !reservation.items().isEmpty()) {
            shipment.setPickupWarehouseId(reservation.items().get(0).warehouseId());
        }

        if (addr != null) {
            Shipment.ShippingAddress shipAddr = new Shipment.ShippingAddress();
            shipAddr.setStreet(addr.street());
            shipAddr.setCity(addr.city());
            shipAddr.setState(addr.state());
            shipAddr.setZip(addr.zip());
            shipAddr.setCountry(addr.country());
            shipment.setShippingAddress(shipAddr);
        }

        return shipmentRepository.save(shipment)
                .flatMap(saved -> {
                    ShippingScheduledPayload.ShippingAddress evtAddr = addr != null
                            ? new ShippingScheduledPayload.ShippingAddress(
                                    addr.street(), addr.city(), addr.state(), addr.zip(), addr.country())
                            : null;

                    // Use local ShippingAddress record - need to import from payload
                    var payloadAddr = new OrderCreatedPayload.ShippingAddress(
                        addr != null ? addr.street() : "",
                        addr != null ? addr.city() : "",
                        addr != null ? addr.state() : "",
                        addr != null ? addr.zip() : "",
                        addr != null ? addr.country() : "US"
                    );

                    var payload = new ShippingScheduledPayload(
                            reservation.orderId(), shipmentId, carrier, trackingNumber,
                            saved.getEstimatedDeliveryDate(),
                            saved.getPickupWarehouseId() != null ? saved.getPickupWarehouseId() : "WH-DEFAULT",
                            payloadAddr);
                    var envelope = EventEnvelope.causedBy(cause, EventType.ShippingScheduled.name(), payload);
                    return Mono.fromCompletableFuture(
                            eventPublisher.publish(KafkaTopicNames.SHIPPING_EVENTS, reservation.orderId(), envelope))
                            .then();
                });
    }

    public Mono<Shipment> getByOrderId(String orderId) {
        return shipmentRepository.findByOrderId(orderId);
    }

    public Mono<Shipment> getById(String shipmentId) {
        return shipmentRepository.findById(shipmentId);
    }
}
