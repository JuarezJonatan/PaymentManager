package com.jejuarez.paymentmanager.integration;

import com.jejuarez.paymentmanager.persistence.entity.Currency;
import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Refund payment (POST /payments/{id}/refund) integration")
class RefundPaymentIT extends IntegrationTestSupport {

    private static final String PAYMENT_ID = "pay_001";
    private static final int CONCURRENT_REFUNDS = 10;

    @Test
    @DisplayName("Refunds an approved payment and persists the REFUNDED status")
    void givenApprovedPayment_whenRefundPayment_thenReturnsOkAndPersistsRefundedStatus() throws Exception {
        paymentRepository.save(aPaymentWithStatus(PaymentStatus.APPROVED));

        mockMvc.perform(post("/payments/{id}/refund", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.amount").value(15000.00));

        assertThat(storedStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("Returns 409 and keeps the status when the payment was already refunded")
    void givenRefundedPayment_whenRefundPayment_thenReturnsConflictAndKeepsStatus() throws Exception {
        paymentRepository.save(aPaymentWithStatus(PaymentStatus.REFUNDED));

        mockMvc.perform(post("/payments/{id}/refund", PAYMENT_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Payment with id 'pay_001' cannot be refunded because its status is REFUNDED"));

        assertThat(storedStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("Returns 409 and keeps the status when the payment was rejected")
    void givenRejectedPayment_whenRefundPayment_thenReturnsConflictAndKeepsStatus() throws Exception {
        paymentRepository.save(aPaymentWithStatus(PaymentStatus.REJECTED));

        mockMvc.perform(post("/payments/{id}/refund", PAYMENT_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Payment with id 'pay_001' cannot be refunded because its status is REJECTED"));

        assertThat(storedStatus()).isEqualTo(PaymentStatus.REJECTED);
    }

    @Test
    @DisplayName("Returns 404 when the payment is not in the database")
    void givenNonExistentPaymentId_whenRefundPayment_thenReturnsNotFound() throws Exception {
        mockMvc.perform(post("/payments/{id}/refund", "pay_missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Payment with id 'pay_missing' was not found"));
    }

    @Test
    @DisplayName("Refunds the payment only once when several refunds arrive at the same time")
    void givenConcurrentRefundsOfSamePayment_whenRefundPayment_thenOnlyOneSucceeds() throws Exception {
        paymentRepository.save(aPaymentWithStatus(PaymentStatus.APPROVED));
        CountDownLatch startSignal = new CountDownLatch(1);
        Callable<Integer> refund = () -> {
            startSignal.await();
            return mockMvc.perform(post("/payments/{id}/refund", PAYMENT_ID)).andReturn().getResponse().getStatus();
        };

        List<Future<Integer>> responses;
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REFUNDS)) {
            responses = IntStream.range(0, CONCURRENT_REFUNDS)
                    .mapToObj(i -> executor.submit(refund))
                    .toList();
            startSignal.countDown();
        }
        List<Integer> statusCodes = responses.stream().map(Future::resultNow).toList();

        assertThat(statusCodes).containsOnlyOnce(200);
        assertThat(statusCodes).filteredOn(code -> code != 200).hasSize(CONCURRENT_REFUNDS - 1).containsOnly(409);
        assertThat(storedStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    private PaymentStatus storedStatus() {
        return paymentRepository.findById(PAYMENT_ID).orElseThrow().getStatus();
    }

    private Payment aPaymentWithStatus(PaymentStatus status) {
        return new Payment(PAYMENT_ID, "COMMERCE_001", new BigDecimal("15000.00"),
                Currency.ARS, status, Instant.parse("2026-10-08T15:00:00Z"));
    }
}
