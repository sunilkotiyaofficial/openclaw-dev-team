package com.example.camel.processor;

import com.example.camel.model.Order;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * EIP — Content Enricher pattern.
 *
 * <p>Adds data to a message from a "real" external source (this demo uses a
 * lookup map for clarity; in production this would be a REST call,
 * cache lookup, or DB query).</p>
 *
 * <p><b>Interview talking point:</b> Camel's
 * {@code .enrich(uri, AggregationStrategy)} DSL is preferred when the
 * enrichment requires another route call. This {@code Processor} pattern
 * is for in-memory enrichments — keeps the main route DSL clean.</p>
 */
@Component
public class EnrichmentProcessor implements Processor {

    /** Demo data — in prod this would be a customer service / CRM lookup. */
    private static final Map<String, String> CUSTOMER_TIERS = Map.of(
            "cust-42", "GOLD",
            "cust-99", "SILVER"
    );

    @Override
    public void process(Exchange exchange) {
        Order original = exchange.getIn().getBody(Order.class);
        String tier = CUSTOMER_TIERS.getOrDefault(original.customerId(), "STANDARD");

        Order enriched = new Order(
                original.orderId(),
                original.customerId(),
                original.amount(),
                original.region(),
                original.timestamp(),
                "tier=" + tier
        );

        exchange.getIn().setBody(enriched);
        exchange.getIn().setHeader("CustomerTier", tier);
    }
}
