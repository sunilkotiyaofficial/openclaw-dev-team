package com.example.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * EIP showcase #10: Multicast vs Recipient List vs Routing Slip
 *
 * <p>Three patterns for one-to-many delivery — each with subtle but
 * important differences. Knowing when to pick which is a senior-level
 * interview signal.</p>
 *
 * <h2>Pattern Comparison</h2>
 *
 * <table>
 *   <tr><th>Pattern</th><th>Recipients</th><th>Order</th><th>Use case</th></tr>
 *   <tr>
 *     <td><b>Multicast</b></td>
 *     <td>Static (defined at route time)</td>
 *     <td>Parallel or sequential</td>
 *     <td>Send same message to N known systems</td>
 *   </tr>
 *   <tr>
 *     <td><b>Recipient List</b></td>
 *     <td>Dynamic (from message header)</td>
 *     <td>Parallel or sequential</td>
 *     <td>Recipients depend on message content</td>
 *   </tr>
 *   <tr>
 *     <td><b>Routing Slip</b></td>
 *     <td>Dynamic ordered list</td>
 *     <td>Sequential, output of one feeds next</td>
 *     <td>Pipeline of variable steps (workflow engine)</td>
 *   </tr>
 * </table>
 *
 * <p><b>Interview talking point:</b></p>
 * <blockquote>
 * "Multicast is the wrong tool when recipients depend on message content
 * — that's Recipient List. Routing Slip is for variable-length pipelines
 * where each step's output feeds the next, like an approval workflow.
 * Picking the right pattern shows architectural maturity."
 * </blockquote>
 */
@Component
public class MulticastRecipientListRoute extends RouteBuilder {

    @Override
    public void configure() {

        // ═══ Pattern A: MULTICAST (static, parallel) ═════════════════════
        // Same message to ALL listed endpoints, in parallel
        from("direct:order-fanout-static")
                .routeId("multicast-static")
                .log("Multicasting order to all systems")

                .multicast()
                    .parallelProcessing()
                    .timeout(5000)
                    .aggregationStrategy((oldEx, newEx) -> oldEx == null ? newEx : oldEx)
                    .to(
                        "kafka:order-warehouse"
                            + "?brokers={{spring.kafka.bootstrap-servers}}",
                        "kafka:order-billing"
                            + "?brokers={{spring.kafka.bootstrap-servers}}",
                        "kafka:order-shipping"
                            + "?brokers={{spring.kafka.bootstrap-servers}}",
                        "kafka:order-analytics"
                            + "?brokers={{spring.kafka.bootstrap-servers}}"
                    )
                .end()
                .log("All static recipients delivered");

        // ═══ Pattern B: RECIPIENT LIST (dynamic, message-driven) ═════════
        // Recipients are computed from message content / header
        from("direct:order-fanout-dynamic")
                .routeId("recipient-list-dynamic")
                .log("Routing based on message content")

                // Header set upstream to "kafka:topicA,kafka:topicB"
                // Or computed via processor based on order content
                .recipientList(header("dynamicRecipients"))
                    .parallelProcessing()
                    .stopOnException()
                .end();

        // ═══ Pattern C: ROUTING SLIP (sequential workflow) ═══════════════
        // Each endpoint in slip processes message and passes to next
        from("direct:approval-workflow")
                .routeId("routing-slip-workflow")
                .log("Starting approval workflow")

                // Header has comma-separated list: "direct:check-credit,direct:check-fraud,direct:notify-customer"
                // Each step's output is the next step's input — like a pipeline
                .routingSlip(header("workflowSteps"))
                    .ignoreInvalidEndpoints()
                .end()
                .log("Workflow complete");

        // Workflow stub steps
        from("direct:check-credit")
                .log("Credit check: APPROVED for ${body}")
                .setHeader("creditApproved", constant(true));

        from("direct:check-fraud")
                .log("Fraud check: CLEAN for ${body}")
                .setHeader("fraudClean", constant(true));

        from("direct:notify-customer")
                .log("Customer notified: ${body}");
    }
}
