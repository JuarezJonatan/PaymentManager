package com.jejuarez.paymentmanager.dto.request;

import com.jejuarez.paymentmanager.persistence.entity.Currency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotBlank(message = "merchantId must not be blank")
        @Size(max = 100, message = "merchantId must be at most 100 characters")
        String merchantId,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "amount must have at most 17 integer digits and 2 decimals")
        BigDecimal amount,

        @NotNull(message = "currency is required")
        Currency currency,

        @NotNull(message = "simulationResult is required")
        SimulationResult simulationResult
) {
}
