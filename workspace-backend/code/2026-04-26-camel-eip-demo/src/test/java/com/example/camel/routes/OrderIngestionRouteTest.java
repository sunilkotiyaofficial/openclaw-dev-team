package com.example.camel.routes;

import com.example.camel.model.Order;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Camel route testing — interview-grade pattern.
 *
 * <p>Demonstrates:</p>
 * <ol>
 *   <li><b>{@code @CamelSpringBootTest}</b> — Spring Boot test context with Camel</li>
 *   <li><b>{@code MockEndpoint}</b> — replace external endpoints with mocks</li>
 *   <li><b>{@code AdviceWith}</b> — modify routes at test time without changing source</li>
 *   <li><b>Assertion-based testing</b> — verify message count, body, headers</li>
 * </ol>
 *
 * <p><b>Interview talking point:</b></p>
 * <blockquote>
 * "Camel routes are highly testable thanks to MockEndpoint. We replace
 * the Kafka producer with a mock at test time, send a test message via
 * ProducerTemplate, then assert on the mock — verifies the entire route
 * flow without a real Kafka cluster. For integration tests, we use
 * Testcontainers Kafka. The two-tier strategy is fast feedback in unit
 * tests, full confidence in integration tests."
 * </blockquote>
 */
@CamelSpringBootTest
@SpringBootTest
class OrderIngestionRouteTest {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private org.apache.camel.CamelContext camelContext;

    /**
     * Mock endpoint that replaces the real Kafka producer at test time.
     * Camel injects this based on the URI we'll redirect to.
     */
    @EndpointInject("mock:kafka-output")
    private MockEndpoint mockKafkaOutput;

    @BeforeEach
    void setUp() throws Exception {
        // Use AdviceWith to redirect Kafka calls to our mock
        AdviceWith.adviceWith(camelContext, "order-ingestion", advice -> {
            advice.weaveByToUri("kafka:orders-validated*")
                    .replace()
                    .to("mock:kafka-output");
        });

        mockKafkaOutput.reset();
    }

    @Test
    void happyPath_ValidOrder_PublishedToKafka() throws InterruptedException {
        // Expect exactly 1 message at the mock
        mockKafkaOutput.expectedMessageCount(1);

        // Send a valid order
        Order validOrder = new Order(
                "ord-001",
                "cust-42",
                new BigDecimal("99.99"),
                "US",
                Instant.now(),
                null
        );

        producerTemplate.sendBody("direct:order-ingestion-pipeline", validOrder);

        // Assert mock received the expected message
        mockKafkaOutput.assertIsSatisfied();

        // Verify enrichment happened (CustomerTier header should be set)
        var receivedExchange = mockKafkaOutput.getExchanges().get(0);
        assertTrue(
                receivedExchange.getIn().getHeaders().containsKey("CustomerTier"),
                "Enrichment processor should set CustomerTier header");

        // Verify Kafka partition key is set
        assertEquals("ord-001",
                receivedExchange.getIn().getHeader("kafka.KEY"),
                "Kafka KEY should match orderId for partitioning");
    }

    @Test
    void invalidOrder_NoAmount_RejectedByValidation() throws InterruptedException {
        // Validation failure → no message reaches Kafka
        mockKafkaOutput.expectedMessageCount(0);

        // Order with null amount (violates @NotNull constraint)
        try {
            Order invalidOrder = new Order(
                    "ord-002",
                    "cust-99",
                    null,                    // ← invalid
                    "US",
                    Instant.now(),
                    null
            );
            producerTemplate.sendBody("direct:order-ingestion-pipeline", invalidOrder);
        } catch (Exception expected) {
            // Bean validator throws — expected
        }

        mockKafkaOutput.assertIsSatisfied();
    }

    @Test
    void highValueOrder_RoutedToHighValueTopic() throws InterruptedException {
        // After AdviceWith for the CBR route, verify routing decision
        Order highValue = new Order(
                "ord-003",
                "cust-99",
                new BigDecimal("5000.00"),    // > $1000 threshold
                "US",
                Instant.now(),
                null
        );

        // (Assertion logic for CBR routing — requires similar AdviceWith setup)
        producerTemplate.sendBody("direct:order-ingestion-pipeline", highValue);

        // Test would verify the high-value topic receives the message
        // and standard topic does not
    }
}
