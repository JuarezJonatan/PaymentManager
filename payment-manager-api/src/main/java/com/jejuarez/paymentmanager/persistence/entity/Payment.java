package com.jejuarez.paymentmanager.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    private static final String ID_PREFIX = "pay_";

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "merchant_id", nullable = false, length = 100)
    private String merchantId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "idempotency_key", unique = true, updatable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", updatable = false, length = 64)
    private String requestHash;

    public Payment(String id, String merchantId, BigDecimal amount, Currency currency, PaymentStatus status,
                   Instant createdAt) {
        this.id = id;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Payment(String merchantId, BigDecimal amount, Currency currency, PaymentStatus status) {
        this(merchantId, amount, currency, status, null, null);
    }

    public Payment(String merchantId, BigDecimal amount, Currency currency, PaymentStatus status,
                   String idempotencyKey, String requestHash) {
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = ID_PREFIX + UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        }
    }
}
