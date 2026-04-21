package com.example.payment.repository;
import com.example.payment.model.PaymentAttempt;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface PaymentRepository extends ReactiveMongoRepository<PaymentAttempt, String> {
    Flux<PaymentAttempt> findByOrderId(String orderId);
    Mono<PaymentAttempt> findByIdempotencyKey(String key);
}
