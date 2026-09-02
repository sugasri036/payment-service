package com.internship.paymentservice.service;

import com.internship.paymentservice.entity.Payment;
import com.internship.paymentservice.repository.PaymentRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentRecheckService {

    private final PaymentRepository paymentRepository;

    private final PaymentService paymentService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PaymentRecheckService(
            PaymentRepository paymentRepository,
            PaymentService paymentService) {

        this.paymentRepository =
                paymentRepository;

        this.paymentService =
                paymentService;
    }


    // =====================================================
    // RECHECK CREATED PAYMENTS
    // =====================================================

    public void recheckPayments() {

        List<Payment> payments =
                paymentRepository.findByStatus("CREATED");


        // -------------------------------------------------
        // NO PAYMENTS
        // -------------------------------------------------

        if (payments.isEmpty()) {

            System.out.println();

            System.out.println(
                    "No CREATED payments to recheck."
            );

            return;
        }


        // -------------------------------------------------
        // BATCH INFORMATION
        // -------------------------------------------------

        System.out.println();

        System.out.println(
                "===================================="
        );

        System.out.println(
                "PAYMENT RECHECK BATCH"
        );

        System.out.println(
                "Payments found: "
                        + payments.size()
        );

        System.out.println(
                "===================================="
        );


        // -------------------------------------------------
        // PROCESS EACH PAYMENT
        // -------------------------------------------------

        for (Payment payment : payments) {

            try {

                System.out.println();

                System.out.println(
                        "Rechecking Payment: "
                                + payment.getPaymentId()
                );

                System.out.println(
                        "Order ID: "
                                + payment.getOrderId()
                );


                // =================================================
                // CALL EXISTING RECHECK LOGIC
                // =================================================

                paymentService.recheckPaymentStatus(
                        payment.getOrderId()
                );


                System.out.println(
                        "Recheck completed: "
                                + payment.getPaymentId()
                );


            } catch (Exception e) {

                System.out.println();

                System.out.println(
                        "Recheck failed for payment: "
                                + payment.getPaymentId()
                );

                System.out.println(
                        "Error: "
                                + e.getMessage()
                );
            }
        }
    }
}