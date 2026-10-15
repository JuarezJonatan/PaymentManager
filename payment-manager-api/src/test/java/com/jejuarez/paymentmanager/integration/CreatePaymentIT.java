package com.jejuarez.paymentmanager.integration;

import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Create payment (POST /payments) integration")
class CreatePaymentIT extends IntegrationTestSupport {

    private static final String IDEMPOTENCY_KEY = "4f9c2a7e-1b3d-4c5e-8f6a-7b8c9d0e1f2a";
    private static final String APPROVED_REQUEST_BODY = """
            {"merchantId": "COMMERCE_001", "amount": 15000, "currency": "ARS", "simulationResult": "APPROVED"}
            """;

    @Test
    @DisplayName("Returns the same payment and stores only one when a request is retried with the same Idempotency-Key")
    void givenRequestRetriedWithSameIdempotencyKey_whenCreatePayment_thenStoresOnlyOnePayment() throws Exception {
        String firstResponse = createWithIdempotencyKey(APPROVED_REQUEST_BODY)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        createWithIdempotencyKey(APPROVED_REQUEST_BODY)
                .andExpect(status().isCreated())
                .andExpect(content().json(firstResponse));

        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Returns 422 and keeps the original payment when the Idempotency-Key is reused with a different request")
    void givenIdempotencyKeyReusedWithDifferentRequest_whenCreatePayment_thenReturnsUnprocessableContentAndKeepsOriginalPayment()
            throws Exception {
        createWithIdempotencyKey(APPROVED_REQUEST_BODY).andExpect(status().isCreated());
        String differentBody = """
                {"merchantId": "COMMERCE_001", "amount": 15000, "currency": "ARS", "simulationResult": "REJECTED"}
                """;

        createWithIdempotencyKey(differentBody)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Idempotency key reused"));

        assertThat(paymentRepository.findAll())
                .singleElement()
                .extracting(Payment::getStatus)
                .isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("Stores a new payment on every request without an Idempotency-Key")
    void givenRequestsWithoutIdempotencyKey_whenCreatePaymentTwice_thenStoresTwoPayments() throws Exception {
        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(APPROVED_REQUEST_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(APPROVED_REQUEST_BODY))
                .andExpect(status().isCreated());

        assertThat(paymentRepository.count()).isEqualTo(2);
    }

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

    private ResultActions createWithIdempotencyKey(String body) throws Exception {
        return mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", IDEMPOTENCY_KEY)
                .content(body));
    }
}
