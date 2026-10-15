package com.jejuarez.paymentmanager.common.exception;

public class IdempotencyKeyMismatchException extends RuntimeException {

    public IdempotencyKeyMismatchException(String key) {
        super("Idempotency-Key '%s' was already used with a different request".formatted(key));
    }
}
