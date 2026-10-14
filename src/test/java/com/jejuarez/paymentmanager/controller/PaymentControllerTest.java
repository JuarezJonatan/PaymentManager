package com.jejuarez.paymentmanager.controller;

import com.jejuarez.paymentmanager.dto.response.PaymentResponse;
import com.jejuarez.paymentmanager.persistence.entity.Currency;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import com.jejuarez.paymentmanager.common.exception.ResourceNotFoundException;
import com.jejuarez.paymentmanager.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PaymentController (web layer)")
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    private static final String PAYMENT_ID = "pay_001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("GET /payments/{id} returns 200 with the payment body when it exists")
    void givenExistingPayment_whenGetPaymentById_thenReturnsOkWithPayment() throws Exception {
        PaymentResponse payment = new PaymentResponse(PAYMENT_ID, "COMMERCE_001", new BigDecimal("15000.00"),
                Currency.ARS, PaymentStatus.APPROVED, Instant.parse("2026-10-08T15:00:00Z"));
        given(paymentService.findById(PAYMENT_ID)).willReturn(payment);

        mockMvc.perform(get("/payments/{id}", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID))
                .andExpect(jsonPath("$.merchantId").value("COMMERCE_001"))
                .andExpect(jsonPath("$.amount").value(15000.00))
                .andExpect(jsonPath("$.currency").value("ARS"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-08T15:00:00Z"));
    }

    @Test
    @DisplayName("GET /payments/{id} returns 404 ProblemDetail when the payment does not exist")
    void givenNonExistentPaymentId_whenGetPaymentById_thenReturnsNotFoundProblemDetail() throws Exception {
        given(paymentService.findById(PAYMENT_ID)).willThrow(new ResourceNotFoundException("Payment", PAYMENT_ID));

        mockMvc.perform(get("/payments/{id}", PAYMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Payment with id 'pay_001' was not found"))
                .andExpect(jsonPath("$.instance").value("/payments/pay_001"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
