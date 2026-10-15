package com.jejuarez.paymentmanager.service;

import com.jejuarez.paymentmanager.dto.request.CreatePaymentRequest;
import com.jejuarez.paymentmanager.dto.request.SimulationResult;
import com.jejuarez.paymentmanager.persistence.entity.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentRequestHasher")
class PaymentRequestHasherTest {

    private final PaymentRequestHasher requestHasher = new PaymentRequestHasher();

    @Test
    @DisplayName("Returns the same SHA-256 hash for amounts that differ only in trailing zeros")
    void givenSameAmountWithDifferentScale_whenHash_thenReturnsSameHash() {
        String hash = requestHasher.hash(aRequest(new BigDecimal("1500.5"), SimulationResult.APPROVED));

        String otherHash = requestHasher.hash(aRequest(new BigDecimal("1500.50"), SimulationResult.APPROVED));

        assertThat(hash).hasSize(64).isEqualTo(otherHash);
    }

    @Test
    @DisplayName("Returns a different hash when any field of the request changes")
    void givenDifferentSimulationResult_whenHash_thenReturnsDifferentHash() {
        String hash = requestHasher.hash(aRequest(new BigDecimal("1500.50"), SimulationResult.APPROVED));

        String otherHash = requestHasher.hash(aRequest(new BigDecimal("1500.50"), SimulationResult.REJECTED));

        assertThat(hash).isNotEqualTo(otherHash);
    }

    private CreatePaymentRequest aRequest(BigDecimal amount, SimulationResult simulationResult) {
        return new CreatePaymentRequest("COMMERCE_001", amount, Currency.ARS, simulationResult);
    }
}
