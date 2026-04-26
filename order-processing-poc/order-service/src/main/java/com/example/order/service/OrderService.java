package com.example.order.service;

import com.example.events.envelope.EventEnvelope;
import com.example.events.envelope.EventType;
import com.example.events.payload.OrderCreatedPayload;
import com.example.kafka.producer.EventPublisher;
import com.example.kafka.topics.KafkaTopicNames;
import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    // SSE sinks per orderId — broadcasts state changes to subscribed clients
    private final Map<String, Sinks.Many<OrderStatusEvent>> orderSinks = new ConcurrentHashMap<>();

    public Mono<Order> createOrder(CreateOrderRequest request) {
        // Idempotency check: if key exists, return existing order
        if (request.idempotencyKey() != null) {
            return orderRepository.findByIdempotencyKey(request.idempotencyKey())
                    .switchIfEmpty(persistAndPublish(request));
        }
        return persistAndPublish(request);
    }

    private Mono<Order> persistAndPublish(CreateOrderRequest request) {
        Order order = buildOrder(request);

        return orderRepository.save(order)
                .flatMap(saved -> {
                    // Build and publish OrderCreated event
                    var payload = new OrderCreatedPayload(
                            saved.getId(),
                            saved.getCustomerId(),
                            saved.getItems().stream().map(i ->
                                new OrderCreatedPayload.OrderItem(i.getSkuId(), i.getQuantity(), i.getUnitPrice())
                            ).toList(),
                            saved.getTotalAmount(),
                            saved.getCurrency(),
                            new OrderCreatedPayload.ShippingAddress(
                                    saved.getShippingAddress().getStreet(),
                                    saved.getShippingAddress().getCity(),
                                    saved.getShippingAddress().getState(),
                                    saved.getShippingAddress().getZip(),
                                    saved.getShippingAddress().getCountry()
                            ),
                            saved.getPaymentMethodId()
                    );
                    var envelope = EventEnvelope.root(EventType.OrderCreated.name(), saved.getId(), payload);

                    return Mono.fromFuture(
                            eventPublisher.publish(KafkaTopicNames.ORDER_LIFECYCLE, saved.getId(), envelope)
                    ).thenReturn(saved);
                })
                .doOnSuccess(o -> log.info("Order created and event published [orderId={}]", o.getId()));
    }

    public Mono<Order> getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found: " + orderId)));
    }

    public Flux<Order> listOrders(String customerId, OrderStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (customerId != null && status != null) {
            return orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        } else if (customerId != null) {
            return orderRepository.findByCustomerId(customerId, pageable);
        } else if (status != null) {
            return orderRepository.findByStatus(status, pageable);
        }
        return orderRepository.findAllBy(pageable);
    }

    /** Called by Kafka consumers to update order saga state. Publishes SSE event. */
    public Mono<Order> updateSagaState(String orderId, OrderStatus newStatus, SagaStateUpdate update) {
        return orderRepository.findById(orderId)
                .flatMap(order -> {
                    OrderStatus previousStatus = order.getStatus();
                    order.setStatus(newStatus);
                    order.setUpdatedAt(Instant.now());

                    // Apply saga state fields
                    if (update.paymentAttemptId() != null) order.getSagaState().setPaymentAttemptId(update.paymentAttemptId());
                    if (update.transactionId() != null)    order.getSagaState().setTransactionId(update.transactionId());
                    if (update.reservationId() != null)    order.getSagaState().setReservationId(update.reservationId());
                    if (update.shipmentId() != null)       order.getSagaState().setShipmentId(update.shipmentId());
                    if (update.trackingNumber() != null)   order.getSagaState().setTrackingNumber(update.trackingNumber());
                    if (update.failureReason() != null)    order.getSagaState().setFailureReason(update.failureReason());

                    return orderRepository.save(order)
                            .doOnSuccess(saved -> {
                                // Push SSE event to any listening clients
                                broadcastStatusChange(saved.getId(), previousStatus, newStatus);
                                log.info("Order saga state updated [orderId={}, {} -> {}]",
                                        orderId, previousStatus, newStatus);
                            });
                });
    }

    /** Subscribe to SSE events for a specific order. */
    public Flux<OrderStatusEvent> subscribeToOrderEvents(String orderId) {
        Sinks.Many<OrderStatusEvent> sink = orderSinks.computeIfAbsent(
                orderId,
                id -> Sinks.many().multicast().onBackpressureBuffer()
        );
        return sink.asFlux()
                .doFinally(signal -> {
                    // Clean up sink if no more subscribers
                    if (sink.currentSubscriberCount() != 0) return;
                    orderSinks.remove(orderId);
                });
    }

    private void broadcastStatusChange(String orderId, OrderStatus previous, OrderStatus current) {
        Sinks.Many<OrderStatusEvent> sink = orderSinks.get(orderId);
        if (sink != null) {
            sink.tryEmitNext(new OrderStatusEvent(orderId, previous, current, Instant.now()));
        }
    }

    private Order buildOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setId("ord_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        order.setCustomerId(request.customerId());
        order.setPaymentMethodId(request.paymentMethodId());
        order.setIdempotencyKey(request.idempotencyKey());

        List<Order.OrderItem> items = request.items().stream().map(i -> {
            Order.OrderItem item = new Order.OrderItem();
            item.setSkuId(i.skuId());
            item.setQuantity(i.quantity());
            item.setUnitPrice(i.unitPrice() != null ? i.unitPrice() : BigDecimal.ZERO);
            return item;
        }).toList();
        order.setItems(items);

        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        if (request.shippingAddress() != null) {
            Order.ShippingAddress addr = new Order.ShippingAddress();
            addr.setStreet(request.shippingAddress().street());
            addr.setCity(request.shippingAddress().city());
            addr.setState(request.shippingAddress().state());
            addr.setZip(request.shippingAddress().zip());
            addr.setCountry(request.shippingAddress().country());
            order.setShippingAddress(addr);
        }

        return order;
    }

    public record CreateOrderRequest(
            String customerId,
            List<OrderItemRequest> items,
            ShippingAddressRequest shippingAddress,
            String paymentMethodId,
            String idempotencyKey
    ) {
        public record OrderItemRequest(String skuId, int quantity, BigDecimal unitPrice) {}
        public record ShippingAddressRequest(String street, String city, String state, String zip, String country) {}
    }

    public record SagaStateUpdate(
            String paymentAttemptId,
            String transactionId,
            String reservationId,
            String shipmentId,
            String trackingNumber,
            String failureReason
    ) {
        public static SagaStateUpdate empty() {
            return new SagaStateUpdate(null, null, null, null, null, null);
        }
    }

    public record OrderStatusEvent(
            String orderId,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            Instant timestamp
    ) {}

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) { super(message); }
    }
}
