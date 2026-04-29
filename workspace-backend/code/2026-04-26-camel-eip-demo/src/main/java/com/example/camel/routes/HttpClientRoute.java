package com.example.camel.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * EIP showcase #7: HTTP Client with Retry + Circuit Breaker
 *
 * <p>Calling external REST APIs is one of the most common Camel use cases.
 * This route shows production-grade patterns: retry with exponential
 * backoff, circuit breaker (Camel + Resilience4j), timeout management,
 * and graceful degradation.</p>
 *
 * <p><b>Interview talking point:</b></p>
 * <blockquote>
 * "External API calls are the #1 source of cascading failures in
 * microservices. Camel's circuit breaker integration with Resilience4j
 * gives you fail-fast behavior when downstreams degrade — instead of
 * piling up timeouts that exhaust thread pools, the circuit opens and
 * we route to a fallback (cached response, default value, or graceful
 * error). This prevents 'gray failure' where the system is technically
 * up but unresponsive."
 * </blockquote>
 *
 * <p><b>Three layers of protection demonstrated:</b></p>
 * <ol>
 *   <li><b>Timeout</b> — fail fast, don't hold threads</li>
 *   <li><b>Retry with backoff</b> — handle transient failures</li>
 *   <li><b>Circuit breaker</b> — protect against systemic failures</li>
 * </ol>
 */
@Component
public class HttpClientRoute extends RouteBuilder {

    @Override
    public void configure() {

        // ─── Onception handling per route ───────────────────────────────
        // Different retry policy for HTTP vs default DLQ
        onException(java.io.IOException.class, java.net.SocketTimeoutException.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(500)
                .backOffMultiplier(2.0)
                .maximumRedeliveryDelay(5000)
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        // ─── Main route: enrich an order via external pricing API ────────
        from("direct:fetch-pricing")
                .routeId("http-client-pricing")
                .log("Fetching pricing for order: ${header.orderId}")

                // ─── EIP: Circuit Breaker (Resilience4j) ────────────────
                .circuitBreaker()
                    .resilience4jConfiguration()
                        .timeoutEnabled(true)
                        .timeoutDuration(2000)              // 2s per call
                        .slidingWindowSize(20)              // last 20 calls evaluated
                        .failureRateThreshold(50.0f)        // open if 50% fail
                        .waitDurationInOpenState(10_000)    // 10s before half-open
                        .permittedNumberOfCallsInHalfOpenState(3)
                    .end()

                    // Primary path — call external API
                    .setHeader("CamelHttpMethod", constant("GET"))
                    .setHeader("Accept", constant("application/json"))
                    .toD("https://api.pricing-service.example.com/v1/quote/${header.orderId}"
                            + "?httpClient.connectionTimeout=2000"
                            + "&httpClient.socketTimeout=2000"
                            + "&throwExceptionOnFailure=true")
                    .log("✓ Pricing fetched: ${body}")

                .onFallback()
                    // Circuit OPEN → fallback path
                    .log(LoggingLevel.WARN, "Circuit OPEN — using cached pricing fallback")
                    .setBody(constant("{\"price\":0.00,\"source\":\"fallback-cache\"}"))
                .end();
    }
}
