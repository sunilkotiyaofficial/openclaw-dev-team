package com.example.events.envelope;

/** All event types in the order processing system. */
public enum EventType {
    OrderCreated,
    OrderCompleted,
    OrderCancelled,
    OrderCompensated,
    PaymentRequested,
    PaymentCompleted,
    PaymentFailed,
    PaymentRefunded,
    InventoryReserved,
    InventoryDepleted,
    InventoryReleased,
    ShippingScheduled,
    ShippingFailed,
    ShippingDelivered
}
