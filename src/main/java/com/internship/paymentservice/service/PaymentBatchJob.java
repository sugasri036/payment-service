package com.internship.paymentservice.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "payment.batch.enabled",
        havingValue = "true",
        matchIfMissing = false
)
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

    @Scheduled(
            fixedDelayString =
                    "${payment.batch.fixed-delay:30000}"
    )
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


        try {

            paymentRecheckService.recheckPayments();

        } catch (Exception e) {

            System.out.println(
                    "Payment batch failed: "
                            + e.getMessage()
            );
        }
    }
}