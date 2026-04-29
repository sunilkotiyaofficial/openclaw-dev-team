package com.example.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * EIP showcase #8: JDBC Component — Database Integration with Polling
 *
 * <p>Two patterns in one route:</p>
 * <ol>
 *   <li><b>Polling consumer</b> — query DB on schedule, emit each row as a message</li>
 *   <li><b>Database write</b> — insert messages into DB with retry</li>
 * </ol>
 *
 * <p><b>Real-world use case:</b> Legacy ETL where a partner FTPs CSVs to
 * a staging DB, and we need to pick up new rows and publish to Kafka.
 * The "modern" approach is Kafka Connect, but for one-off integrations
 * Camel JDBC is faster to ship.</p>
 *
 * <p><b>Interview talking point:</b></p>
 * <blockquote>
 * "Camel JDBC has its place — quick wins for legacy DB-to-stream
 * integrations. For high-volume CDC, I'd reach for Debezium or Kafka
 * Connect; Camel JDBC is simpler to wire up and easier to own when
 * the team isn't already running a CDC pipeline."
 * </blockquote>
 */
@Component
public class JdbcCrudRoute extends RouteBuilder {

    @Override
    public void configure() {

        // ─── Pattern 1: Poll DB for new orders, publish to Kafka ─────────
        from("timer:poll-new-orders?period=10000")  // every 10s
                .routeId("jdbc-poll-new-orders")
                .log("Polling DB for new orders...")

                .setBody(constant(
                        "SELECT id, customer_id, amount, region "
                        + "FROM orders_staging "
                        + "WHERE status = 'NEW' "
                        + "LIMIT 100"))
                .to("jdbc:dataSource")  // executes query

                // Camel returns rows as List<Map<String, Object>>
                .split(body())
                    .marshal().json()
                    .to("kafka:orders-from-staging"
                            + "?brokers={{spring.kafka.bootstrap-servers}}"
                            + "&enableIdempotence=true&acks=all")
                .end()

                // Mark all picked rows as PROCESSED
                .setBody(constant(
                        "UPDATE orders_staging "
                        + "SET status = 'PROCESSED', processed_at = NOW() "
                        + "WHERE status = 'NEW' "
                        + "AND id IN (SELECT id FROM orders_staging "
                        + "             WHERE status = 'NEW' LIMIT 100)"))
                .to("jdbc:dataSource");

        // ─── Pattern 2: Audit log writes (called from other routes) ──────
        from("direct:audit-write")
                .routeId("jdbc-audit-write")
                .log("Writing audit entry: ${body}")
                // Use parameterized query to prevent SQL injection
                .toD("sql:INSERT INTO audit_log (event_type, payload, created_at) "
                        + "VALUES (:#${header.eventType}, :#${body}, NOW())"
                        + "?dataSource=#dataSource");
    }
}
