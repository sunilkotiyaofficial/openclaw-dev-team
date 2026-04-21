package com.example.kafka.topics;

/** All Kafka topic name constants. Reference these everywhere — never hardcode topic strings. */
public final class KafkaTopicNames {
    private KafkaTopicNames() {}

    public static final String ORDER_LIFECYCLE = "order.lifecycle";
    public static final String PAYMENT_EVENTS  = "payment.events";
    public static final String INVENTORY_EVENTS = "inventory.events";
    public static final String SHIPPING_EVENTS  = "shipping.events";
    public static final String ORDER_DLQ        = "order.dlq";
    public static final String ORDER_AUDIT      = "order.audit";
}
