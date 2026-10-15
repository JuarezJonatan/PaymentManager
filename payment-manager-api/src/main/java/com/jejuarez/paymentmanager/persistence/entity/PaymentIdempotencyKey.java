package com.jejuarez.paymentmanager.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "payment_idempotency_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentIdempotencyKey implements Persistable<String> {

    @Id
    @Column(name = "idempotency_key", length = 100)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, updatable = false)
    private Payment payment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public PaymentIdempotencyKey(String key, String requestHash, Payment payment) {
        this.key = key;
        this.requestHash = requestHash;
        this.payment = payment;
    }

    @Override
    public String getId() {
        return key;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        newEntity = false;
    }
}
