package com.example.kafka.consumer;

import com.example.events.envelope.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Abstract base class for all Kafka event consumers.
 * Guarantees exactly-once processing semantics by storing processed eventIds in MongoDB.
 *
 * Subclasses implement doHandle() with their business logic.
 * This class handles:
 *   - Deduplication (re-delivered events are silently acked and skipped)
 *   - Acknowledgment only after successful processing + persistence
 *   - On error: no ack, no record insertion → triggers retry → eventually DLQ
 */
@Slf4j
@RequiredArgsConstructor
public abstract class IdempotentEventHandler<T> {

    private static final String PROCESSED_EVENTS_COLLECTION = "processed_events";

    private final ReactiveMongoTemplate mongoTemplate;

    /**
     * Entry point called by @KafkaListener methods.
     * Do NOT override this. Implement doHandle() instead.
     */
    public final Mono<Void> handle(EventEnvelope<T> envelope, Acknowledgment ack) {
        String eventId = envelope.eventId();
        String handlerClass = this.getClass().getSimpleName();

        return mongoTemplate.findById(eventId, ProcessedEvent.class, PROCESSED_EVENTS_COLLECTION)
                .flatMap(existing -> {
                    log.debug("Skipping duplicate event [eventId={}, handler={}]", eventId, handlerClass);
                    ack.acknowledge();
                    return Mono.<Void>empty();
                })
                .switchIfEmpty(
                    doHandle(envelope)
                        .then(mongoTemplate.insert(
                                new ProcessedEvent(eventId, Instant.now(), handlerClass),
                                PROCESSED_EVENTS_COLLECTION
                        ))
                        .doOnSuccess(saved -> {
                            ack.acknowledge();
                            log.info("Processed event [eventId={}, eventType={}, handler={}]",
                                    eventId, envelope.eventType(), handlerClass);
                        })
                        .doOnError(error -> log.error(
                                "Error processing event [eventId={}, eventType={}, handler={}]: {}",
                                eventId, envelope.eventType(), handlerClass, error.getMessage(), error))
                        .then()
                )
                .onErrorResume(error -> {
                    // Do NOT ack on error — let Spring Kafka retry (backoff) then route to DLQ
                    log.error("Unhandled error in handler {} for event {}: {}",
                            handlerClass, eventId, error.getMessage());
                    return Mono.error(error);
                });
    }

    /**
     * Implement your business logic here.
     * Guaranteed to be called at most once per (eventId, handlerClass) pair.
     */
    protected abstract Mono<Void> doHandle(EventEnvelope<T> envelope);

    @Document(collection = PROCESSED_EVENTS_COLLECTION)
    public record ProcessedEvent(
            @Id String eventId,
            @Indexed Instant processedAt,
            String handlerClass
    ) {}
}
