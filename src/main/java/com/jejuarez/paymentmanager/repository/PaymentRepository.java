package com.jejuarez.paymentmanager.repository;

import com.jejuarez.paymentmanager.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
}
