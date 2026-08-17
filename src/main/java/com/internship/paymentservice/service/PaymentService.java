package com.internship.paymentservice.service;

import com.internship.paymentservice.dto.CreatePaymentRequest;
import com.internship.paymentservice.entity.Payment;
import com.internship.paymentservice.repository.PaymentRepository;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public PaymentService(
            PaymentRepository paymentRepository) {

        this.paymentRepository = paymentRepository;
    }


    // =====================================================
    // RAZORPAY CONFIGURATION CHECK
    // =====================================================

    @PostConstruct
    public void checkRazorpayConfig() {

        System.out.println();
        System.out.println(
                "===================================="
        );

        System.out.println(
                "RAZORPAY CONFIG CHECK"
        );

        System.out.println(
                "===================================="
        );

        System.out.println(
                "Key ID: [" + keyId + "]"
        );

        System.out.println(
                "Key ID length: "
                        + (keyId == null
                        ? 0
                        : keyId.length())
        );

        System.out.println(
                "Key ID starts with rzp_test_: "
                        + (keyId != null
                        && keyId.startsWith("rzp_test_"))
        );

        System.out.println(
                "Key Secret length: "
                        + (keySecret == null
                        ? 0
                        : keySecret.length())
        );

        System.out.println(
                "Key Secret first 4 chars: "
                        + (keySecret == null
                        ? "NULL"
                        : keySecret.substring(
                                0,
                                Math.min(
                                        4,
                                        keySecret.length()
                                )
                        ))
        );

        System.out.println(
                "Key Secret last 4 chars: "
                        + (keySecret == null
                        ? "NULL"
                        : keySecret.substring(
                                Math.max(
                                        0,
                                        keySecret.length() - 4
                                )
                        ))
        );

        System.out.println(
                "===================================="
        );
    }


    // =====================================================
    // CREATE PAYMENT
    // =====================================================

    public Payment createPayment(
            CreatePaymentRequest request,
            String idempotencyKey)
            throws Exception {

        // -------------------------------------------------
        // VALIDATE IDEMPOTENCY KEY
        // -------------------------------------------------

        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Idempotency-Key header is required"
            );
        }


        // -------------------------------------------------
        // CHECK EXISTING PAYMENT
        // -------------------------------------------------

        Payment existingPayment =
                paymentRepository
                        .findByIdempotencyKey(
                                idempotencyKey
                        )
                        .orElse(null);

        if (existingPayment != null) {

            System.out.println(
                    "IDEMPOTENT REQUEST: "
                            + "Returning existing payment for key: "
                            + idempotencyKey
            );

            return existingPayment;
        }


        // -------------------------------------------------
        // CONVERT AMOUNT TO PAISE
        // -------------------------------------------------

        int amountInPaise =
                (int) Math.round(
                        request.getAmount() * 100
                );


        // -------------------------------------------------
        // CREATE RAZORPAY ORDER REQUEST
        // -------------------------------------------------

        JSONObject orderRequest =
                new JSONObject();

        orderRequest.put(
                "amount",
                amountInPaise
        );

        orderRequest.put(
                "currency",
                "INR"
        );

        orderRequest.put(
                "receipt",
                "receipt_"
                        + System.currentTimeMillis()
        );


        // -------------------------------------------------
        // CREATE HEADERS
        // -------------------------------------------------

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBasicAuth(
                keyId.trim(),
                keySecret.trim(),
                StandardCharsets.UTF_8
        );

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );


        // -------------------------------------------------
        // CREATE HTTP REQUEST
        // -------------------------------------------------

        HttpEntity<String> entity =
                new HttpEntity<>(
                        orderRequest.toString(),
                        headers
                );


        // -------------------------------------------------
        // LOG
        // -------------------------------------------------

        System.out.println();
        System.out.println(
                "===================================="
        );

        System.out.println(
                "CREATING RAZORPAY ORDER"
        );

        System.out.println(
                "Amount: ₹"
                        + request.getAmount()
        );

        System.out.println(
                "Amount in paise: "
                        + amountInPaise
        );

        System.out.println(
                "===================================="
        );


        // -------------------------------------------------
        // CALL RAZORPAY
        // -------------------------------------------------

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        "https://api.razorpay.com/v1/orders",
                        entity,
                        String.class
                );


        // -------------------------------------------------
        // READ RAZORPAY RESPONSE
        // -------------------------------------------------

        JSONObject razorpayOrder =
                new JSONObject(
                        response.getBody()
                );

        String razorpayOrderId =
                razorpayOrder.getString("id");


        System.out.println(
                "RAZORPAY ORDER CREATED: "
                        + razorpayOrderId
        );


        // -------------------------------------------------
        // CREATE DATABASE PAYMENT
        // -------------------------------------------------

        Payment payment =
                new Payment();

        payment.setOrderId(
                razorpayOrderId
        );

        payment.setAmount(
                request.getAmount()
        );

        payment.setUserId(
                request.getUserId()
        );

        payment.setStatus(
                "CREATED"
        );

        payment.setCreatedAt(
                LocalDateTime.now()
        );

        payment.setIdempotencyKey(
                idempotencyKey
        );


        // -------------------------------------------------
        // SAVE PAYMENT
        // -------------------------------------------------

        return paymentRepository.save(
                payment
        );
    }


    // =====================================================
    // UPDATE PAYMENT STATUS
    // =====================================================

    public Payment updateStatus(
            String orderId,
            String status,
            String paymentId) {

        Payment payment =
                findPaymentByOrderId(orderId);


        // -------------------------------------------------
        // UPDATE STATUS
        // -------------------------------------------------

        if (status != null
                && !status.isBlank()) {

            payment.setStatus(
                    status.trim().toUpperCase()
            );
        }


        // -------------------------------------------------
        // UPDATE PAYMENT ID
        // -------------------------------------------------

        if (paymentId != null
                && !paymentId.isBlank()) {

            payment.setPaymentId(
                    paymentId.trim()
            );
        }


        System.out.println();
        System.out.println(
                "===================================="
        );

        System.out.println(
                "PAYMENT STATUS UPDATED"
        );

        System.out.println(
                "Order ID: "
                        + payment.getOrderId()
        );

        System.out.println(
                "Status: "
                        + payment.getStatus()
        );

        System.out.println(
                "Payment ID: "
                        + payment.getPaymentId()
        );

        System.out.println(
                "===================================="
        );


        return paymentRepository.save(
                payment
        );
    }


    // =====================================================
    // GET ALL PAYMENTS
    // =====================================================

    public List<Payment> getAllPayments() {

        return paymentRepository.findAll();
    }


    // =====================================================
    // GET PAYMENT BY ORDER ID
    // =====================================================

    public Payment getPaymentByOrderId(
            String orderId) {

        return findPaymentByOrderId(
                orderId
        );
    }


    // =====================================================
    // COMMON ORDER ID LOOKUP
    // =====================================================
    //
    // FIX:
    //
    // 1. Try direct repository lookup.
    // 2. If direct lookup fails, perform a normalized
    //    comparison against the existing database records.
    //
    // This protects against:
    //
    // - leading/trailing whitespace
    // - hidden whitespace characters
    // - case differences
    // - old database records
    // - values created before the current repository logic
    //
    // =====================================================

    private Payment findPaymentByOrderId(
            String orderId) {

        if (orderId == null
                || orderId.isBlank()) {

            throw new RuntimeException(
                    "Order ID is required"
            );
        }

        String requestedOrderId =
                normalizeOrderId(orderId);


        System.out.println();
        System.out.println(
                "===================================="
        );

        System.out.println(
                "PAYMENT LOOKUP"
        );

        System.out.println(
                "Requested Order ID: ["
                        + requestedOrderId
                        + "]"
        );

        System.out.println(
                "===================================="
        );


        // =================================================
        // STEP 1: NORMAL DATABASE LOOKUP
        // =================================================

        Payment payment =
                paymentRepository
                        .findByOrderId(
                                requestedOrderId
                        )
                        .orElse(null);


        if (payment != null) {

            System.out.println(
                    "PAYMENT FOUND USING DIRECT LOOKUP: "
                            + payment.getOrderId()
            );

            return payment;
        }


        // =================================================
        // STEP 2: FALLBACK NORMALIZED LOOKUP
        // =================================================
        //
        // If the database equality lookup failed,
        // check the actual Payment objects already stored
        // in the database.
        //
        // =================================================

        System.out.println(
                "DIRECT LOOKUP FAILED"
        );

        System.out.println(
                "STARTING NORMALIZED FALLBACK LOOKUP..."
        );


        List<Payment> allPayments =
                paymentRepository.findAll();


        System.out.println(
                "TOTAL PAYMENTS IN DATABASE: "
                        + allPayments.size()
        );


        for (Payment candidate :
                allPayments) {

            String candidateOrderId =
                    normalizeOrderId(
                            candidate.getOrderId()
                    );


            System.out.println(
                    "Comparing requested ["
                            + requestedOrderId
                            + "] with database ["
                            + candidateOrderId
                            + "]"
            );


            if (requestedOrderId.equalsIgnoreCase(
                    candidateOrderId)) {

                System.out.println(
                        "PAYMENT FOUND USING FALLBACK LOOKUP: "
                                + candidate.getOrderId()
                );

                return candidate;
            }
        }


        // =================================================
        // STEP 3: NOT FOUND
        // =================================================

        System.out.println(
                "PAYMENT NOT FOUND FOR ORDER: ["
                        + requestedOrderId
                        + "]"
        );


        throw new RuntimeException(
                "Payment not found for order: "
                        + requestedOrderId
        );
    }


    // =====================================================
    // NORMALIZE ORDER ID
    // =====================================================

    private String normalizeOrderId(
            String orderId) {

        if (orderId == null) {
            return "";
        }

        return orderId
                .replace("\u200B", "")
                .replace("\uFEFF", "")
                .trim();
    }


    // =====================================================
    // GET PAYMENTS BY USER
    // =====================================================

    public List<Payment> getPaymentsByUser(
            String userId) {

        return paymentRepository
                .findByUserId(userId);
    }


    // =====================================================
    // GET PAYMENTS BY STATUS
    // =====================================================

    public List<Payment> getPaymentsByStatus(
            String status) {

        return paymentRepository
                .findByStatus(status);
    }


    // =====================================================
    // REFUND PAYMENT
    // =====================================================

    public Payment refundPayment(
            String orderId)
            throws Exception {

        Payment payment =
                findPaymentByOrderId(orderId);


        // -------------------------------------------------
        // 1. ALREADY REFUNDED CHECK
        // -------------------------------------------------

        if ("REFUNDED".equalsIgnoreCase(
                payment.getStatus())) {

            throw new RuntimeException(
                    "Payment has already been refunded"
            );
        }


        // -------------------------------------------------
        // 2. PAYMENT MUST BE PAID
        // -------------------------------------------------

        if (!"PAID".equalsIgnoreCase(
                payment.getStatus())) {

            throw new RuntimeException(
                    "Only PAID payments can be refunded"
            );
        }


        // -------------------------------------------------
        // 3. NO RAZORPAY PAYMENT ID
        // -------------------------------------------------

        if (payment.getPaymentId() == null
                || payment.getPaymentId().isBlank()) {

            System.out.println();
            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "TEST REFUND MODE"
            );

            System.out.println(
                    "NO RAZORPAY PAYMENT ID"
            );

            System.out.println(
                    "Order ID: "
                            + payment.getOrderId()
            );

            System.out.println(
                    "Amount: ₹"
                            + payment.getAmount()
            );


            String testRefundId =
                    "test_refund_"
                            + System.currentTimeMillis();


            payment.setRefundId(
                    testRefundId
            );

            payment.setRefundAmount(
                    payment.getAmount()
            );

            payment.setStatus(
                    "REFUNDED"
            );


            Payment savedPayment =
                    paymentRepository.save(
                            payment
                    );


            System.out.println(
                    "TEST REFUND SUCCESSFUL"
            );

            System.out.println(
                    "Refund ID: "
                            + testRefundId
            );

            System.out.println(
                    "===================================="
            );


            return savedPayment;
        }


        // -------------------------------------------------
        // 4. CALCULATE REFUND AMOUNT
        // -------------------------------------------------

        int refundAmountInPaise =
                (int) Math.round(
                        payment.getAmount() * 100
                );


        // =================================================
        // SPECIAL TEST REFUND
        // =================================================

        if ("pay_test123".equalsIgnoreCase(
                payment.getPaymentId())) {

            System.out.println();
            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "TEST REFUND MODE"
            );

            System.out.println(
                    "Payment ID: "
                            + payment.getPaymentId()
            );

            System.out.println(
                    "Amount: ₹"
                            + payment.getAmount()
            );


            String testRefundId =
                    "test_refund_"
                            + System.currentTimeMillis();


            payment.setRefundId(
                    testRefundId
            );

            payment.setRefundAmount(
                    payment.getAmount()
            );

            payment.setStatus(
                    "REFUNDED"
            );


            Payment savedPayment =
                    paymentRepository.save(
                            payment
                    );


            System.out.println(
                    "TEST REFUND SUCCESSFUL"
            );

            System.out.println(
                    "Order ID: "
                            + orderId
            );

            System.out.println(
                    "Payment ID: "
                            + payment.getPaymentId()
            );

            System.out.println(
                    "Refund ID: "
                            + testRefundId
            );

            System.out.println(
                    "Refund Amount: ₹"
                            + payment.getAmount()
            );

            System.out.println(
                    "===================================="
            );


            return savedPayment;
        }


        // =================================================
        // REAL RAZORPAY REFUND
        // =================================================

        JSONObject refundRequest =
                new JSONObject();

        refundRequest.put(
                "amount",
                refundAmountInPaise
        );


        // -------------------------------------------------
        // CREATE AUTH HEADERS
        // -------------------------------------------------

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBasicAuth(
                keyId.trim(),
                keySecret.trim(),
                StandardCharsets.UTF_8
        );

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );


        // -------------------------------------------------
        // CREATE HTTP REQUEST
        // -------------------------------------------------

        HttpEntity<String> entity =
                new HttpEntity<>(
                        refundRequest.toString(),
                        headers
                );


        // -------------------------------------------------
        // RAZORPAY REFUND URL
        // -------------------------------------------------

        String refundUrl =
                "https://api.razorpay.com/v1/payments/"
                        + payment.getPaymentId()
                        + "/refund";


        System.out.println();
        System.out.println(
                "===================================="
        );

        System.out.println(
                "CALLING RAZORPAY REFUND API"
        );

        System.out.println(
                "Payment ID: "
                        + payment.getPaymentId()
        );

        System.out.println(
                "Refund Amount: ₹"
                        + payment.getAmount()
        );

        System.out.println(
                "===================================="
        );


        // -------------------------------------------------
        // CALL RAZORPAY
        // -------------------------------------------------

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        refundUrl,
                        entity,
                        String.class
                );


        // -------------------------------------------------
        // READ RESPONSE
        // -------------------------------------------------

        JSONObject refund =
                new JSONObject(
                        response.getBody()
                );


        String refundId =
                refund.getString("id");


        int actualRefundAmountInPaise =
                refund.getInt("amount");


        double actualRefundAmount =
                actualRefundAmountInPaise
                        / 100.0;


        // -------------------------------------------------
        // UPDATE DATABASE
        // -------------------------------------------------

        payment.setRefundId(
                refundId
        );

        payment.setRefundAmount(
                actualRefundAmount
        );

        payment.setStatus(
                "REFUNDED"
        );


        Payment savedPayment =
                paymentRepository.save(
                        payment
                );


        // -------------------------------------------------
        // LOG SUCCESS
        // -------------------------------------------------

        System.out.println();
        System.out.println(
                "===================================="
        );

        System.out.println(
                "REFUND SUCCESSFUL"
        );

        System.out.println(
                "Order ID: "
                        + orderId
        );

        System.out.println(
                "Payment ID: "
                        + payment.getPaymentId()
        );

        System.out.println(
                "Refund ID: "
                        + refundId
        );

        System.out.println(
                "Refund Amount: ₹"
                        + actualRefundAmount
        );

        System.out.println(
                "===================================="
        );


        return savedPayment;
    }


    // =====================================================
    // CRON JOB
    // Check CREATED payments every 1 minute
    // =====================================================

    @Scheduled(fixedRate = 60000)
    public void checkStalePayments() {

        System.out.println();
        System.out.println(
                "===================================="
        );

        System.out.println(
                "CRON JOB: CHECKING PAYMENT STATUS"
        );

        System.out.println(
                "===================================="
        );


        try {

            List<Payment> createdPayments =
                    paymentRepository
                            .findByStatus("CREATED");


            System.out.println(
                    "CREATED PAYMENTS FOUND: "
                            + createdPayments.size()
            );


            for (Payment payment :
                    createdPayments) {

                try {

                    // -------------------------------------------------
                    // CHECK EXPIRY
                    // -------------------------------------------------

                    if (payment.getCreatedAt() == null) {

                        System.out.println(
                                "CREATED AT IS NULL FOR PAYMENT: "
                                        + payment.getOrderId()
                        );

                        continue;
                    }


                    LocalDateTime expiryTime =
                            payment.getCreatedAt()
                                    .plusMinutes(10);


                    if (LocalDateTime.now()
                            .isAfter(expiryTime)) {

                        payment.setStatus(
                                "EXPIRED"
                        );

                        paymentRepository.save(
                                payment
                        );


                        System.out.println(
                                "PAYMENT EXPIRED: "
                                        + payment.getOrderId()
                        );


                        continue;
                    }


                    // -------------------------------------------------
                    // CREATE RAZORPAY AUTH HEADERS
                    // -------------------------------------------------

                    HttpHeaders headers =
                            new HttpHeaders();

                    headers.setBasicAuth(
                            keyId.trim(),
                            keySecret.trim(),
                            StandardCharsets.UTF_8
                    );


                    // -------------------------------------------------
                    // RAZORPAY ORDER URL
                    // -------------------------------------------------

                    String orderUrl =
                            "https://api.razorpay.com/v1/orders/"
                                    + payment.getOrderId();


                    HttpEntity<String> entity =
                            new HttpEntity<>(
                                    headers
                            );


                    // -------------------------------------------------
                    // GET ORDER FROM RAZORPAY
                    // -------------------------------------------------

                    ResponseEntity<String> response =
                            restTemplate.exchange(
                                    orderUrl,
                                    HttpMethod.GET,
                                    entity,
                                    String.class
                            );


                    // -------------------------------------------------
                    // READ RAZORPAY RESPONSE
                    // -------------------------------------------------

                    JSONObject razorpayOrder =
                            new JSONObject(
                                    response.getBody()
                            );


                    String razorpayStatus =
                            razorpayOrder.getString(
                                    "status"
                            );


                    System.out.println(
                            "Order: "
                                    + payment.getOrderId()
                                    + " | Razorpay Status: "
                                    + razorpayStatus
                    );


                    // -------------------------------------------------
                    // UPDATE STATUS
                    // -------------------------------------------------

                    if ("paid".equalsIgnoreCase(
                            razorpayStatus)) {

                        payment.setStatus(
                                "PAID"
                        );

                        paymentRepository.save(
                                payment
                        );


                        System.out.println(
                                "PAYMENT UPDATED TO PAID: "
                                        + payment.getOrderId()
                        );


                    } else if (
                            "attempted".equalsIgnoreCase(
                                    razorpayStatus)) {

                        System.out.println(
                                "PAYMENT ATTEMPTED: "
                                        + payment.getOrderId()
                        );


                    } else {

                        System.out.println(
                                "PAYMENT STILL PENDING: "
                                        + payment.getOrderId()
                        );
                    }


                } catch (Exception e) {

                    System.out.println(
                            "COULD NOT CHECK ORDER: "
                                    + payment.getOrderId()
                    );

                    System.out.println(
                            e.getMessage()
                    );
                }
            }


        } catch (Exception e) {

            System.out.println(
                    "CRON JOB FAILED"
            );

            e.printStackTrace();
        }
    }
}