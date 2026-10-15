package com.jejuarez.paymentmanager.service;

import com.jejuarez.paymentmanager.dto.request.SimulationResult;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentAuthorizationSimulator")
class PaymentAuthorizationSimulatorTest {

    private final PaymentAuthorizationSimulator simulator = new PaymentAuthorizationSimulator();

    @Test
    @DisplayName("Approves the payment when the simulation result is APPROVED")
    void givenApprovedSimulationResult_whenAuthorize_thenReturnsApproved() {
        PaymentStatus status = simulator.authorize(SimulationResult.APPROVED);

        assertThat(status).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("Rejects the payment when the simulation result is REJECTED")
    void givenRejectedSimulationResult_whenAuthorize_thenReturnsRejected() {
        PaymentStatus status = simulator.authorize(SimulationResult.REJECTED);

        assertThat(status).isEqualTo(PaymentStatus.REJECTED);
    }
}
