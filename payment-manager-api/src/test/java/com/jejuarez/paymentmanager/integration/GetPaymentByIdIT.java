package com.jejuarez.paymentmanager.integration;

import com.jejuarez.paymentmanager.persistence.entity.Currency;
import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Get payment by id (GET /payments/{id}) integration")
class GetPaymentByIdIT extends IntegrationTestSupport {

    @Test
    @DisplayName("Returns the payment stored in the database")
    void givenPersistedPayment_whenGetPaymentById_thenReturnsStoredPayment() throws Exception {
        paymentRepository.save(new Payment("pay_003", "COMMERCE_002", new BigDecimal("8200.00"),
                Currency.ARS, PaymentStatus.REFUNDED, Instant.parse("2026-10-08T15:10:00Z")));

        mockMvc.perform(get("/payments/{id}", "pay_003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pay_003"))
                .andExpect(jsonPath("$.merchantId").value("COMMERCE_002"))
                .andExpect(jsonPath("$.amount").value(8200.00))
                .andExpect(jsonPath("$.currency").value("ARS"))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-08T15:10:00Z"));
    }

    @Test
    @DisplayName("Returns 404 when the payment is not in the database")
    void givenNonExistentPaymentId_whenGetPaymentById_thenReturnsNotFound() throws Exception {
        mockMvc.perform(get("/payments/{id}", "pay_missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Payment with id 'pay_missing' was not found"));
    }
}
