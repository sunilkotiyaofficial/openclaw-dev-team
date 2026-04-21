package com.example.order.repository;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderRepository extends ReactiveMongoRepository<Order, String> {
    Flux<Order> findByCustomerId(String customerId, Pageable pageable);
    Flux<Order> findByStatus(OrderStatus status, Pageable pageable);
    Flux<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status, Pageable pageable);
    Mono<Order> findByIdempotencyKey(String idempotencyKey);
    Flux<Order> findAllBy(Pageable pageable);
}
