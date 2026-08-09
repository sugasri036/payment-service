package com.internship.paymentservice.controller;

import com.internship.paymentservice.dto.CreatePaymentRequest;
import com.internship.paymentservice.entity.Payment;
import com.internship.paymentservice.service.PaymentService;

import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;


    public PaymentController(
            PaymentService paymentService) {

        this.paymentService =
                paymentService;
    }


    // =====================================================
    // CREATE PAYMENT
    // =====================================================

    @PostMapping("/create")
    public Payment createPayment(
            @RequestBody CreatePaymentRequest request)
            throws Exception {

        return paymentService.createPayment(
                request
        );
    }


    // =====================================================
    // UPDATE PAYMENT STATUS
    // =====================================================

    @PostMapping("/{orderId}/status")
    public Payment updateStatus(
            @PathVariable String orderId,
            @RequestParam String status) {

        return paymentService.updateStatus(
                orderId,
                status,
                null
        );
    }


    // =====================================================
    // GET ALL PAYMENTS
    // =====================================================

    @GetMapping
    public List<Payment> getAllPayments() {

        return paymentService.getAllPayments();
    }


    // =====================================================
    // GET PAYMENT BY ORDER ID
    // =====================================================

    @GetMapping("/order/{orderId}")
    public Payment getPaymentByOrderId(
            @PathVariable String orderId) {

        return paymentService
                .getPaymentByOrderId(
                        orderId
                );
    }


    // =====================================================
    // GET PAYMENTS BY USER
    // =====================================================

    @GetMapping("/user/{userId}")
    public List<Payment> getPaymentsByUser(
            @PathVariable String userId) {

        return paymentService
                .getPaymentsByUser(
                        userId
                );
    }


    // =====================================================
    // GET PAYMENTS BY STATUS
    // =====================================================

    @GetMapping("/status/{status}")
    public List<Payment> getPaymentsByStatus(
            @PathVariable String status) {

        return paymentService
                .getPaymentsByStatus(
                        status
                );
    }


    // =====================================================
    // REFUND PAYMENT
    // =====================================================

    @PostMapping("/{orderId}/refund")
    public Payment refundPayment(
            @PathVariable String orderId)
            throws Exception {

        return paymentService.refundPayment(
                orderId
        );
    }
}