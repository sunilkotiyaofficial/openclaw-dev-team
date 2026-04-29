package com.example.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * EIP showcase #6: JMS → Kafka Bridge (legacy modernization)
 *
 * <p>One of the most common enterprise integration patterns: bridging
 * legacy JMS-based systems (IBM MQ, ActiveMQ, TIBCO EMS) into modern
 * Kafka-based event meshes. Camel makes this a 10-line route.</p>
 *
 * <p><b>Real-world scenario:</b> "We have 15 legacy services using IBM MQ.
 * The new platform is Kafka-based. We can't rewrite everything overnight."
 * Camel bridges old to new with NO code changes to the legacy systems —
 * they keep publishing to MQ; Camel routes mirror those messages to Kafka
 * for the modern consumers.</p>
 *
 * <p><b>Interview talking point:</b></p>
 * <blockquote>
 * "Strangler Fig pattern at the integration layer. Camel sits between
 * legacy MQ and modern Kafka, mirroring messages bidirectionally during
 * migration. Once all consumers move to Kafka, we drain the JMS side
 * and decommission. Zero downtime, zero rewrites — we did this for
 * 15 services over 6 months."
 * </blockquote>
 *
 * <p><b>Production notes:</b></p>
 * <ul>
 *   <li>Use {@code transacted()} for JMS consumer to ensure atomicity</li>
 *   <li>{@code preserveMessageQos=true} to map JMS priorities to Kafka headers</li>
 *   <li>Idempotency: include MQ message-id as Kafka header for de-dup</li>
 * </ul>
 */
@Component
public class JmsBridgeRoute extends RouteBuilder {

    @Override
    public void configure() {

        // ─── Direction 1: Legacy JMS → Modern Kafka ─────────────────────
        from("activemq:queue:legacy.orders.queue"
                + "?concurrentConsumers=5"
                + "&transacted=true")
                .routeId("jms-to-kafka-bridge")
                .log("Bridging legacy JMS → Kafka: ${body}")

                // Preserve JMS metadata as Kafka headers
                .setHeader("source-system", constant("LEGACY_MQ"))
                .setHeader("kafka.KEY", header("JMSMessageID"))
                .setHeader("legacy-jms-priority", header("JMSPriority"))
                .setHeader("legacy-jms-correlation-id", header("JMSCorrelationID"))

                // Publish to modern Kafka topic
                .to("kafka:legacy-orders-bridge"
                        + "?brokers={{spring.kafka.bootstrap-servers}}"
                        + "&enableIdempotence=true"
                        + "&acks=all");

        // ─── Direction 2: Modern Kafka → Legacy JMS (reverse bridge) ─────
        // For acknowledgments / replies that need to flow back to legacy
        from("kafka:legacy-orders-replies"
                + "?brokers={{spring.kafka.bootstrap-servers}}")
                .routeId("kafka-to-jms-bridge")
                .log("Bridging Kafka reply → legacy JMS: ${body}")

                // Set JMS reply queue / correlation
                .setHeader("JMSCorrelationID", header("kafka-correlation-id"))
                .to("activemq:queue:legacy.orders.replies");
    }
}
