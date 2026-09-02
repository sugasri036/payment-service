package com.internship.paymentservice.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentBatchJob {

    private final PaymentRecheckService paymentRecheckService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PaymentBatchJob(
            PaymentRecheckService paymentRecheckService) {

        this.paymentRecheckService =
                paymentRecheckService;
    }


    // =====================================================
    // AUTOMATIC PAYMENT RECHECK
    // =====================================================

    @Scheduled(fixedDelay = 30000)
    public void runPaymentRecheck() {

        System.out.println();

        System.out.println(
                "===================================="
        );

        System.out.println(
                "AUTOMATIC PAYMENT BATCH STARTED"
        );

        System.out.println(
                "===================================="
        );


        paymentRecheckService.recheckPayments();
    }
}