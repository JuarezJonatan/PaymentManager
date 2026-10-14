package com.jejuarez.paymentmanager.common.dto.request;

import com.jejuarez.paymentmanager.persistence.entity.Currency;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreatePaymentRequest validation")
class CreatePaymentRequestTest {

    private static final String MERCHANT_ID = "COMMERCE_001";
    private static final BigDecimal AMOUNT = new BigDecimal("15000.00");

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("A request with all fields valid has no violations")
    void givenValidRequest_whenValidate_thenHasNoViolations() {
        CreatePaymentRequest request = new CreatePaymentRequest(MERCHANT_ID, AMOUNT, Currency.ARS, SimulationResult.APPROVED);

        Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("merchantId null, empty or blank is rejected")
    void givenBlankMerchantId_whenValidate_thenReportsMerchantIdViolation(String merchantId) {
        CreatePaymentRequest request = new CreatePaymentRequest(merchantId, AMOUNT, Currency.ARS, SimulationResult.APPROVED);

        Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

        assertThatSingleViolation(violations, "merchantId", "merchantId must not be blank");
    }

    @Test
    @DisplayName("merchantId longer than 100 characters is rejected")
    void givenTooLongMerchantId_whenValidate_thenReportsMerchantIdViolation() {
        CreatePaymentRequest request = new CreatePaymentRequest("M".repeat(101), AMOUNT, Currency.ARS, SimulationResult.APPROVED);

        Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

        assertThatSingleViolation(violations, "merchantId", "merchantId must be at most 100 characters");
    }

    @Test
    @DisplayName("A missing amount is rejected")
    void givenNullAmount_whenValidate_thenReportsAmountRequired() {
        CreatePaymentRequest request = new CreatePaymentRequest(MERCHANT_ID, null, Currency.ARS, SimulationResult.APPROVED);

        Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

        assertThatSingleViolation(violations, "amount", "amount is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-0.01", "-15000"})
    @DisplayName("An amount equal to or below zero is rejected")
    void givenNonPositiveAmount_whenValidate_thenReportsAmountMustBePositive(String amount) {
        CreatePaymentRequest request = new CreatePaymentRequest(MERCHANT_ID, new BigDecimal(amount), Currency.ARS, SimulationResult.APPROVED);

        Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

        assertThatSingleViolation(violations, "amount", "amount must be greater than zero");
    }

    @Test
    @DisplayName("An amount with more than 2 decimals is rejected")
    void givenAmountWithThreeDecimals_whenValidate_thenReportsAmountDigitsViolation() {
        CreatePaymentRequest request = new CreatePaymentRequest(MERCHANT_ID, new BigDecimal("10.001"), Currency.ARS, SimulationResult.APPROVED);

        Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

        assertThatSingleViolation(violations, "amount", "amount must have at most 17 integer digits and 2 decimals");
    }

    @Test
    @DisplayName("A missing currency is rejected")
    void givenNullCurrency_whenValidate_thenReportsCurrencyRequired() {
        CreatePaymentRequest request = new CreatePaymentRequest(MERCHANT_ID, AMOUNT, null, SimulationResult.APPROVED);

        Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

        assertThatSingleViolation(violations, "currency", "currency is required");
    }

    @Test
    @DisplayName("A missing simulationResult is rejected")
    void givenNullSimulationResult_whenValidate_thenReportsSimulationResultRequired() {
        CreatePaymentRequest request = new CreatePaymentRequest(MERCHANT_ID, AMOUNT, Currency.ARS, null);

        Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

        assertThatSingleViolation(violations, "simulationResult", "simulationResult is required");
    }

    private void assertThatSingleViolation(Set<ConstraintViolation<CreatePaymentRequest>> violations,
                                           String expectedField, String expectedMessage) {
        assertThat(violations)
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.getPropertyPath()).hasToString(expectedField);
                    assertThat(violation.getMessage()).isEqualTo(expectedMessage);
                });
    }
}
