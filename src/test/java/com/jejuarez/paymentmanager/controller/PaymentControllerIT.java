package com.jejuarez.paymentmanager.controller;

import com.jejuarez.paymentmanager.persistence.entity.Currency;
import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import com.jejuarez.paymentmanager.persistence.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PaymentController integration (H2 + Flyway)")
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
    @DisplayName("GET /payments/{id} returns the payment stored in the database")
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
    @DisplayName("GET /payments/{id} returns 404 when the payment is not in the database")
    void givenNonExistentPaymentId_whenGetPaymentById_thenReturnsNotFound() throws Exception {
        mockMvc.perform(get("/payments/{id}", "pay_missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Payment with id 'pay_missing' was not found"));
    }

    @Test
    @DisplayName("POST /payments stores an approved payment with a generated id and creation date")
    void givenApprovedSimulation_whenCreatePayment_thenStoresApprovedPayment() throws Exception {
        String body = """
                {"merchantId": "COMMERCE_001", "amount": 15000, "currency": "ARS", "simulationResult": "APPROVED"}
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(startsWith("pay_")))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.createdAt").exists());

        assertThat(paymentRepository.findAll())
                .singleElement()
                .satisfies(payment -> {
                    assertThat(payment.getMerchantId()).isEqualTo("COMMERCE_001");
                    assertThat(payment.getAmount()).isEqualByComparingTo("15000");
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
                    assertThat(payment.getCreatedAt()).isNotNull();
                });
    }

    @Test
    @DisplayName("POST /payments stores a rejected payment when the simulation result is REJECTED")
    void givenRejectedSimulation_whenCreatePayment_thenStoresRejectedPayment() throws Exception {
        String body = """
                {"merchantId": "COMMERCE_001", "amount": 15000, "currency": "ARS", "simulationResult": "REJECTED"}
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(paymentRepository.findAll())
                .singleElement()
                .extracting(Payment::getStatus)
                .isEqualTo(PaymentStatus.REJECTED);
    }

    @Test
    @DisplayName("POST /payments returns 400 and stores nothing when the amount is not positive")
    void givenNonPositiveAmount_whenCreatePayment_thenReturnsBadRequestAndStoresNothing() throws Exception {
        String body = """
                {"merchantId": "COMMERCE_001", "amount": -10, "currency": "ARS", "simulationResult": "APPROVED"}
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("amount must be greater than zero"));

        assertThat(paymentRepository.count()).isZero();
    }
}
