package com.jejuarez.paymentmanager.integration;

import com.jejuarez.paymentmanager.persistence.entity.Currency;
import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentIdempotencyKey;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Create payment (POST /payments) integration")
class CreatePaymentIT extends IntegrationTestSupport {

    private static final String IDEMPOTENCY_KEY = "4f9c2a7e-1b3d-4c5e-8f6a-7b8c9d0e1f2a";
    private static final int CONCURRENT_REQUESTS = 10;
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
    @DisplayName("Stores only one payment when several requests with the same Idempotency-Key arrive at the same time")
    void givenConcurrentRequestsWithSameIdempotencyKey_whenCreatePayment_thenStoresOnlyOnePayment() throws Exception {
        CountDownLatch startSignal = new CountDownLatch(1);
        Callable<Integer> create = () -> {
            startSignal.await();
            return createWithIdempotencyKey(APPROVED_REQUEST_BODY).andReturn().getResponse().getStatus();
        };

        List<Future<Integer>> responses;
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)) {
            responses = IntStream.range(0, CONCURRENT_REQUESTS)
                    .mapToObj(i -> executor.submit(create))
                    .toList();
            startSignal.countDown();
        }
        List<Integer> statusCodes = responses.stream().map(Future::resultNow).toList();

        assertThat(statusCodes).contains(201).containsOnly(201, 409);
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(idempotencyKeyRepository.findById(IDEMPOTENCY_KEY))
                .hasValueSatisfying(storedKey -> assertThat(storedKey.getPayment().getId())
                        .isEqualTo(paymentRepository.findAll().getFirst().getId()));
    }

    @Test
    @DisplayName("Rejects storing an Idempotency-Key that already exists instead of pointing it to another payment")
    void givenStoredIdempotencyKey_whenStoringSameKeyForAnotherPayment_thenThrowsAndKeepsOriginalPayment() {
        Payment originalPayment = paymentRepository.save(anApprovedPayment());
        Payment otherPayment = paymentRepository.save(anApprovedPayment());
        idempotencyKeyRepository.saveAndFlush(new PaymentIdempotencyKey(IDEMPOTENCY_KEY, "a".repeat(64), originalPayment));

        assertThatThrownBy(() -> idempotencyKeyRepository.saveAndFlush(
                new PaymentIdempotencyKey(IDEMPOTENCY_KEY, "a".repeat(64), otherPayment)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(idempotencyKeyRepository.findById(IDEMPOTENCY_KEY))
                .hasValueSatisfying(storedKey ->
                        assertThat(storedKey.getPayment().getId()).isEqualTo(originalPayment.getId()));
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

    private Payment anApprovedPayment() {
        return new Payment("COMMERCE_001", new BigDecimal("15000.00"), Currency.ARS, PaymentStatus.APPROVED);
    }
}
