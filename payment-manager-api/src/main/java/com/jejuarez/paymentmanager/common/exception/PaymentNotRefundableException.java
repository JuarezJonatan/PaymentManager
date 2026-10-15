package com.jejuarez.paymentmanager.common.exception;

import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;

public class PaymentNotRefundableException extends RuntimeException {

    public PaymentNotRefundableException(String paymentId, PaymentStatus status) {
        super("Payment with id '%s' cannot be refunded because its status is %s".formatted(paymentId, status));
    }
}
