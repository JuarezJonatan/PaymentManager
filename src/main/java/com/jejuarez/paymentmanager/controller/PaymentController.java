package com.jejuarez.paymentmanager.controller;

import com.jejuarez.paymentmanager.common.dto.response.PaymentResponse;
import com.jejuarez.paymentmanager.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    public PaymentResponse findById(@PathVariable String id) {
        return paymentService.findById(id);
    }
}
