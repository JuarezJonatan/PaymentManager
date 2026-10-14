package com.jejuarez.paymentmanager.mapper;

import com.jejuarez.paymentmanager.dto.request.CreatePaymentRequest;
import com.jejuarez.paymentmanager.dto.request.SimulationResult;
import com.jejuarez.paymentmanager.persistence.entity.Currency;
import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
    @DisplayName("Normalizes the amount to two decimals")
    void givenIntegerAmount_whenToEntity_thenAmountHasTwoDecimals() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "COMMERCE_001", new BigDecimal("15000"), Currency.ARS, SimulationResult.APPROVED);

        Payment payment = paymentMapper.toEntity(request, PaymentStatus.APPROVED);

        assertThat(payment.getAmount()).isEqualTo(new BigDecimal("15000.00"));
    }
}
