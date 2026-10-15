package com.jejuarez.paymentmanager.service;

import com.jejuarez.paymentmanager.dto.request.CreatePaymentRequest;
import com.jejuarez.paymentmanager.dto.request.ListPaymentsRequest;
import com.jejuarez.paymentmanager.dto.request.SimulationResult;
import com.jejuarez.paymentmanager.dto.response.PageResponse;
import com.jejuarez.paymentmanager.dto.response.PaymentResponse;
import com.jejuarez.paymentmanager.persistence.entity.Currency;
import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import com.jejuarez.paymentmanager.common.exception.PaymentNotRefundableException;
import com.jejuarez.paymentmanager.common.exception.ResourceNotFoundException;
import com.jejuarez.paymentmanager.mapper.PaymentMapper;
import com.jejuarez.paymentmanager.persistence.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentAuthorizationSimulator authorizationSimulator;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("Authorizes, stores and returns the new payment")
    void givenValidRequest_whenCreate_thenStoresAuthorizedPaymentAndReturnsResponse() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "COMMERCE_001", new BigDecimal("15000"), Currency.ARS, SimulationResult.APPROVED);
        Payment newPayment = new Payment("COMMERCE_001", new BigDecimal("15000.00"), Currency.ARS, PaymentStatus.APPROVED);
        Payment savedPayment = anApprovedPayment();
        PaymentResponse expectedResponse = new PaymentResponse(
                PAYMENT_ID, "COMMERCE_001", new BigDecimal("15000.00"), Currency.ARS, PaymentStatus.APPROVED, CREATED_AT);
        given(authorizationSimulator.authorize(SimulationResult.APPROVED)).willReturn(PaymentStatus.APPROVED);
        given(paymentMapper.toEntity(request, PaymentStatus.APPROVED)).willReturn(newPayment);
        given(paymentRepository.save(newPayment)).willReturn(savedPayment);
        given(paymentMapper.toResponse(savedPayment)).willReturn(expectedResponse);

        PaymentResponse response = paymentService.create(request);

        assertThat(response).isEqualTo(expectedResponse);
    }

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

    @Test
    @DisplayName("Lists all payments newest first using the requested page when no status is given")
    void givenNoStatus_whenList_thenQueriesAllPaymentsNewestFirst() {
        Pageable expectedPageable = PageRequest.of(2, 10, NEWEST_FIRST);
        Page<Payment> payments = new PageImpl<>(List.of(anApprovedPayment()), expectedPageable, 21);
        PageResponse<PaymentResponse> expectedResponse = new PageResponse<>(List.of(), 2, 10, 21, 3);
        given(paymentRepository.findAll(expectedPageable)).willReturn(payments);
        given(paymentMapper.toPageResponse(payments)).willReturn(expectedResponse);

        PageResponse<PaymentResponse> response = paymentService.list(new ListPaymentsRequest(null, 2, 10));

        assertThat(response).isEqualTo(expectedResponse);
        then(paymentRepository).should(never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("Lists only the payments with the given status newest first")
    void givenStatus_whenList_thenQueriesPaymentsByStatusNewestFirst() {
        Pageable expectedPageable = PageRequest.of(0, 20, NEWEST_FIRST);
        Page<Payment> payments = new PageImpl<>(List.of(anApprovedPayment()), expectedPageable, 1);
        PageResponse<PaymentResponse> expectedResponse = new PageResponse<>(List.of(), 0, 20, 1, 1);
        given(paymentRepository.findByStatus(PaymentStatus.APPROVED, expectedPageable)).willReturn(payments);
        given(paymentMapper.toPageResponse(payments)).willReturn(expectedResponse);

        PageResponse<PaymentResponse> response = paymentService.list(new ListPaymentsRequest(PaymentStatus.APPROVED, 0, 20));

        assertThat(response).isEqualTo(expectedResponse);
        then(paymentRepository).should(never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Refunds an approved payment with a conditional update and returns it as REFUNDED")
    void givenApprovedPayment_whenRefund_thenReturnsRefundedPayment() {
        Payment refundedPayment = aPaymentWithStatus(PaymentStatus.REFUNDED);
        PaymentResponse expectedResponse = new PaymentResponse(
                PAYMENT_ID, "COMMERCE_001", new BigDecimal("15000.00"), Currency.ARS, PaymentStatus.REFUNDED, CREATED_AT);
        given(paymentRepository.updateStatusIfCurrent(PAYMENT_ID, PaymentStatus.APPROVED, PaymentStatus.REFUNDED))
                .willReturn(1);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(refundedPayment));
        given(paymentMapper.toResponse(refundedPayment)).willReturn(expectedResponse);

        PaymentResponse response = paymentService.refund(PAYMENT_ID);

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("Throws PaymentNotRefundableException when the payment was already refunded")
    void givenRefundedPayment_whenRefund_thenThrowsPaymentNotRefundableException() {
        given(paymentRepository.updateStatusIfCurrent(PAYMENT_ID, PaymentStatus.APPROVED, PaymentStatus.REFUNDED))
                .willReturn(0);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(aPaymentWithStatus(PaymentStatus.REFUNDED)));

        assertThatThrownBy(() -> paymentService.refund(PAYMENT_ID))
                .isInstanceOf(PaymentNotRefundableException.class)
                .hasMessage("Payment with id 'pay_001' cannot be refunded because its status is REFUNDED");
    }

    @Test
    @DisplayName("Throws PaymentNotRefundableException when the payment was rejected")
    void givenRejectedPayment_whenRefund_thenThrowsPaymentNotRefundableException() {
        given(paymentRepository.updateStatusIfCurrent(PAYMENT_ID, PaymentStatus.APPROVED, PaymentStatus.REFUNDED))
                .willReturn(0);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(aPaymentWithStatus(PaymentStatus.REJECTED)));

        assertThatThrownBy(() -> paymentService.refund(PAYMENT_ID))
                .isInstanceOf(PaymentNotRefundableException.class)
                .hasMessage("Payment with id 'pay_001' cannot be refunded because its status is REJECTED");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when refunding a payment that does not exist")
    void givenNonExistentPaymentId_whenRefund_thenThrowsResourceNotFoundException() {
        given(paymentRepository.updateStatusIfCurrent(PAYMENT_ID, PaymentStatus.APPROVED, PaymentStatus.REFUNDED))
                .willReturn(0);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refund(PAYMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment with id 'pay_001' was not found");
    }

    private Payment anApprovedPayment() {
        return aPaymentWithStatus(PaymentStatus.APPROVED);
    }

    private Payment aPaymentWithStatus(PaymentStatus status) {
        return new Payment(PAYMENT_ID, "COMMERCE_001", new BigDecimal("15000.00"),
                Currency.ARS, status, CREATED_AT);
    }
}
