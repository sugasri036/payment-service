package com.internship.paymentservice.controller;

import com.internship.paymentservice.service.PaymentService;
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

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    public String webhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {

            System.out.println("====================================");
            System.out.println("RAZORPAY WEBHOOK RECEIVED");
            System.out.println("Payload:");
            System.out.println(payload);
            System.out.println("====================================");

            // =====================================================
            // 1. Verify Razorpay webhook signature
            // =====================================================

            String expectedSignature =
                    generateSignature(payload, webhookSecret);

            if (!expectedSignature.equals(signature)) {

                System.out.println("INVALID WEBHOOK SIGNATURE");

                throw new RuntimeException(
                        "Invalid Razorpay webhook signature"
                );
            }

            System.out.println("WEBHOOK SIGNATURE VERIFIED");


            // =====================================================
            // 2. Successful payment
            // =====================================================

            if (payload.contains("\"event\":\"order.paid\"")
                    || payload.contains("\"event\":\"payment.captured\"")) {

                System.out.println("SUCCESS PAYMENT EVENT RECEIVED");

                String orderId = extractOrderId(payload);
                String paymentId = extractPaymentId(payload);

                System.out.println("Order ID: " + orderId);
                System.out.println("Payment ID: " + paymentId);

                if (orderId != null) {

                    paymentService.updateStatus(
                            orderId,
                            "PAID",
                            paymentId
                    );

                    System.out.println(
                            "PAYMENT STATUS UPDATED TO PAID"
                    );

                } else {

                    System.out.println(
                            "ORDER ID NOT FOUND IN WEBHOOK"
                    );
                }
            }


            // =====================================================
            // 3. Failed payment
            // =====================================================

            if (payload.contains("\"event\":\"payment.failed\"")) {

                System.out.println(
                        "PAYMENT FAILED EVENT RECEIVED"
                );

                String orderId = extractOrderId(payload);

                System.out.println("Order ID: " + orderId);

                if (orderId != null) {

                    paymentService.updateStatus(
                            orderId,
                            "FAILED",
                            null
                    );

                    System.out.println(
                            "PAYMENT STATUS UPDATED TO FAILED"
                    );

                } else {

                    System.out.println(
                            "ORDER ID NOT FOUND IN WEBHOOK"
                    );
                }
            }


            return "Webhook processed successfully";


        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Webhook processing failed",
                    e
            );
        }
    }


    // =========================================================
    // Generate HMAC SHA256 signature
    // =========================================================

    private String generateSignature(
            String payload,
            String secret) throws Exception {

        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKeySpec secretKey =
                new SecretKeySpec(
                        secret.getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"
                );

        mac.init(secretKey);

        byte[] hash =
                mac.doFinal(
                        payload.getBytes(StandardCharsets.UTF_8)
                );

        StringBuilder hex = new StringBuilder();

        for (byte b : hash) {

            hex.append(
                    String.format("%02x", b)
            );
        }

        return hex.toString();
    }


    // =========================================================
    // Extract Order ID
    // =========================================================

    private String extractOrderId(String payload) {

        // -----------------------------------------------------
        // payment.captured / payment.failed
        //
        // Example:
        // "order_id":"order_ABC123"
        // -----------------------------------------------------

        String key = "\"order_id\":\"";

        int start = payload.indexOf(key);

        if (start != -1) {

            start += key.length();

            int end =
                    payload.indexOf("\"", start);

            if (end != -1) {

                return payload.substring(start, end);
            }
        }


        // -----------------------------------------------------
        // order.paid
        //
        // Example:
        // "id":"order_ABC123"
        // -----------------------------------------------------

        key = "\"id\":\"order_";

        start = payload.indexOf(key);

        if (start != -1) {

            // Move to the beginning of "order_"
            start += 6;

            int end =
                    payload.indexOf("\"", start);

            if (end != -1) {

                return payload.substring(start, end);
            }
        }

        return null;
    }


    // =========================================================
    // Extract Payment ID
    // =========================================================

    private String extractPaymentId(String payload) {

        // Example:
        // "id":"pay_ABC123"

        String key = "\"id\":\"pay_";

        int start = payload.indexOf(key);

        if (start == -1) {

            return null;
        }

        // Move to the beginning of "pay_"
        start += 6;

        int end =
                payload.indexOf("\"", start);

        if (end == -1) {

            return null;
        }

        return payload.substring(start, end);
    }
}