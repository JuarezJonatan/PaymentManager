package com.jejuarez.paymentmanager.persistence.repository;

import com.jejuarez.paymentmanager.persistence.entity.PaymentIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentIdempotencyKeyRepository extends JpaRepository<PaymentIdempotencyKey, String> {
}
