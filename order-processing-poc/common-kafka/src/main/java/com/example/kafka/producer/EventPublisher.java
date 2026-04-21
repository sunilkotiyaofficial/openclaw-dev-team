package com.example.kafka.producer;

import com.example.events.envelope.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Central publisher for all domain events.
 * Partitions by orderId (the partition key) to guarantee ordering per order.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;

    /**
     * Publish an event to a topic, partitioned by the given key (use orderId).
     *
     * @param topic        target Kafka topic
     * @param partitionKey partition key — MUST be orderId for saga ordering guarantees
     * @param event        the event envelope to publish
     * @return CompletableFuture that completes when the broker acknowledges
     */
    public CompletableFuture<SendResult<String, EventEnvelope<?>>> publish(
            String topic, String partitionKey, EventEnvelope<?> event) {

        return kafkaTemplate.send(topic, partitionKey, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published event [eventId={}, eventType={}, topic={}, partition={}, offset={}]",
                                event.eventId(), event.eventType(), topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish event [eventId={}, eventType={}, topic={}]: {}",
                                event.eventId(), event.eventType(), topic, ex.getMessage(), ex);
                        throw new RuntimeException("Failed to publish event: " + event.eventId(), ex);
                    }
                });
    }
}
