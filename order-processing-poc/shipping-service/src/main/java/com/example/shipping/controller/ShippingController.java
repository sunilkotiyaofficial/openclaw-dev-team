package com.example.shipping.controller;

import com.example.shipping.model.Shipment;
import com.example.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @GetMapping("/{shipmentId}")
    public Mono<ResponseEntity<Shipment>> getShipment(@PathVariable String shipmentId) {
        return shippingService.getById(shipmentId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    public Mono<ResponseEntity<Shipment>> getShipmentByOrder(@PathVariable String orderId) {
        return shippingService.getByOrderId(orderId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
