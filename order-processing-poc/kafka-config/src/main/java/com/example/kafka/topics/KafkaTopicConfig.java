package com.example.kafka.topics;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Creates all Kafka topics on startup if they don't already exist.
 * Replication factor is 1 for dev (override to 3 in prod profile via kafka.topics.replication-factor=3).
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.replication-factor:1}")
    private int replicationFactor;

    @Bean public NewTopic orderLifecycleTopic() {
        return TopicBuilder.name(KafkaTopicNames.ORDER_LIFECYCLE)
                .partitions(12).replicas(replicationFactor)
                .config("retention.ms", "604800000").build();
    }

    @Bean public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(KafkaTopicNames.PAYMENT_EVENTS)
                .partitions(12).replicas(replicationFactor)
                .config("retention.ms", "604800000").build();
    }

    @Bean public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(KafkaTopicNames.INVENTORY_EVENTS)
                .partitions(6).replicas(replicationFactor)
                .config("retention.ms", "604800000").build();
    }

    @Bean public NewTopic shippingEventsTopic() {
        return TopicBuilder.name(KafkaTopicNames.SHIPPING_EVENTS)
                .partitions(6).replicas(replicationFactor)
                .config("retention.ms", "604800000").build();
    }

    @Bean public NewTopic orderDlqTopic() {
        return TopicBuilder.name(KafkaTopicNames.ORDER_DLQ)
                .partitions(3).replicas(replicationFactor)
                .config("retention.ms", "2592000000").build();
    }

    @Bean public NewTopic orderAuditTopic() {
        return TopicBuilder.name(KafkaTopicNames.ORDER_AUDIT)
                .partitions(6).replicas(replicationFactor)
                .config("retention.ms", "7776000000")
                .config("cleanup.policy", "compact").build();
    }
}
