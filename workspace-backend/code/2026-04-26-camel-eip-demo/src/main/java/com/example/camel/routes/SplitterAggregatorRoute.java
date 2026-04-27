package com.example.camel.routes;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * EIP showcase #3: Splitter + Aggregator
 *
 * <p>Classic pattern: receive a bulk message (list of orders), split into
 * individual messages, process each in parallel, then aggregate results
 * back into a single response.</p>
 *
 * <p><b>Use case:</b> A retail system receives a "batch upload" of 1,000
 * orders from a partner. Each order needs validation + enrichment + DB
 * write. Splitting allows parallel processing (10x faster). Aggregating
 * lets the caller see one consolidated response with success/failure counts.</p>
 *
 * <p><b>Interview talking point:</b> Splitter parallelism is configurable
 * with {@code .parallelProcessing()}; the executor service is
 * {@code @Autowired}-able from Spring. Aggregator's
 * {@code AggregationStrategy} is where the merge logic lives — separate from
 * the route, easy to unit test.</p>
 */
@Component
public class SplitterAggregatorRoute extends RouteBuilder {

    @Override
    public void configure() {

        // Endpoint receives a bulk batch (List<Order> via REST or Kafka)
        from("direct:bulk-orders")
                .routeId("bulk-order-processor")
                .log("Received bulk batch of ${body.size()} orders")

                // ─── EIP: Splitter ──────────────────────────────────────
                .split(body())                       // split each list element into separate exchange
                    .parallelProcessing()             // process branches in parallel
                    .streaming()                       // streaming mode — don't load all in memory
                    .stopOnException()                 // stop entire batch on first hard failure
                    .aggregationStrategy(new BatchResultAggregator())
                    .timeout(30_000)                   // 30s timeout per item

                    // Per-item processing
                    .log("→ processing item: ${body}")
                    .to("direct:process-single-order")

                .end()  // ← end of split

                // After split completes, aggregator has run — body is now the merged result
                .log("✓ Bulk batch processed: ${body}");

        // Each split branch routes here for individual processing
        from("direct:process-single-order")
                .routeId("single-order-processor")
                // In production: validation, enrichment, DB write, Kafka publish
                .delay(50)  // simulate processing time
                .setBody(simple("processed:${body.orderId}"));
    }

    /**
     * AggregationStrategy — defines how individual split results merge
     * back into one response.
     *
     * <p>Pattern: collect successes + failures into a structured summary.</p>
     */
    private static class BatchResultAggregator implements AggregationStrategy {

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            // First exchange in the aggregation
            if (oldExchange == null) {
                List<String> results = new ArrayList<>();
                results.add(newExchange.getIn().getBody(String.class));
                newExchange.getIn().setBody(results);
                return newExchange;
            }

            // Subsequent exchanges — append to running list
            @SuppressWarnings("unchecked")
            List<String> results = oldExchange.getIn().getBody(List.class);
            results.add(newExchange.getIn().getBody(String.class));
            return oldExchange;
        }
    }
}
