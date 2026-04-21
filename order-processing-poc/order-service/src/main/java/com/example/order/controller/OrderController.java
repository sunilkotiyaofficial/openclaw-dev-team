package com.example.order.controller;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.service.OrderService;
import com.example.order.service.OrderService.CreateOrderRequest;
import com.example.order.service.OrderService.OrderStatusEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ResponseEntity<Order>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader) {

        // Support idempotency key via header OR body field
        CreateOrderRequest requestWithKey = idempotencyKeyHeader != null && request.idempotencyKey() == null
                ? new CreateOrderRequest(request.customerId(), request.items(),
                        request.shippingAddress(), request.paymentMethodId(), idempotencyKeyHeader)
                : request;

        return orderService.createOrder(requestWithKey)
                .map(order -> ResponseEntity.accepted().body(order))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    @GetMapping
    public Flux<Order> listOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.listOrders(customerId, status, page, size);
    }

    @GetMapping("/{orderId}")
    public Mono<ResponseEntity<Order>> getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId)
                .map(ResponseEntity::ok)
                .onErrorReturn(OrderService.OrderNotFoundException.class,
                        ResponseEntity.notFound().build());
    }

    /**
     * SSE endpoint — streams real-time saga state changes to the React dashboard.
     * Client connects and receives OrderStatusEvent objects as JSON data: {...} events.
     */
    @GetMapping(value = "/{orderId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<OrderStatusEvent> streamOrderEvents(@PathVariable String orderId) {
        return orderService.subscribeToOrderEvents(orderId);
    }
}
