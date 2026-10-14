package com.jejuarez.paymentmanager.controller;

import com.jejuarez.paymentmanager.dto.request.CreatePaymentRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PaymentController (web layer)")
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    private static final String PAYMENT_ID = "pay_001";
    private static final String VALID_REQUEST_BODY = """
            {"merchantId": "COMMERCE_001", "amount": 15000, "currency": "ARS", "simulationResult": "APPROVED"}
            """;

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

    @Test
    @DisplayName("POST /payments returns 201 with the created payment")
    void givenValidRequest_whenCreatePayment_thenReturnsCreatedWithPayment() throws Exception {
        PaymentResponse payment = new PaymentResponse(PAYMENT_ID, "COMMERCE_001", new BigDecimal("15000.00"),
                Currency.ARS, PaymentStatus.APPROVED, Instant.parse("2026-10-08T15:00:00Z"));
        given(paymentService.create(any(CreatePaymentRequest.class))).willReturn(payment);

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID))
                .andExpect(jsonPath("$.merchantId").value("COMMERCE_001"))
                .andExpect(jsonPath("$.amount").value(15000.00))
                .andExpect(jsonPath("$.currency").value("ARS"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-08T15:00:00Z"));
    }

    @Test
    @DisplayName("POST /payments returns 400 with field errors and does not create the payment when fields are invalid")
    void givenInvalidFields_whenCreatePayment_thenReturnsBadRequestWithFieldErrors() throws Exception {
        String body = """
                {"merchantId": " ", "amount": 0, "currency": "ARS", "simulationResult": "APPROVED"}
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.instance").value("/payments"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.merchantId").value("merchantId must not be blank"))
                .andExpect(jsonPath("$.errors.amount").value("amount must be greater than zero"));
        then(paymentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /payments returns 400 listing the accepted values when the currency is not supported")
    void givenUnsupportedCurrency_whenCreatePayment_thenReturnsBadRequestWithAcceptedValues() throws Exception {
        String body = """
                {"merchantId": "COMMERCE_001", "amount": 15000, "currency": "XYZ", "simulationResult": "APPROVED"}
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail").value(
                        "Invalid value 'XYZ' for field 'currency'. Accepted values: [ARS, USD, EUR, BRL, CLP, UYU, PYG, BOB, PEN, COP, MXN, GBP]"));
        then(paymentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /payments returns 400 listing the accepted values when the simulationResult is invalid")
    void givenInvalidSimulationResult_whenCreatePayment_thenReturnsBadRequestWithAcceptedValues() throws Exception {
        String body = """
                {"merchantId": "COMMERCE_001", "amount": 15000, "currency": "ARS", "simulationResult": "MAYBE"}
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Invalid value 'MAYBE' for field 'simulationResult'. Accepted values: [APPROVED, REJECTED]"));
        then(paymentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /payments returns 400 naming the field when the amount is not a number")
    void givenNonNumericAmount_whenCreatePayment_thenReturnsBadRequestNamingTheField() throws Exception {
        String body = """
                {"merchantId": "COMMERCE_001", "amount": "abc", "currency": "ARS", "simulationResult": "APPROVED"}
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value for field 'amount'"));
        then(paymentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /payments returns 400 when the body is not valid JSON")
    void givenMalformedJson_whenCreatePayment_thenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content("{\"merchantId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail").value("Malformed JSON request body"));
        then(paymentService).shouldHaveNoInteractions();
    }
}
