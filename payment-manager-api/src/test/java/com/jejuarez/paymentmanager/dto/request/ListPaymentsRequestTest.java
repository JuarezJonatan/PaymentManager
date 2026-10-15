package com.jejuarez.paymentmanager.dto.request;

import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ListPaymentsRequest")
class ListPaymentsRequestTest {

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
    @DisplayName("Defaults to the first page of 20 payments when page and size are missing")
    void givenMissingPageAndSize_whenCreated_thenUsesDefaults() {
        ListPaymentsRequest request = new ListPaymentsRequest(PaymentStatus.APPROVED, null, null);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    @DisplayName("Accepts sizes within the allowed range")
    void givenSizeWithinRange_whenValidate_thenHasNoViolations(int size) {
        ListPaymentsRequest request = new ListPaymentsRequest(null, 0, size);

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    @DisplayName("Rejects sizes outside the allowed range")
    void givenSizeOutOfRange_whenValidate_thenReportsSizeViolation(int size) {
        ListPaymentsRequest request = new ListPaymentsRequest(null, 0, size);

        assertThat(validator.validate(request))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("size must be between 1 and 100");
    }

    @Test
    @DisplayName("Rejects a negative page")
    void givenNegativePage_whenValidate_thenReportsPageViolation() {
        ListPaymentsRequest request = new ListPaymentsRequest(null, -1, 20);

        assertThat(validator.validate(request))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("page must be greater than or equal to zero");
    }
}
