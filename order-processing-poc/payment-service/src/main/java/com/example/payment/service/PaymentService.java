package com.example.payment.service;

import com.example.events.envelope.EventEnvelope;
import com.example.events.envelope.EventType;
import com.example.events.payload.PaymentCompletedPayload;
import com.example.events.payload.PaymentFailedPayload;
import com.example.events.payload.PaymentRequestedPayload;
import com.example.kafka.producer.EventPublisher;
import com.example.kafka.topics.KafkaTopicNames;
import com.example.payment.model.PaymentAttempt;
import com.example.payment.model.PaymentStatus;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MockPaymentGateway gateway;
    private final EventPublisher eventPublisher;

    public Mono<PaymentAttempt> processPayment(EventEnvelope<?> causingEnvelope,
                                                PaymentRequestedPayload payload) {
        return paymentRepository.findByIdempotencyKey(payload.idempotencyKey())
                .switchIfEmpty(executePayment(causingEnvelope, payload));
    }

    private Mono<PaymentAttempt> executePayment(EventEnvelope<?> cause,
                                                 PaymentRequestedPayload payload) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setId(payload.paymentAttemptId());
        attempt.setOrderId(payload.orderId());
        attempt.setAmount(payload.amount());
        attempt.setCurrency(payload.currency());
        attempt.setPaymentMethodId(payload.paymentMethodId());
        attempt.setIdempotencyKey(payload.idempotencyKey());

        return paymentRepository.save(attempt)
                .flatMap(saved ->
                    gateway.charge(saved.getPaymentMethodId(), saved.getAmount(), saved.getIdempotencyKey())
                        .flatMap(result -> {
                            if (result.success()) {
                                saved.setStatus(PaymentStatus.COMPLETED);
                                saved.setTransactionId(result.transactionId());
                                saved.setProcessedAt(Instant.now());

                                var completedPayload = new PaymentCompletedPayload(
                                        saved.getOrderId(), saved.getId(),
                                        saved.getTransactionId(), saved.getAmount(),
                                        saved.getCurrency(), saved.getProcessedAt());
                                var envelope = EventEnvelope.causedBy(cause,
                                        EventType.PaymentCompleted.name(), completedPayload);

                                return paymentRepository.save(saved)
                                        .flatMap(s -> Mono.fromCompletableFuture(
                                                eventPublisher.publish(KafkaTopicNames.PAYMENT_EVENTS, s.getOrderId(), envelope))
                                                .thenReturn(s));
                            } else {
                                saved.setStatus(PaymentStatus.FAILED);
                                saved.setFailureReason(result.failureReason());
                                saved.setProcessedAt(Instant.now());

                                var failedPayload = new PaymentFailedPayload(
                                        saved.getOrderId(), saved.getId(),
                                        result.failureReason(), result.failureCode(), false, Instant.now());
                                var envelope = EventEnvelope.causedBy(cause,
                                        EventType.PaymentFailed.name(), failedPayload);

                                return paymentRepository.save(saved)
                                        .flatMap(s -> Mono.fromCompletableFuture(
                                                eventPublisher.publish(KafkaTopicNames.PAYMENT_EVENTS, s.getOrderId(), envelope))
                                                .thenReturn(s));
                            }
                        })
                );
    }

    public Flux<PaymentAttempt> getByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public Mono<PaymentAttempt> getById(String id) {
        return paymentRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Payment not found: " + id)));
    }
}
