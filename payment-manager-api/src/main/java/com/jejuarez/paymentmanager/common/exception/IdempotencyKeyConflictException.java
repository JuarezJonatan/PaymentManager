package com.jejuarez.paymentmanager.common.exception;

public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException(String key) {
        super("A request with Idempotency-Key '%s' is already being processed".formatted(key));
    }
}
