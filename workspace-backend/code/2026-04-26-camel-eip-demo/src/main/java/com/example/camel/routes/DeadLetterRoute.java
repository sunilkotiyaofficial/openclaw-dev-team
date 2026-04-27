package com.example.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * EIP showcase #4: Dead Letter Channel + Retry with Exponential Backoff
 *
 * <p>Production-grade error handling. Every Camel route in production
 * MUST have an error handler. Without it, exceptions stall consumers
 * and fill the broker with retried-forever messages.</p>
 *
 * <p><b>Pattern:</b></p>
 * <ol>
 *   <li>First failure → wait 1s, retry</li>
 *   <li>Second failure → wait 2s, retry (exponential backoff x2)</li>
 *   <li>Third failure → wait 4s, retry</li>
 *   <li>Fourth failure → route to DLQ topic with full exception context</li>
 * </ol>
 *
 * <p><b>Interview talking point:</b> The DLQ message includes:
 * original payload, headers, exception message, stack trace,
 * route ID, and {@code CamelExchangeException} details. Operations team
 * can inspect, fix root cause, and replay messages from DLQ to
 * original topic. This is the foundation of "no message ever lost."</p>
 */
@Component
public class DeadLetterRoute extends RouteBuilder {

    @Override
    public void configure() {

        // ─── Global Error Handler — Dead Letter Channel ─────────────────
        // Applies to ALL routes unless they override
        errorHandler(deadLetterChannel("kafka:orders-dlq"
                        + "?brokers={{spring.kafka.bootstrap-servers}}"
                        + "&enableIdempotence=true"
                        + "&acks=all")
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)              // 1s base
                .backOffMultiplier(2.0)              // x2 each retry → 1s, 2s, 4s
                .maximumRedeliveryDelay(10_000)      // cap at 10s
                .retryAttemptedLogLevel(org.apache.camel.LoggingLevel.WARN)
                .logRetryStackTrace(true)
                .logExhaustedMessageBody(true)
                .useExponentialBackOff());

        // ─── Demo Route — Simulates Failures ────────────────────────────
        // Trigger via: producerTemplate.sendBody("direct:flaky-processor", payload)
        from("direct:flaky-processor")
                .routeId("flaky-processor")
                .log("Processing: ${body}")
                .process(exchange -> {
                    String body = exchange.getIn().getBody(String.class);
                    // Simulate transient failure on certain inputs
                    if (body != null && body.contains("FAIL")) {
                        throw new RuntimeException(
                                "Transient failure simulated for body: " + body);
                    }
                })
                .log("✓ Successfully processed: ${body}");

        // ─── Per-route override — Custom error handling for specific route ──
        // This route IGNORES the global error handler and uses its own logic.
        // Pattern: critical routes that should not retry (e.g., financial transfers)
        from("direct:no-retry-processor")
                .routeId("no-retry-processor")
                .errorHandler(noErrorHandler())  // disable retries
                .onException(Exception.class)
                    .handled(true)
                    .log("✗ Failed permanently — alerting ops")
                    // In production: emit to PagerDuty, log to ELK, etc.
                    .to("kafka:critical-failures"
                            + "?brokers={{spring.kafka.bootstrap-servers}}")
                .end()
                .process(exchange -> {
                    // Critical processing logic that should not retry
                });
    }
}
