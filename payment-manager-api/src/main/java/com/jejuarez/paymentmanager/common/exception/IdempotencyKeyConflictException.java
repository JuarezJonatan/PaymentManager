package com.jejuarez.paymentmanager.common.exception;

public class IdempotencyKeyConflictException extends RuntimeException {

    private IdempotencyKeyConflictException(String message) {
        super(message);
    }

    public static IdempotencyKeyConflictException reusedWithDifferentRequest(String key) {
        return new IdempotencyKeyConflictException(
                "Idempotency-Key '%s' was already used with a different request".formatted(key));
    }

    public static IdempotencyKeyConflictException concurrentRequest(String key) {
        return new IdempotencyKeyConflictException(
                "A request with Idempotency-Key '%s' is already being processed".formatted(key));
    }
}
