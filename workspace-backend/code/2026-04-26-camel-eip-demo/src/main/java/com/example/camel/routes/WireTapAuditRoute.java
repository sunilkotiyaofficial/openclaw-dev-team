package com.example.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * EIP showcase #9: Wire Tap (Non-Intrusive Audit Logging)
 *
 * <p>Wire Tap copies the message to a secondary route WITHOUT affecting
 * the primary flow. Used for audit logging, monitoring, and dual-write
 * scenarios where you don't want the secondary path to slow down or
 * fail the main pipeline.</p>
 *
 * <p><b>Real-world use case:</b></p>
 * <ul>
 *   <li>Audit log every order request for compliance (SOX, GDPR)</li>
 *   <li>Send copy to data lake for analytics</li>
 *   <li>Mirror traffic to a shadow service during migration</li>
 *   <li>Forward to monitoring/alerting pipeline</li>
 * </ul>
 *
 * <p><b>Critical distinction from Multicast:</b></p>
 * <ul>
 *   <li><b>Wire Tap</b>: async, fire-and-forget. Original flow continues immediately.</li>
 *   <li><b>Multicast</b>: synchronous, waits for all branches. Slower, allows aggregation.</li>
 * </ul>
 *
 * <p><b>Interview talking point:</b></p>
 * <blockquote>
 * "Wire Tap is the right pattern when secondary processing is
 * fire-and-forget — audit logs, metrics emission, async notifications.
 * If the audit DB is down, the order still processes; the audit failure
 * doesn't cascade. Compare to multicast which waits for all branches —
 * fine when you need aggregation, dangerous when one branch can hang."
 * </blockquote>
 */
@Component
public class WireTapAuditRoute extends RouteBuilder {

    @Override
    public void configure() {

        // Main order processing route with audit wire tap
        from("direct:order-with-audit")
                .routeId("order-with-audit")
                .log("Processing order with audit trail")

                // ─── EIP: Wire Tap ──────────────────────────────────────
                // Copy message to audit pipeline asynchronously.
                // Main flow continues immediately, audit happens in parallel.
                .wireTap("direct:audit-pipeline")
                    .copy()  // creates a deep copy (safer for mutating processors)
                .end()

                // Main processing continues — audit failures don't affect this
                .log("Main flow: continuing with order processing")
                .to("direct:order-business-logic");

        // Secondary audit route — runs in parallel, decoupled
        from("direct:audit-pipeline")
                .routeId("audit-pipeline")
                .setHeader("eventType", constant("ORDER_RECEIVED"))
                .marshal().json()

                // Could be: file, JDBC, Kafka, S3, Splunk, ELK, etc.
                .multicast()
                    // Branch A: write to audit DB
                    .to("direct:audit-write")
                    // Branch B: emit to compliance Kafka topic
                    .to("kafka:audit-events"
                            + "?brokers={{spring.kafka.bootstrap-servers}}")
                    // Branch C: stream to security analytics
                    .to("log:security-events?level=INFO")
                .end();

        // Stub for primary business logic
        from("direct:order-business-logic")
                .routeId("order-business-logic")
                .log("Business logic processed order: ${body}");
    }
}
