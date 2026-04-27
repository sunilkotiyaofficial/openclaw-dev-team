package com.example.camel.routes;

import com.example.camel.model.Order;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * EIP showcase #2: Content-Based Router (CBR)
 *
 * <p>Replaces if/else chains in service code with declarative routing.
 * The router examines message content and dispatches to different routes.</p>
 *
 * <p><b>Real-world parallel:</b> "Route high-value orders to manual review,
 * standard orders to auto-fulfillment, international orders to regional
 * fulfillment centers." Each branch is independently testable and
 * deployable without touching the others.</p>
 *
 * <p><b>Interview talking point:</b> CBR is the most common EIP in
 * production — every routing decision in a system maps to it. Compare
 * to a Spring {@code @Service} with switch-case logic: CBR is
 * declarative, observable (each branch has metrics), and you can
 * dynamically reload routes without restart.</p>
 */
@Component
public class ContentBasedRouterRoute extends RouteBuilder {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000.00");

    @Override
    public void configure() {

        from("kafka:orders-validated"
                + "?brokers={{spring.kafka.bootstrap-servers}}"
                + "&groupId=cbr-routing"
                // Manual ack pattern — only commit after successful routing
                + "&autoCommitEnable=false"
                + "&allowManualCommit=true")
                .routeId("content-based-router")
                .unmarshal().json(Order.class)
                .log("CBR evaluating order: ${body.orderId}, amount=${body.amount}")

                // ─── EIP: Content-Based Router ──────────────────────────
                .choice()
                    // Branch 1: High-value orders → manual review queue
                    .when(simple("${body.amount} > " + HIGH_VALUE_THRESHOLD))
                        .log("→ HIGH-VALUE: routing to manual review")
                        .to("direct:high-value-handler")

                    // Branch 2: International orders → regional fulfillment
                    .when(simple("${body.region} != 'US'"))
                        .log("→ INTERNATIONAL: routing to regional fulfillment")
                        .to("direct:international-handler")

                    // Branch 3: Default — standard fulfillment
                    .otherwise()
                        .log("→ STANDARD: auto-fulfillment")
                        .to("direct:standard-handler")
                .end()

                // Manual commit AFTER successful routing — exactly-once-ish semantics
                .process(exchange -> {
                    var manualCommit = exchange.getIn()
                            .getHeader("CamelKafkaManualCommit",
                                    org.apache.camel.component.kafka.consumer.support.classic.KafkaManualCommit.class);
                    if (manualCommit != null) {
                        manualCommit.commit();
                    }
                });

        // ─── Branch Endpoints (Stub Implementations) ─────────────────────
        // In production, each of these would be its own service/route
        // emitting to its own Kafka topic for the relevant downstream system.

        from("direct:high-value-handler")
                .routeId("high-value-handler")
                .marshal().json()
                .to("kafka:orders-high-value"
                        + "?brokers={{spring.kafka.bootstrap-servers}}"
                        + "&enableIdempotence=true&acks=all");

        from("direct:international-handler")
                .routeId("international-handler")
                .marshal().json()
                .to("kafka:orders-international"
                        + "?brokers={{spring.kafka.bootstrap-servers}}"
                        + "&enableIdempotence=true&acks=all");

        from("direct:standard-handler")
                .routeId("standard-handler")
                .marshal().json()
                .to("kafka:orders-standard"
                        + "?brokers={{spring.kafka.bootstrap-servers}}"
                        + "&enableIdempotence=true&acks=all");
    }
}
