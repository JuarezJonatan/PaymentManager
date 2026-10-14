package com.jejuarez.paymentmanager.persistence.repository;

import com.jejuarez.paymentmanager.persistence.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
}
