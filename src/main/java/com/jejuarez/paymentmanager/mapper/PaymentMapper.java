package com.jejuarez.paymentmanager.mapper;

import com.jejuarez.paymentmanager.dto.request.CreatePaymentRequest;
import com.jejuarez.paymentmanager.dto.response.PaymentResponse;
import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    private static final int AMOUNT_SCALE = 2;

    public Payment toEntity(CreatePaymentRequest request, PaymentStatus status) {
        return new Payment(
                request.merchantId(),
                request.amount().setScale(AMOUNT_SCALE),
                request.currency(),
                status
        );
    }

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getMerchantId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}
