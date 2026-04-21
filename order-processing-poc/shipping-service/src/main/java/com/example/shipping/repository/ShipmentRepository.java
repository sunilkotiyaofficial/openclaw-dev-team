package com.example.shipping.repository;
import com.example.shipping.model.Shipment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;
public interface ShipmentRepository extends ReactiveMongoRepository<Shipment, String> {
    Mono<Shipment> findByOrderId(String orderId);
    Mono<Shipment> findByTrackingNumber(String trackingNumber);
}
