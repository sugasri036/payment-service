package com.internship.paymentservice.controller;

import com.internship.paymentservice.service.PaymentService;

import com.razorpay.Utils;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/webhook")
public class PaymentWebhookController {

    private final PaymentService paymentService;


    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;


    public PaymentWebhookController(
            PaymentService paymentService) {

        this.paymentService =
                paymentService;
    }


    // =====================================================
    // RAZORPAY WEBHOOK
    // =====================================================

    @PostMapping
    public ResponseEntity<String> handleWebhook(

            @RequestBody String payload,

            @RequestHeader(
                    value = "X-Razorpay-Signature",
                    required = false
            )
            String signature

    ) {

        try {

            // =================================================
            // CHECK SIGNATURE
            // =================================================

            if (signature == null ||
                    signature.isBlank()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Missing Razorpay signature"
                        );
            }


            // =================================================
            // VERIFY WEBHOOK
            // =================================================

            Utils.verifyWebhookSignature(
                    payload,
                    signature,
                    webhookSecret
            );


            // =================================================
            // PARSE JSON
            // =================================================

            JSONObject webhook =
                    new JSONObject(payload);


            String event =
                    webhook.optString(
                            "event"
                    );


            System.out.println(
                    "Razorpay Webhook Event: "
                            + event
            );


            // =================================================
            // PAYMENT EVENTS
            // =================================================

            if (
                    "payment.authorized".equals(event)
                            ||
                    "payment.captured".equals(event)
                            ||
                    "payment.failed".equals(event)
            ) {

                JSONObject paymentEntity =
                        webhook
                                .getJSONObject("payload")
                                .getJSONObject("payment")
                                .getJSONObject("entity");


                String razorpayPaymentId =
                        paymentEntity.optString(
                                "id",
                                null
                        );


                String razorpayOrderId =
                        paymentEntity.optString(
                                "order_id",
                                null
                        );


                if (razorpayOrderId == null ||
                        razorpayOrderId.isBlank()) {

                    throw new RuntimeException(
                            "Razorpay order ID missing from webhook"
                    );
                }


                if (razorpayPaymentId == null ||
                        razorpayPaymentId.isBlank()) {

                    throw new RuntimeException(
                            "Razorpay payment ID missing from webhook"
                    );
                }


                String status;


                // -------------------------------------------------
                // AUTHORIZED
                // -------------------------------------------------

                if (
                        "payment.authorized"
                                .equals(event)
                ) {

                    status =
                            "AUTHORIZED";
                }


                // -------------------------------------------------
                // CAPTURED
                // -------------------------------------------------

                else if (
                        "payment.captured"
                                .equals(event)
                ) {

                    status =
                            "CAPTURED";
                }


                // -------------------------------------------------
                // FAILED
                // -------------------------------------------------

                else {

                    status =
                            "FAILED";
                }


                /*
                 * IMPORTANT:
                 *
                 * PaymentService.updateStatus() expects:
                 *
                 * 1. razorpayOrderId
                 * 2. status
                 * 3. razorpayPaymentId
                 */

                paymentService.updateStatus(

                        razorpayOrderId,

                        status,

                        razorpayPaymentId

                );
            }


            // =================================================
            // ORDER PAID
            // =================================================

            else if (
                    "order.paid".equals(event)
            ) {

                JSONObject orderEntity =
                        webhook
                                .getJSONObject("payload")
                                .getJSONObject("order")
                                .getJSONObject("entity");


                String paidOrderId =
                        orderEntity.optString(
                                "id",
                                null
                        );


                if (paidOrderId == null ||
                        paidOrderId.isBlank()) {

                    throw new RuntimeException(
                            "Razorpay order ID missing"
                    );
                }


                /*
                 * For order.paid we only have the
                 * Razorpay order ID here.
                 *
                 * PaymentService will find the
                 * corresponding local payment.
                 */

                paymentService.updateStatus(

                        paidOrderId,

                        "PAID",

                        null

                );
            }


            // =================================================
            // REFUND EVENTS
            // =================================================

            else if (
                    "refund.created".equals(event)
                            ||
                    "refund.processed".equals(event)
                            ||
                    "refund.failed".equals(event)
            ) {

                System.out.println(
                        "Refund webhook received: "
                                + event
                );

                /*
                 * Refund processing will be handled in
                 * the next payment-hardening phase.
                 */
            }


            // =================================================
            // OTHER EVENTS
            // =================================================

            else {

                System.out.println(
                        "Unhandled Razorpay event: "
                                + event
                );
            }


            // =================================================
            // SUCCESS
            // =================================================

            return ResponseEntity
                    .ok()
                    .body(
                            "Webhook processed successfully"
                    );


        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Webhook processing failed: "
                                    + e.getMessage()
                    );
        }
    }
}