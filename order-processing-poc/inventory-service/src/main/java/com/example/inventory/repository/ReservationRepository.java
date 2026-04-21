package com.example.inventory.repository;
import com.example.inventory.model.Reservation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;
public interface ReservationRepository extends ReactiveMongoRepository<Reservation, String> {
    Mono<Reservation> findByOrderId(String orderId);
}
