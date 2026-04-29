package com.example.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.spi.IdempotentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * EIP showcase #11: Idempotent Consumer
 *
 * <p>Filters duplicate messages by tracking message identifiers in a
 * repository. Critical for at-least-once delivery systems (Kafka, JMS)
 * where duplicates ARE expected, not exceptional.</p>
 *
 * <p><b>Repository options (production trade-offs):</b></p>
 * <table>
 *   <tr><th>Repository</th><th>When to use</th><th>Trade-off</th></tr>
 *   <tr>
 *     <td>MemoryIdempotentRepository</td>
 *     <td>Single-node dev/test</td>
 *     <td>Lost on restart, doesn't scale</td>
 *   </tr>
 *   <tr>
 *     <td>JdbcMessageIdRepository</td>
 *     <td>Persistent, single source of truth</td>
 *     <td>DB load on every message, latency</td>
 *   </tr>
 *   <tr>
 *     <td>RedisIdempotentRepository</td>
 *     <td>Distributed, fast, with TTL</td>
 *     <td>Need Redis ops, eventual consistency edge cases</td>
 *   </tr>
 *   <tr>
 *     <td>HazelcastIdempotentRepository</td>
 *     <td>In-memory cluster, no extra infra</td>
 *     <td>Memory pressure, partition tolerance</td>
 *   </tr>
 *   <tr>
 *     <td>CaffeineIdempotentRepository</td>
 *     <td>Single-node high-throughput</td>
 *     <td>Lost on restart, single-node only</td>
 *   </tr>
 * </table>
 *
 * <p><b>Interview talking point:</b></p>
 * <blockquote>
 * "At-least-once delivery + Idempotent Consumer = effectively exactly-once
 * processing. The choice of repository depends on scale and consistency
 * requirements. For most production: Redis with a sensible TTL (24h),
 * keyed on a business identifier like orderId or correlationId — never
 * on the message body hash, because identical content can be a legitimate
 * retry of a duplicate-tolerant operation."
 * </blockquote>
 */
@Component
public class IdempotentConsumerRoute extends RouteBuilder {

    @Override
    public void configure() {

        from("kafka:incoming-payments"
                + "?brokers={{spring.kafka.bootstrap-servers}}"
                + "&groupId=payment-processor")
                .routeId("idempotent-payment-processor")
                .log("Received payment: ${header.kafka.KEY}")

                // ─── EIP: Idempotent Consumer ───────────────────────────
                .idempotentConsumer(
                        header("paymentTransactionId"),     // KEY: business identifier
                        new MemoryIdempotentRepository())    // STORE: replace with Redis in prod
                    .skipDuplicate(true)                      // silently skip dupes (vs throwing)
                    .removeOnFailure(true)                    // remove key if processing fails — allows retry

                    // Only NEW messages reach here; duplicates are dropped
                    .log("Processing NEW payment: ${header.paymentTransactionId}")
                    .to("direct:process-payment")

                .end();

        // Payment business logic
        from("direct:process-payment")
                .routeId("payment-business-logic")
                .log("Charging payment...")
                // In real life: call payment gateway, update DB, emit event
                .delay(50);
    }
}

/**
 * Bean configuration for production-grade idempotent repositories.
 *
 * <p>Demo uses {@code MemoryIdempotentRepository}; in production swap
 * for Redis or JDBC variant via Spring profile.</p>
 */
@Configuration
class IdempotentRepoConfig {

    @Bean
    public IdempotentRepository idempotentRepository() {
        // Production: use RedisIdempotentRepository with 24h TTL
        // For demo: in-memory, max 1000 entries
        return MemoryIdempotentRepository.memoryIdempotentRepository(1000);
    }
}
