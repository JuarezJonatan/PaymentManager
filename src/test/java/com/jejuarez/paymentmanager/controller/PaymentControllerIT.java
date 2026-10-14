package com.jejuarez.paymentmanager.controller;

import com.jejuarez.paymentmanager.entity.Currency;
import com.jejuarez.paymentmanager.entity.Payment;
import com.jejuarez.paymentmanager.entity.PaymentStatus;
import com.jejuarez.paymentmanager.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @AfterEach
    void cleanUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void givenPersistedPayment_whenGetPaymentById_thenReturnsStoredPayment() throws Exception {
        paymentRepository.save(Payment.builder()
                .id("pay_003")
                .merchantId("COMMERCE_002")
                .amount(new BigDecimal("8200.00"))
                .currency(Currency.ARS)
                .status(PaymentStatus.REFUNDED)
                .createdAt(Instant.parse("2026-10-08T15:10:00Z"))
                .build());

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
    void givenNonExistentPaymentId_whenGetPaymentById_thenReturnsNotFound() throws Exception {
        mockMvc.perform(get("/payments/{id}", "pay_missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Payment with id 'pay_missing' was not found"));
    }
}
