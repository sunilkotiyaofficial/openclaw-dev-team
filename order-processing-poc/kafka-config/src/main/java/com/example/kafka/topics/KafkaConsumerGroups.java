package com.example.kafka.topics;

/** Consumer group ID constants. Each service owns exactly one group per topic it consumes. */
public final class KafkaConsumerGroups {
    private KafkaConsumerGroups() {}

    public static final String PAYMENT_SERVICE   = "payment-service";
    public static final String INVENTORY_SERVICE = "inventory-service";
    public static final String ORDER_SERVICE     = "order-service";
    public static final String SHIPPING_SERVICE  = "shipping-service";
    public static final String DASHBOARD_SERVICE = "order-dashboard-svc";
}
