package com.jejuarez.paymentmanager.service;

import com.jejuarez.paymentmanager.dto.response.PaymentResponse;
import com.jejuarez.paymentmanager.exception.ResourceNotFoundException;
import com.jejuarez.paymentmanager.mapper.PaymentMapper;
import com.jejuarez.paymentmanager.persistence.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Transactional(readOnly = true)
    public PaymentResponse findById(String id) {
        return paymentRepository.findById(id)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }
}
