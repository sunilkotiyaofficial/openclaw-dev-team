package com.example.payment.controller;

import com.example.payment.model.PaymentAttempt;
import com.example.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public Flux<PaymentAttempt> listPayments(@RequestParam(required = false) String orderId) {
        if (orderId != null) return paymentService.getByOrderId(orderId);
        return Flux.empty();
    }

    @GetMapping("/{paymentAttemptId}")
    public Mono<ResponseEntity<PaymentAttempt>> getPayment(@PathVariable String paymentAttemptId) {
        return paymentService.getById(paymentAttemptId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }
}
