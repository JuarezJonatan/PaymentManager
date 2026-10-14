package com.jejuarez.paymentmanager.service;

import com.jejuarez.paymentmanager.dto.response.PaymentResponse;
import com.jejuarez.paymentmanager.persistence.entity.Currency;
import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import com.jejuarez.paymentmanager.exception.ResourceNotFoundException;
import com.jejuarez.paymentmanager.mapper.PaymentMapper;
import com.jejuarez.paymentmanager.persistence.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@DisplayName("PaymentService")
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String PAYMENT_ID = "pay_001";
    private static final Instant CREATED_AT = Instant.parse("2026-10-08T15:00:00Z");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("Returns the mapped payment when the id exists")
    void givenExistingPayment_whenFindById_thenReturnsPaymentResponse() {
        Payment payment = anApprovedPayment();
        PaymentResponse expectedResponse = new PaymentResponse(
                PAYMENT_ID, "COMMERCE_001", new BigDecimal("15000.00"), Currency.ARS, PaymentStatus.APPROVED, CREATED_AT);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(paymentMapper.toResponse(payment)).willReturn(expectedResponse);

        PaymentResponse response = paymentService.findById(PAYMENT_ID);

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException and skips mapping when the id does not exist")
    void givenNonExistentPaymentId_whenFindById_thenThrowsResourceNotFoundException() {
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findById(PAYMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment with id 'pay_001' was not found");
        then(paymentMapper).should(never()).toResponse(any());
    }

    private Payment anApprovedPayment() {
        return Payment.builder()
                .id(PAYMENT_ID)
                .merchantId("COMMERCE_001")
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.ARS)
                .status(PaymentStatus.APPROVED)
                .createdAt(CREATED_AT)
                .build();
    }
}
