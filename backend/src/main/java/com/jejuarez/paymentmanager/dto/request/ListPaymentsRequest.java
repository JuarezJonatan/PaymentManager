package com.jejuarez.paymentmanager.dto.request;

import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Objects;

public record ListPaymentsRequest(
        PaymentStatus status,

        @PositiveOrZero(message = "page must be greater than or equal to zero")
        Integer page,

        @Min(value = 1, message = "size must be between 1 and 100")
        @Max(value = 100, message = "size must be between 1 and 100")
        Integer size
) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    public ListPaymentsRequest {
        page = Objects.requireNonNullElse(page, DEFAULT_PAGE);
        size = Objects.requireNonNullElse(size, DEFAULT_SIZE);
    }
}
