package com.jejuarez.paymentmanager.controller;

import com.jejuarez.paymentmanager.dto.request.CreatePaymentRequest;
import com.jejuarez.paymentmanager.dto.request.ListPaymentsRequest;
import com.jejuarez.paymentmanager.dto.response.PageResponse;
import com.jejuarez.paymentmanager.dto.response.PaymentResponse;
import com.jejuarez.paymentmanager.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class PaymentController {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 1, max = 100, message = "Idempotency-Key must be between 1 and 100 characters")
            String idempotencyKey) {
        return paymentService.create(request, idempotencyKey);
    }

    @GetMapping
    public PageResponse<PaymentResponse> list(@Valid @ModelAttribute ListPaymentsRequest request) {
        return paymentService.list(request);
    }

    @GetMapping("/{id}")
    public PaymentResponse findById(@PathVariable String id) {
        return paymentService.findById(id);
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refund(@PathVariable String id) {
        return paymentService.refund(id);
    }
}
