package com.eventapp.paymentservice.controller;

import com.eventapp.paymentservice.dto.request.PaymentRequestDTO;
import com.eventapp.paymentservice.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.base-path}/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final IPaymentService paymentService;

    @PostMapping
    public ResponseEntity<Void> makePayment(@RequestBody @Validated PaymentRequestDTO request) {
        paymentService.processPayment(request);

        return ResponseEntity.status(HttpStatus.CREATED)
              .build();
    }
}