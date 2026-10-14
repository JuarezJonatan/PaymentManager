package com.jejuarez.paymentmanager.integration;

import com.jejuarez.paymentmanager.persistence.entity.Currency;
import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("List payments (GET /payments) integration")
class ListPaymentsIT extends IntegrationTestSupport {

    @BeforeEach
    void seedPayments() {
        paymentRepository.saveAll(List.of(
                aPayment("pay_001", PaymentStatus.APPROVED, "2026-10-08T15:00:00Z"),
                aPayment("pay_002", PaymentStatus.REJECTED, "2026-10-08T15:05:00Z"),
                aPayment("pay_003", PaymentStatus.REFUNDED, "2026-10-08T15:10:00Z"),
                aPayment("pay_004", PaymentStatus.APPROVED, "2026-10-08T15:15:00Z")));
    }

    @Test
    @DisplayName("Returns every stored payment newest first with the page metadata")
    void givenStoredPayments_whenListPayments_thenReturnsAllNewestFirst() throws Exception {
        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(contains("pay_004", "pay_003", "pay_002", "pay_001")))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("Returns only the payments with the requested status")
    void givenStatusFilter_whenListPayments_thenReturnsOnlyMatchingPayments() throws Exception {
        mockMvc.perform(get("/payments").param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(contains("pay_004", "pay_001")))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Returns the requested slice of payments when paginating")
    void givenPageAndSize_whenListPayments_thenReturnsRequestedSlice() throws Exception {
        mockMvc.perform(get("/payments").param("page", "1").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(contains("pay_001")))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("Returns an empty page when the requested page is beyond the last one")
    void givenPageBeyondLast_whenListPayments_thenReturnsEmptyContent() throws Exception {
        mockMvc.perform(get("/payments").param("page", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    private Payment aPayment(String id, PaymentStatus status, String createdAt) {
        return new Payment(id, "COMMERCE_001", new BigDecimal("15000.00"), Currency.ARS, status, Instant.parse(createdAt));
    }
}
