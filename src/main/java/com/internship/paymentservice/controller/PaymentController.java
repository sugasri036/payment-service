package com.internship.paymentservice.controller;

import com.internship.paymentservice.dto.CreatePaymentRequest;
import com.internship.paymentservice.dto.RefundPaymentRequest;
import com.internship.paymentservice.dto.VerifyPaymentRequest;
import com.internship.paymentservice.entity.Payment;
import com.internship.paymentservice.service.PaymentService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
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

    @PostMapping
    public ResponseEntity<?> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {

        try {

            return ResponseEntity.ok(
                    paymentService.createPayment(
                            request
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // VERIFY PAYMENT
    // =====================================================

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {

        try {

            return ResponseEntity.ok(
                    paymentService.verifyPayment(
                            request
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // REFUND
    // =====================================================

    @PostMapping("/{id}/refund")
    public ResponseEntity<?> refundPayment(
            @PathVariable Long id,
            @Valid @RequestBody RefundPaymentRequest request) {

        try {

            return ResponseEntity.ok(
                    paymentService.refundPayment(
                            id,
                            request
                    )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // GET ALL
    // =====================================================

    @GetMapping
    public ResponseEntity<List<Payment>>
    getAllPayments() {

        return ResponseEntity.ok(
                paymentService.getAllPayments()
        );
    }


    // =====================================================
    // GET BY DATABASE ID
    // =====================================================

    @GetMapping("/{id}")
    public ResponseEntity<Payment>
    getPaymentById(
            @PathVariable Long id) {

        return paymentService
                .getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }


    // =====================================================
    // GET BY INTERNAL ORDER ID
    // =====================================================

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment>
    getPaymentByOrderId(
            @PathVariable String orderId) {

        return paymentService
                .getPaymentByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }


    // =====================================================
    // GET BY USER
    // =====================================================

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>>
    getPaymentsByUser(
            @PathVariable String userId) {

        return ResponseEntity.ok(
                paymentService
                        .getPaymentsByUser(userId)
        );
    }


    // =====================================================
    // GET BY STATUS
    // =====================================================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>>
    getPaymentsByStatus(
            @PathVariable String status) {

        return ResponseEntity.ok(
                paymentService
                        .getPaymentsByStatus(status)
        );
    }


    // =====================================================
    // RECHECK PAYMENT STATUS
    // PHASE 4
    // =====================================================

    @GetMapping("/recheck/{orderId}")
    public ResponseEntity<?> recheckPaymentStatus(
            @PathVariable String orderId) {

        try {

            return ResponseEntity.ok(
                    paymentService.recheckPaymentStatus(
                            orderId
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}