package com.example.camel.routes;

import com.example.camel.model.Order;
import com.example.camel.processor.EnrichmentProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * EIP showcase #1: REST DSL → Validation → Enrichment → Kafka
 *
 * <p>This single route replaces ~150 lines of Spring Boot code (a
 * {@code @RestController} + {@code @Service} + {@code @KafkaTemplate}
 * setup + error handling).</p>
 *
 * <p><b>Patterns demonstrated:</b></p>
 * <ul>
 *   <li>REST DSL with auto JSON marshalling</li>
 *   <li>Bean Validation as a Camel processor (declarative)</li>
 *   <li>Content Enricher (custom processor)</li>
 *   <li>Producer to Kafka with key partitioning by orderId</li>
 * </ul>
 *
 * <p><b>Interview talking point:</b> The REST DSL auto-generates an OpenAPI
 * spec at {@code /api-doc} — useful for API gateway integration and
 * Postman generation without manual annotations.</p>
 */
@Component
public class OrderIngestionRoute extends RouteBuilder {

    @Autowired
    private EnrichmentProcessor enrichmentProcessor;

    @Override
    public void configure() {

        // ─── REST DSL Configuration (one-time setup) ───────────────────────
        restConfiguration()
                .component("platform-http")
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true")
                .apiContextPath("/api-doc")
                .apiProperty("api.title", "Order Ingestion API")
                .apiProperty("api.version", "1.0");

        // ─── REST Endpoint Definition ──────────────────────────────────────
        rest("/api/orders")
                .post()
                .description("Submit a new order — routed via EIP pipeline")
                .consumes("application/json")
                .produces("application/json")
                .type(Order.class)
                .to("direct:order-ingestion-pipeline");

        // ─── EIP Pipeline ──────────────────────────────────────────────────
        from("direct:order-ingestion-pipeline")
                .routeId("order-ingestion")
                .log("Received order: ${body}")

                // Pattern: Validation (declarative via bean-validator component)
                .to("bean-validator://order")

                // Pattern: Content Enricher (custom processor)
                .process(enrichmentProcessor)

                // Pattern: Message Header for Kafka partitioning
                .setHeader("kafka.KEY", simple("${body.orderId}"))

                // Pattern: Producer to Kafka with idempotency
                // Topic: orders-validated
                // Partition key: orderId (ensures same order → same partition)
                .marshal().json()
                .to("kafka:orders-validated"
                        + "?brokers={{spring.kafka.bootstrap-servers}}"
                        + "&groupId=order-ingestion-producer"
                        // Idempotent producer — safe retries, no duplicates
                        + "&enableIdempotence=true"
                        + "&acks=all"
                        + "&maxInFlightRequest=5")
                .log("✓ Order ${body} published to Kafka");
    }
}
