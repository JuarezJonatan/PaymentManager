package com.jejuarez.paymentmanager.persistence.repository;

import com.jejuarez.paymentmanager.persistence.entity.Payment;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Payment p SET p.status = :newStatus WHERE p.id = :id AND p.status = :expectedStatus")
    int updateStatusIfCurrent(@Param("id") String id,
                              @Param("expectedStatus") PaymentStatus expectedStatus,
                              @Param("newStatus") PaymentStatus newStatus);
}
