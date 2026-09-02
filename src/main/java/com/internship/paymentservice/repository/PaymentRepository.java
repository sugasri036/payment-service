package com.internship.paymentservice.repository;

import com.internship.paymentservice.entity.Payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment>
    findByIdempotencyKey(
            String idempotencyKey
    );

    Optional<Payment>
    findByRazorpayOrderId(
            String razorpayOrderId
    );

    Optional<Payment>
    findByOrderId(
            String orderId
    );

    List<Payment>
    findByUserId(
            String userId
    );

    List<Payment>
    findByStatus(
            String status
    );
}