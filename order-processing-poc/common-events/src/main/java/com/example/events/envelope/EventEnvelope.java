package com.example.events.envelope;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Generic envelope wrapping all domain events.
 * Carries correlation/causation IDs for distributed tracing across the saga.
 */
public record EventEnvelope<T>(
        String eventId,        // UUID v4 — unique per event
        String eventType,      // EventType enum name
        String schemaVersion,  // "1.0" — increment on breaking changes
        Instant occurredAt,
        String correlationId,  // Traces the entire saga (set at OrderCreated, propagated)
        String causationId,    // ID of the event that caused this one
        T payload
) {
    @JsonCreator
    public EventEnvelope(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("schemaVersion") String schemaVersion,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("causationId") String causationId,
            @JsonProperty("payload") T payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.schemaVersion = schemaVersion;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.payload = payload;
    }

    /** Factory — creates a root event (no causation). */
    public static <T> EventEnvelope<T> root(String eventType, String correlationId, T payload) {
        return new EventEnvelope<>(
                java.util.UUID.randomUUID().toString(),
                eventType,
                "1.0",
                Instant.now(),
                correlationId,
                null,
                payload
        );
    }

    /** Factory — creates a caused-by event (carries parent eventId as causationId). */
    public static <T> EventEnvelope<T> causedBy(EventEnvelope<?> cause, String eventType, T payload) {
        return new EventEnvelope<>(
                java.util.UUID.randomUUID().toString(),
                eventType,
                "1.0",
                Instant.now(),
                cause.correlationId(),
                cause.eventId(),
                payload
        );
    }
}
