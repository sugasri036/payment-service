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


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PaymentWebhookController(
            PaymentService paymentService
    ) {
        this.paymentService = paymentService;
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
            // 1. CHECK SIGNATURE
            // =================================================

            if (signature == null || signature.isBlank()) {

                return ResponseEntity
                        .badRequest()
                        .body("Missing Razorpay signature");
            }


            // =================================================
            // 2. VERIFY WEBHOOK SIGNATURE
            // =================================================

            Utils.verifyWebhookSignature(
                    payload,
                    signature,
                    webhookSecret
            );


            // =================================================
            // 3. CONVERT PAYLOAD TO JSON
            // =================================================

            JSONObject webhook =
                    new JSONObject(payload);


            // =================================================
            // 4. GET EVENT TYPE
            // =================================================

            String event =
                    webhook.optString("event");


            System.out.println(
                    "Razorpay Webhook Event: " + event
            );


            // =================================================
            // 5. PAYMENT EVENTS
            // =================================================

            if (
                    "payment.authorized".equals(event)
                    || "payment.captured".equals(event)
                    || "payment.failed".equals(event)
            ) {

                JSONObject paymentEntity =
                        webhook
                                .getJSONObject("payload")
                                .getJSONObject("payment")
                                .getJSONObject("entity");


                // ---------------------------------------------
                // Razorpay Payment ID
                // ---------------------------------------------

                String paymentId =
                        paymentEntity.optString(
                                "id",
                                null
                        );


                // ---------------------------------------------
                // Razorpay Order ID
                // ---------------------------------------------

                String orderId =
                        paymentEntity.optString(
                                "order_id",
                                null
                        );


                // ---------------------------------------------
                // Razorpay status
                // ---------------------------------------------

                String razorpayStatus =
                        paymentEntity.optString(
                                "status",
                                null
                        );


                System.out.println(
                        "Razorpay Payment ID: "
                                + paymentId
                );

                System.out.println(
                        "Razorpay Order ID: "
                                + orderId
                );

                System.out.println(
                        "Razorpay Status: "
                                + razorpayStatus
                );


                // ---------------------------------------------
                // PAYMENT AUTHORIZED
                // ---------------------------------------------

                if (
                        "payment.authorized"
                                .equals(event)
                ) {

                    paymentService.updateStatus(
                            paymentId,
                            orderId,
                            "AUTHORIZED"
                    );
                }


                // ---------------------------------------------
                // PAYMENT CAPTURED
                // ---------------------------------------------

                else if (
                        "payment.captured"
                                .equals(event)
                ) {

                    paymentService.updateStatus(
                            paymentId,
                            orderId,
                            "CAPTURED"
                    );
                }


                // ---------------------------------------------
                // PAYMENT FAILED
                // ---------------------------------------------

                else if (
                        "payment.failed"
                                .equals(event)
                ) {

                    paymentService.updateStatus(
                            paymentId,
                            orderId,
                            "FAILED"
                    );
                }
            }


            // =================================================
            // 6. ORDER PAID EVENT
            // =================================================

            else if ("order.paid".equals(event)) {

                JSONObject orderEntity =
                        webhook
                                .getJSONObject("payload")
                                .getJSONObject("order")
                                .getJSONObject("entity");


                // ---------------------------------------------
                // Get Order ID
                // ---------------------------------------------

                String paidOrderId =
                        orderEntity.optString(
                                "id",
                                null
                        );


                System.out.println(
                        "Paid Order ID: "
                                + paidOrderId
                );


                // ---------------------------------------------
                // Update database
                // ---------------------------------------------

                paymentService.updateStatus(
                        null,
                        paidOrderId,
                        "PAID"
                );
            }


            // =================================================
            // 7. OTHER EVENTS
            // =================================================

            else {

                System.out.println(
                        "Unhandled Razorpay event: "
                                + event
                );
            }


            // =================================================
            // 8. SUCCESS RESPONSE
            // =================================================

            return ResponseEntity
                    .ok()
                    .body(
                            "Webhook processed successfully"
                    );


        } catch (Exception e) {

            // =================================================
            // ERROR HANDLING
            // =================================================

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