package com.jejuarez.paymentmanager.mapper;

import com.jejuarez.paymentmanager.dto.request.CreatePaymentRequest;
import com.jejuarez.paymentmanager.dto.request.SimulationResult;
import com.jejuarez.paymentmanager.dto.response.PageResponse;
import com.jejuarez.paymentmanager.dto.response.PaymentResponse;
import com.jejuarez.paymentmanager.persistence.entity.Currency;
import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentMapper")
class PaymentMapperTest {

    private final PaymentMapper paymentMapper = new PaymentMapper();

    @Test
    @DisplayName("Maps the request and the authorized status into a new payment without id nor creation date")
    void givenCreatePaymentRequest_whenToEntity_thenReturnsNewPaymentWithStatus() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "COMMERCE_001", new BigDecimal("15000.50"), Currency.USD, SimulationResult.REJECTED);

        Payment payment = paymentMapper.toEntity(request, PaymentStatus.REJECTED);

        assertThat(payment.getId()).isNull();
        assertThat(payment.getCreatedAt()).isNull();
        assertThat(payment.getMerchantId()).isEqualTo("COMMERCE_001");
        assertThat(payment.getAmount()).isEqualTo(new BigDecimal("15000.50"));
        assertThat(payment.getCurrency()).isEqualTo(Currency.USD);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
    }

    @Test
    @DisplayName("Keeps the idempotency key and the request hash in the new payment")
    void givenIdempotencyKey_whenToEntity_thenReturnsNewPaymentWithKeyAndRequestHash() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "COMMERCE_001", new BigDecimal("15000"), Currency.ARS, SimulationResult.APPROVED);

        Payment payment = paymentMapper.toEntity(request, PaymentStatus.APPROVED, "key-001", "a".repeat(64));

        assertThat(payment.getIdempotencyKey()).isEqualTo("key-001");
        assertThat(payment.getRequestHash()).isEqualTo("a".repeat(64));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("Normalizes the amount to two decimals")
    void givenIntegerAmount_whenToEntity_thenAmountHasTwoDecimals() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "COMMERCE_001", new BigDecimal("15000"), Currency.ARS, SimulationResult.APPROVED);

        Payment payment = paymentMapper.toEntity(request, PaymentStatus.APPROVED);

        assertThat(payment.getAmount()).isEqualTo(new BigDecimal("15000.00"));
    }

    @Test
    @DisplayName("Maps a page of payments into a page response keeping the pagination metadata")
    void givenPageOfPayments_whenToPageResponse_thenReturnsMappedContentAndMetadata() {
        Instant createdAt = Instant.parse("2026-10-08T15:00:00Z");
        Payment payment = new Payment("pay_001", "COMMERCE_001", new BigDecimal("15000.00"),
                Currency.ARS, PaymentStatus.APPROVED, createdAt);
        PageImpl<Payment> payments = new PageImpl<>(List.of(payment), PageRequest.of(1, 1), 3);

        PageResponse<PaymentResponse> response = paymentMapper.toPageResponse(payments);

        assertThat(response.content()).containsExactly(new PaymentResponse(
                "pay_001", "COMMERCE_001", new BigDecimal("15000.00"), Currency.ARS, PaymentStatus.APPROVED, createdAt));
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(3);
    }
}
