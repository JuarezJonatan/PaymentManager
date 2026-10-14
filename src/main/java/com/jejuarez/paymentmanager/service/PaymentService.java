package com.jejuarez.paymentmanager.service;

import com.jejuarez.paymentmanager.common.exception.ResourceNotFoundException;
import com.jejuarez.paymentmanager.dto.request.CreatePaymentRequest;
import com.jejuarez.paymentmanager.dto.response.PaymentResponse;
import com.jejuarez.paymentmanager.mapper.PaymentMapper;
import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import com.jejuarez.paymentmanager.persistence.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentAuthorizationSimulator authorizationSimulator;

    public PaymentResponse create(CreatePaymentRequest request) {
        PaymentStatus status = authorizationSimulator.authorize(request.simulationResult());
        Payment payment = paymentRepository.save(paymentMapper.toEntity(request, status));
        return paymentMapper.toResponse(payment);
    }

    public PaymentResponse findById(String id) {
        return paymentRepository.findById(id)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }
}
