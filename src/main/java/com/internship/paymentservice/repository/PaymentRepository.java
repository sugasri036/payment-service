package com.internship.paymentservice.repository;

import com.internship.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    // =====================================================
    // FIND PAYMENT BY RAZORPAY ORDER ID
    // =====================================================

    Optional<Payment> findByOrderId(String orderId);


    // =====================================================
    // FIND PAYMENTS BY STATUS
    // =====================================================

    List<Payment> findByStatus(String status);


    // =====================================================
    // FIND PAYMENTS BY USER
    // =====================================================

    List<Payment> findByUserId(String userId);


    // =====================================================
    // IDEMPOTENCY
    // =====================================================

    Optional<Payment> findByIdempotencyKey(
            String idempotencyKey
    );
}