package com.jejuarez.paymentmanager.dto.response;

import com.jejuarez.paymentmanager.persistence.entity.Currency;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String id,
        String merchantId,
        BigDecimal amount,
        Currency currency,
        PaymentStatus status,
        Instant createdAt
) {
}
