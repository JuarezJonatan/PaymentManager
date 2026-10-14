package com.jejuarez.paymentmanager.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String paymentId) {
        super("Payment with id '%s' was not found".formatted(paymentId));
    }
}
