package com.jejuarez.paymentmanager.integration;

import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Create payment (POST /payments) integration")
class CreatePaymentIT extends IntegrationTestSupport {

    @Test
    @DisplayName("Stores an approved payment with a generated id and creation date")
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
    @DisplayName("Stores a rejected payment when the simulation result is REJECTED")
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
    @DisplayName("Returns 400 and stores nothing when the amount is not positive")
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
