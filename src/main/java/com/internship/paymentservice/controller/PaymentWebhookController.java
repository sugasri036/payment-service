package com.internship.paymentservice.controller;

import com.internship.paymentservice.service.PaymentService;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/payments")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    public PaymentWebhookController(
            PaymentService paymentService) {

        this.paymentService = paymentService;
    }

    // =====================================================
    // RAZORPAY WEBHOOK
    // =====================================================

    @PostMapping("/webhook")
    public String webhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature")
            String signature) {

        try {

            System.out.println();
            System.out.println("====================================");
            System.out.println("RAZORPAY WEBHOOK RECEIVED");
            System.out.println("====================================");

            // =================================================
            // 1. VERIFY SIGNATURE
            // =================================================

            String expectedSignature =
                    generateSignature(
                            payload,
                            webhookSecret
                    );

            if (!expectedSignature.equals(signature)) {

                System.out.println(
                        "INVALID WEBHOOK SIGNATURE"
                );

                throw new RuntimeException(
                        "Invalid Razorpay webhook signature"
                );
            }

            System.out.println(
                    "WEBHOOK SIGNATURE VERIFIED"
            );


            // =================================================
            // 2. PARSE JSON
            // =================================================

            JSONObject webhook =
                    new JSONObject(payload);

            String event =
                    webhook.getString("event");

            System.out.println(
                    "EVENT: " + event
            );


            // =================================================
            // 3. PAYMENT CAPTURED
            // =================================================

            if ("payment.captured".equals(event)) {

                JSONObject paymentEntity =
                        webhook
                                .getJSONObject("payload")
                                .getJSONObject("payment")
                                .getJSONObject("entity");

                String paymentId =
                        paymentEntity.getString("id");

                String orderId =
                        paymentEntity.getString("order_id");

                System.out.println(
                        "Payment ID: " + paymentId
                );

                System.out.println(
                        "Order ID: " + orderId
                );

                paymentService.updateStatus(
                        orderId,
                        "PAID",
                        paymentId
                );

                System.out.println(
                        "PAYMENT STATUS UPDATED TO PAID"
                );
            }


            // =================================================
            // 4. PAYMENT FAILED
            // =================================================

            else if ("payment.failed".equals(event)) {

                JSONObject paymentEntity =
                        webhook
                                .getJSONObject("payload")
                                .getJSONObject("payment")
                                .getJSONObject("entity");

                String orderId =
                        paymentEntity.getString("order_id");

                System.out.println(
                        "Order ID: " + orderId
                );

                paymentService.updateStatus(
                        orderId,
                        "FAILED",
                        null
                );

                System.out.println(
                        "PAYMENT STATUS UPDATED TO FAILED"
                );
            }


            // =================================================
            // 5. ORDER PAID
            // =================================================

            else if ("order.paid".equals(event)) {

                JSONObject orderEntity =
                        webhook
                                .getJSONObject("payload")
                                .getJSONObject("order")
                                .getJSONObject("entity");

                String orderId =
                        orderEntity.getString("id");

                System.out.println(
                        "Order ID: " + orderId
                );

                paymentService.updateStatus(
                        orderId,
                        "PAID",
                        null
                );

                System.out.println(
                        "ORDER STATUS UPDATED TO PAID"
                );
            }


            // =================================================
            // 6. OTHER EVENTS
            // =================================================

            else {

                System.out.println(
                        "EVENT RECEIVED BUT NOT HANDLED: "
                                + event
                );
            }


            System.out.println(
                    "WEBHOOK PROCESSING COMPLETED"
            );

            System.out.println(
                    "===================================="
            );


            return "Webhook processed successfully";


        } catch (Exception e) {

            System.out.println(
                    "WEBHOOK PROCESSING FAILED"
            );

            e.printStackTrace();

            throw new RuntimeException(
                    "Webhook processing failed",
                    e
            );
        }
    }


    // =====================================================
    // HMAC SHA256 SIGNATURE
    // =====================================================

    private String generateSignature(
            String payload,
            String secret)
            throws Exception {

        Mac mac =
                Mac.getInstance("HmacSHA256");

        SecretKeySpec secretKey =
                new SecretKeySpec(
                        secret.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        "HmacSHA256"
                );

        mac.init(secretKey);

        byte[] hash =
                mac.doFinal(
                        payload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        StringBuilder hex =
                new StringBuilder();

        for (byte b : hash) {

            hex.append(
                    String.format(
                            "%02x",
                            b
                    )
            );
        }

        return hex.toString();
    }
}