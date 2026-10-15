package com.jejuarez.paymentmanager.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String VALIDATION_TITLE = "Validation failed";
    public static final String INTERNAL_SERVER_ERROR = "Internal server error";
    public static final String RESOURCE_NOT_FOUND = "Resource not found";
    public static final String PAYMENT_NOT_REFUNDABLE = "Payment cannot be refunded";
    public static final String IDEMPOTENCY_KEY_CONFLICT = "Idempotency key conflict";
    public static final String AN_UNEXPECTED_ERROR_OCCURRED = "An unexpected error occurred";
    public static final String UNEXPECTED_ERROR = "Unexpected error";
    public static final String INVALID = "is invalid";
    public static final String REQUEST_HAS_INVALID_FIELDS = "Request has invalid fields";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {
        return problemDetail(HttpStatus.NOT_FOUND, RESOURCE_NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PaymentNotRefundableException.class)
    public ProblemDetail handlePaymentNotRefundable(PaymentNotRefundableException exception) {
        return problemDetail(HttpStatus.CONFLICT, PAYMENT_NOT_REFUNDABLE, exception.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    public ProblemDetail handleIdempotencyKeyConflict(IdempotencyKeyConflictException exception) {
        return problemDetail(HttpStatus.CONFLICT, IDEMPOTENCY_KEY_CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error(UNEXPECTED_ERROR, exception);
        return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, AN_UNEXPECTED_ERROR_OCCURRED);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        this::fieldErrorMessage,
                        (first, second) -> first,
                        LinkedHashMap::new));
        ProblemDetail body = problemDetail(HttpStatus.BAD_REQUEST, VALIDATION_TITLE, REQUEST_HAS_INVALID_FIELDS);
        body.setProperty("errors", errors);
        return handleExceptionInternal(exception, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                parameterErrors.getFieldErrors().forEach(fieldError ->
                        errors.putIfAbsent(fieldError.getField(), fieldErrorMessage(fieldError)));
            } else {
                errors.putIfAbsent(parameterName(result), parameterErrorMessage(result));
            }
        }
        ProblemDetail body = problemDetail(HttpStatus.BAD_REQUEST, VALIDATION_TITLE, REQUEST_HAS_INVALID_FIELDS);
        body.setProperty("errors", errors);
        return handleExceptionInternal(exception, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problemDetail(HttpStatus.BAD_REQUEST, VALIDATION_TITLE, unreadableBodyDetail(exception));
        return handleExceptionInternal(exception, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    private String fieldErrorMessage(FieldError fieldError) {
        if (fieldError.isBindingFailure() && fieldError.contains(TypeMismatchException.class)) {
            Class<?> requiredType = fieldError.unwrap(TypeMismatchException.class).getRequiredType();
            if (requiredType != null && requiredType.isEnum()) {
                return "Invalid value '%s'. Accepted values: %s".formatted(
                        fieldError.getRejectedValue(), Arrays.toString(requiredType.getEnumConstants()));
            }
            return "Invalid value '%s'".formatted(fieldError.getRejectedValue());
        }
        return Objects.requireNonNullElse(fieldError.getDefaultMessage(), INVALID);
    }

    private String parameterName(ParameterValidationResult result) {
        RequestHeader requestHeader = result.getMethodParameter().getParameterAnnotation(RequestHeader.class);
        if (requestHeader != null && !requestHeader.name().isEmpty()) {
            return requestHeader.name();
        }
        return Objects.requireNonNullElse(result.getMethodParameter().getParameterName(), INVALID);
    }

    private String parameterErrorMessage(ParameterValidationResult result) {
        return result.getResolvableErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(INVALID);
    }

    private String unreadableBodyDetail(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof InvalidFormatException invalidFormat
                && invalidFormat.getTargetType() != null
                && invalidFormat.getTargetType().isEnum()) {
            return "Invalid value '%s' for field '%s'. Accepted values: %s".formatted(
                    invalidFormat.getValue(),
                    fieldPath(invalidFormat),
                    Arrays.toString(invalidFormat.getTargetType().getEnumConstants()));
        }
        if (exception.getCause() instanceof MismatchedInputException mismatchedInput
                && !mismatchedInput.getPath().isEmpty()) {
            return "Invalid value for field '%s'".formatted(fieldPath(mismatchedInput));
        }
        return "Malformed JSON request body";
    }

    private String fieldPath(JacksonException exception) {
        return exception.getPath().stream()
                .map(JacksonException.Reference::getPropertyName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
    }

    private ProblemDetail problemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
