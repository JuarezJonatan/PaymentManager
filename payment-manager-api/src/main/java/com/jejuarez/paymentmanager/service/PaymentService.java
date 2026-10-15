package com.jejuarez.paymentmanager.service;

import com.jejuarez.paymentmanager.common.exception.IdempotencyKeyConflictException;
import com.jejuarez.paymentmanager.common.exception.IdempotencyKeyMismatchException;
import com.jejuarez.paymentmanager.common.exception.PaymentNotRefundableException;
import com.jejuarez.paymentmanager.common.exception.ResourceNotFoundException;
import com.jejuarez.paymentmanager.dto.request.CreatePaymentRequest;
import com.jejuarez.paymentmanager.dto.request.ListPaymentsRequest;
import com.jejuarez.paymentmanager.dto.response.PageResponse;
import com.jejuarez.paymentmanager.dto.response.PaymentResponse;
import com.jejuarez.paymentmanager.mapper.PaymentMapper;
import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentIdempotencyKey;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import com.jejuarez.paymentmanager.persistence.repository.PaymentIdempotencyKeyRepository;
import com.jejuarez.paymentmanager.persistence.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final PaymentRepository paymentRepository;
    private final PaymentIdempotencyKeyRepository idempotencyKeyRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentAuthorizationSimulator authorizationSimulator;
    private final PaymentRequestHasher requestHasher;

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request, String idempotencyKey) {
        if (idempotencyKey == null) {
            return paymentMapper.toResponse(authorizeAndSave(request));
        }
        String requestHash = requestHasher.hash(request);
        return idempotencyKeyRepository.findById(idempotencyKey)
                .map(storedKey -> replay(storedKey, requestHash))
                .orElseGet(() -> createWithIdempotencyKey(request, idempotencyKey, requestHash));
    }

    private PaymentResponse replay(PaymentIdempotencyKey storedKey, String requestHash) {
        if (!storedKey.getRequestHash().equals(requestHash)) {
            throw new IdempotencyKeyMismatchException(storedKey.getKey());
        }
        return paymentMapper.toResponse(storedKey.getPayment());
    }

    private PaymentResponse createWithIdempotencyKey(CreatePaymentRequest request, String idempotencyKey,
                                                     String requestHash) {
        Payment payment = authorizeAndSave(request);
        try {
            idempotencyKeyRepository.saveAndFlush(new PaymentIdempotencyKey(idempotencyKey, requestHash, payment));
        } catch (DataIntegrityViolationException exception) {
            throw new IdempotencyKeyConflictException(idempotencyKey);
        }
        return paymentMapper.toResponse(payment);
    }

    private Payment authorizeAndSave(CreatePaymentRequest request) {
        PaymentStatus status = authorizationSimulator.authorize(request.simulationResult());
        return paymentRepository.save(paymentMapper.toEntity(request, status));
    }

    public PaymentResponse findById(String id) {
        return paymentRepository.findById(id)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    public PageResponse<PaymentResponse> list(ListPaymentsRequest request) {
        Pageable pageable = PageRequest.of(request.page(), request.size(), NEWEST_FIRST);
        Page<Payment> payments = request.status() == null
                ? paymentRepository.findAll(pageable)
                : paymentRepository.findByStatus(request.status(), pageable);
        return paymentMapper.toPageResponse(payments);
    }

    @Transactional
    public PaymentResponse refund(String id) {
        int refundedRows = paymentRepository.updateStatusIfCurrent(id, PaymentStatus.APPROVED, PaymentStatus.REFUNDED);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
        if (refundedRows == 0) {
            throw new PaymentNotRefundableException(id, payment.getStatus());
        }
        return paymentMapper.toResponse(payment);
    }
}
