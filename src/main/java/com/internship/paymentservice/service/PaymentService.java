package com.internship.paymentservice.service;

import com.internship.paymentservice.dto.CreatePaymentRequest;
import com.internship.paymentservice.dto.RefundPaymentRequest;
import com.internship.paymentservice.dto.VerifyPaymentRequest;
import com.internship.paymentservice.entity.Payment;
import com.internship.paymentservice.repository.PaymentRepository;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final RazorpayClient razorpayClient;

    private final RestTemplate restTemplate;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;


    // =====================================================
    // PAYMENT EXPIRY
    // =====================================================

    private static final long PAYMENT_EXPIRY_MINUTES = 10;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PaymentService(
            PaymentRepository paymentRepository,
            RestTemplate restTemplate,
            @Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret)
            throws Exception {

        this.paymentRepository =
                paymentRepository;

        this.restTemplate =
                restTemplate;

        this.razorpayClient =
                new RazorpayClient(
                        keyId.trim(),
                        keySecret.trim()
                );
    }


    // =====================================================
    // CREATE PAYMENT
    // =====================================================

    public Payment createPayment(
            CreatePaymentRequest request) {

        // -------------------------------------------------
        // VALIDATE AMOUNT
        // -------------------------------------------------

        if (request.getAmount() == null ||
                request.getAmount() <= 0) {

            throw new IllegalArgumentException(
                    "Amount must be greater than 0"
            );
        }


        // -------------------------------------------------
        // VALIDATE ORDER ID
        // -------------------------------------------------

        if (request.getOrderId() == null ||
                request.getOrderId().isBlank()) {

            throw new IllegalArgumentException(
                    "Order ID is required"
            );
        }


        // -------------------------------------------------
        // VALIDATE USER
        // -------------------------------------------------

        if (request.getUserId() == null ||
                request.getUserId().isBlank()) {

            throw new IllegalArgumentException(
                    "User ID is required"
            );
        }


        // =================================================
        // IDEMPOTENCY
        // =================================================

        String idempotencyKey =
                request.getIdempotencyKey();


        if (idempotencyKey != null &&
                !idempotencyKey.isBlank()) {

            Optional<Payment> existingPayment =
                    paymentRepository
                            .findByIdempotencyKey(
                                    idempotencyKey
                            );


            if (existingPayment.isPresent()) {

                System.out.println(
                        "IDEMPOTENT REQUEST"
                );

                return existingPayment.get();
            }

        } else {

            idempotencyKey =
                    UUID.randomUUID().toString();
        }


        // =================================================
        // CREATE PAYMENT OBJECT
        // =================================================

        Payment payment =
                new Payment();


        payment.setPaymentId(
                "PAY-" + UUID.randomUUID()
        );


        payment.setOrderId(
                request.getOrderId()
        );


        payment.setUserId(
                request.getUserId()
        );


        payment.setAmount(
                request.getAmount()
        );


        payment.setIdempotencyKey(
                idempotencyKey
        );


        payment.setStatus(
                "CREATED"
        );


        payment.setCreatedAt(
                LocalDateTime.now()
        );


        // =================================================
        // CREATE RAZORPAY ORDER
        // =================================================

        try {

            int amountInPaise =
                    (int) Math.round(
                            request.getAmount() * 100
                    );


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
                    payment.getPaymentId()
            );


            System.out.println();
            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "CREATING RAZORPAY ORDER"
            );

            System.out.println(
                    "Internal Order ID: "
                            + payment.getOrderId()
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


            Order razorpayOrder =
                    razorpayClient.orders.create(
                            orderRequest
                    );


            String razorpayOrderId =
                    razorpayOrder.get("id");


            payment.setRazorpayOrderId(
                    razorpayOrderId
            );


            System.out.println(
                    "RAZORPAY ORDER CREATED: "
                            + razorpayOrderId
            );


        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to create Razorpay order: "
                            + e.getMessage(),
                    e
            );
        }


        // =================================================
        // SAVE PAYMENT
        // =================================================

        Payment savedPayment =
                paymentRepository.save(
                        payment
                );


        System.out.println();
        System.out.println(
                "PAYMENT CREATED"
        );

        System.out.println(
                "Internal Order ID: "
                        + savedPayment.getOrderId()
        );

        System.out.println(
                "Payment ID: "
                        + savedPayment.getPaymentId()
        );

        System.out.println(
                "Razorpay Order ID: "
                        + savedPayment.getRazorpayOrderId()
        );

        System.out.println(
                "Status: "
                        + savedPayment.getStatus()
        );


        return savedPayment;
    }


    // =====================================================
    // VERIFY PAYMENT
    // =====================================================

    public Payment verifyPayment(
            VerifyPaymentRequest request) {

        if (request.getRazorpayOrderId() == null ||
                request.getRazorpayOrderId().isBlank()) {

            throw new IllegalArgumentException(
                    "Razorpay Order ID is required"
            );
        }


        if (request.getRazorpayPaymentId() == null ||
                request.getRazorpayPaymentId().isBlank()) {

            throw new IllegalArgumentException(
                    "Razorpay Payment ID is required"
            );
        }


        if (request.getRazorpaySignature() == null ||
                request.getRazorpaySignature().isBlank()) {

            throw new IllegalArgumentException(
                    "Razorpay Signature is required"
            );
        }


        // =================================================
        // FIND PAYMENT
        // =================================================

        Payment payment =
                paymentRepository
                        .findByRazorpayOrderId(
                                request.getRazorpayOrderId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Payment not found for Razorpay order: "
                                                + request.getRazorpayOrderId()
                                )
                        );


        // =================================================
        // VERIFY SIGNATURE
        // =================================================

        try {

            JSONObject attributes =
                    new JSONObject();


            attributes.put(
                    "razorpay_order_id",
                    request.getRazorpayOrderId()
            );


            attributes.put(
                    "razorpay_payment_id",
                    request.getRazorpayPaymentId()
            );


            attributes.put(
                    "razorpay_signature",
                    request.getRazorpaySignature()
            );


            boolean signatureValid =
                    Utils.verifyPaymentSignature(
                            attributes,
                            razorpaySecret
                    );


            if (!signatureValid) {

                throw new RuntimeException(
                        "Invalid Razorpay payment signature"
                );
            }


        } catch (Exception e) {

            throw new RuntimeException(
                    "Payment verification failed: "
                            + e.getMessage(),
                    e
            );
        }


        // =================================================
        // UPDATE PAYMENT
        // =================================================

        payment.setRazorpayPaymentId(
                request.getRazorpayPaymentId()
        );


        payment.setPaymentId(
                request.getRazorpayPaymentId()
        );


        payment.setStatus(
                "VERIFIED"
        );


        payment.setVerifiedAt(
                LocalDateTime.now()
        );


        Payment savedPayment =
                paymentRepository.save(
                        payment
                );


        // =================================================
        // UPDATE ORDER
        // =================================================

        try {

            updateOrderAfterPayment(
                    savedPayment
            );

        } catch (Exception e) {

            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "WARNING"
            );

            System.out.println(
                    "PAYMENT VERIFIED"
            );

            System.out.println(
                    "BUT ORDER UPDATE FAILED"
            );

            System.out.println(
                    "Order ID: "
                            + savedPayment.getOrderId()
            );

            System.out.println(
                    "Reason: "
                            + e.getMessage()
            );

            System.out.println(
                    "===================================="
            );


            throw new RuntimeException(
                    "Payment verified, but order could not be updated to PAID: "
                            + e.getMessage(),
                    e
            );
        }


        return savedPayment;
    }


    // =====================================================
    // UPDATE ORDER AFTER PAYMENT
    // =====================================================

    private void updateOrderAfterPayment(
            Payment payment) {

        String orderUrl =
                UriComponentsBuilder
                        .fromUriString(
                                "http://localhost:8083/api/orders/"
                                        + payment.getOrderId()
                                        + "/payment"
                        )
                        .queryParam(
                                "paymentId",
                                payment.getPaymentId()
                        )
                        .toUriString();


        System.out.println();
        System.out.println(
                "===================================="
        );

        System.out.println(
                "UPDATING ORDER AFTER PAYMENT"
        );

        System.out.println(
                "Order ID: "
                        + payment.getOrderId()
        );

        System.out.println(
                "Payment ID: "
                        + payment.getPaymentId()
        );

        System.out.println(
                "URL: "
                        + orderUrl
        );

        System.out.println(
                "===================================="
        );


        restTemplate.put(
                orderUrl,
                null
        );


        System.out.println(
                "ORDER UPDATED TO PAID: "
                        + payment.getOrderId()
        );
    }


    // =====================================================
    // UPDATE ORDER AS PAYMENT FAILED
    // =====================================================

    private void updateOrderAsPaymentFailed(
            Payment payment) {

        String orderUrl =
                UriComponentsBuilder
                        .fromUriString(
                                "http://localhost:8083/api/orders/"
                                        + payment.getOrderId()
                                        + "/payment-failed"
                        )
                        .toUriString();


        System.out.println();
        System.out.println(
                "===================================="
        );

        System.out.println(
                "UPDATING ORDER AS PAYMENT FAILED"
        );

        System.out.println(
                "Order ID: "
                        + payment.getOrderId()
        );

        System.out.println(
                "URL: "
                        + orderUrl
        );

        System.out.println(
                "===================================="
        );


        restTemplate.put(
                orderUrl,
                null
        );


        System.out.println(
                "ORDER UPDATED TO PAYMENT_FAILED: "
                        + payment.getOrderId()
        );
    }


    // =====================================================
    // PHASE 4
    // RECHECK PAYMENT STATUS
    // =====================================================

    public Payment recheckPaymentStatus(
            String orderId) {

        // -------------------------------------------------
        // FIND PAYMENT
        // -------------------------------------------------

        Payment payment =
                paymentRepository
                        .findByOrderId(orderId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Payment not found for order: "
                                                + orderId
                                )
                        );


        // -------------------------------------------------
        // CHECK RAZORPAY ORDER ID
        // -------------------------------------------------

        if (payment.getRazorpayOrderId() == null ||
                payment.getRazorpayOrderId().isBlank()) {

            throw new RuntimeException(
                    "Razorpay Order ID not found"
            );
        }


        try {

            // =================================================
            // ASK RAZORPAY FOR PAYMENTS
            // =================================================

            List<com.razorpay.Payment> razorpayPayments =
                    razorpayClient.orders.fetchPayments(
                            payment.getRazorpayOrderId()
                    );


            // =================================================
            // NO PAYMENT FOUND
            // =================================================

            if (razorpayPayments == null ||
                    razorpayPayments.isEmpty()) {

                System.out.println();
                System.out.println(
                        "===================================="
                );

                System.out.println(
                        "NO RAZORPAY PAYMENT FOUND"
                );

                System.out.println(
                        "Order ID: "
                                + orderId
                );


                // -------------------------------------------------
                // CHECK WHETHER PAYMENT EXPIRED
                // -------------------------------------------------

                if (payment.getCreatedAt() != null) {

                    long ageMinutes =
                            Duration.between(
                                    payment.getCreatedAt(),
                                    LocalDateTime.now()
                            ).toMinutes();


                    System.out.println(
                            "Payment age: "
                                    + ageMinutes
                                    + " minutes"
                    );


                    if (ageMinutes >=
                            PAYMENT_EXPIRY_MINUTES) {

                        // -----------------------------------------
                        // MARK PAYMENT EXPIRED
                        // -----------------------------------------

                        payment.setStatus(
                                "EXPIRED"
                        );


                        Payment savedPayment =
                                paymentRepository.save(
                                        payment
                                );


                        System.out.println(
                                "PAYMENT EXPIRED"
                        );


                        // -----------------------------------------
                        // MARK ORDER FAILED
                        // -----------------------------------------

                        try {

                            updateOrderAsPaymentFailed(
                                    savedPayment
                            );

                        } catch (Exception e) {

                            System.out.println(
                                    "ORDER UPDATE FAILED: "
                                            + e.getMessage()
                            );
                        }


                        return savedPayment;
                    }
                }


                System.out.println(
                        "PAYMENT STILL WAITING"
                );

                System.out.println(
                        "===================================="
                );


                return payment;
            }


            // =================================================
            // GET PAYMENT
            // =================================================

            com.razorpay.Payment razorpayPayment =
                    razorpayPayments.get(0);


            String razorpayStatus =
                    razorpayPayment.get(
                            "status"
                    );


            String razorpayPaymentId =
                    razorpayPayment.get(
                            "id"
                    );


            // =================================================
            // LOG
            // =================================================

            System.out.println();
            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "RAZORPAY PAYMENT RECHECK"
            );

            System.out.println(
                    "Internal Order ID: "
                            + orderId
            );

            System.out.println(
                    "Razorpay Order ID: "
                            + payment.getRazorpayOrderId()
            );

            System.out.println(
                    "Razorpay Payment ID: "
                            + razorpayPaymentId
            );

            System.out.println(
                    "Razorpay Status: "
                            + razorpayStatus
            );

            System.out.println(
                    "===================================="
            );


            // =================================================
            // PAYMENT CAPTURED
            // =================================================

            if ("captured".equalsIgnoreCase(
                    razorpayStatus
            )) {

                payment.setRazorpayPaymentId(
                        razorpayPaymentId
                );


                payment.setPaymentId(
                        razorpayPaymentId
                );


                payment.setStatus(
                        "VERIFIED"
                );


                if (payment.getVerifiedAt() == null) {

                    payment.setVerifiedAt(
                            LocalDateTime.now()
                    );
                }


                Payment savedPayment =
                        paymentRepository.save(
                                payment
                        );


                // -------------------------------------------------
                // UPDATE ORDER
                // -------------------------------------------------

                try {

                    updateOrderAfterPayment(
                            savedPayment
                    );

                } catch (Exception e) {

                    System.out.println(
                            "ORDER UPDATE FAILED: "
                                    + e.getMessage()
                    );
                }


                System.out.println();
                System.out.println(
                        "PAYMENT RECHECK SUCCESSFUL"
                );

                System.out.println(
                        "Payment ID: "
                                + razorpayPaymentId
                );

                System.out.println(
                        "Payment Status: VERIFIED"
                );


                return savedPayment;
            }


            // =================================================
            // PAYMENT FAILED
            // =================================================

            if ("failed".equalsIgnoreCase(
                    razorpayStatus
            )) {

                payment.setRazorpayPaymentId(
                        razorpayPaymentId
                );


                payment.setStatus(
                        "FAILED"
                );


                Payment savedPayment =
                        paymentRepository.save(
                                payment
                        );


                // -------------------------------------------------
                // IMPORTANT FIX
                // -------------------------------------------------

                try {

                    updateOrderAsPaymentFailed(
                            savedPayment
                    );

                } catch (Exception e) {

                    System.out.println(
                            "ORDER UPDATE FAILED: "
                                    + e.getMessage()
                    );
                }


                System.out.println();
                System.out.println(
                        "===================================="
                );

                System.out.println(
                        "PAYMENT RECHECK: FAILED"
                );

                System.out.println(
                        "ORDER MARKED AS PAYMENT_FAILED"
                );

                System.out.println(
                        "===================================="
                );


                return savedPayment;
            }


            // =================================================
            // PAYMENT STILL PROCESSING
            // =================================================

            System.out.println();
            System.out.println(
                    "PAYMENT STILL PROCESSING"
            );


            return payment;


        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to recheck Razorpay payment status: "
                            + e.getMessage(),
                    e
            );
        }
    }


    // =====================================================
    // REFUND PAYMENT
    // =====================================================

    public Payment refundPayment(
            Long id,
            RefundPaymentRequest request) {

        Payment payment =
                paymentRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Payment not found"
                                )
                        );


        if (!"VERIFIED".equalsIgnoreCase(
                payment.getStatus()
        ) &&
                !"PAID".equalsIgnoreCase(
                        payment.getStatus()
                )) {

            throw new RuntimeException(
                    "Only successful payments can be refunded"
            );
        }


        if (request.getAmount() == null ||
                request.getAmount() <= 0) {

            throw new IllegalArgumentException(
                    "Refund amount must be greater than 0"
            );
        }


        if (request.getAmount() >
                payment.getAmount()) {

            throw new IllegalArgumentException(
                    "Refund amount cannot exceed payment amount"
            );
        }


        payment.setRefundId(
                "REF-" + UUID.randomUUID()
        );


        payment.setRefundAmount(
                request.getAmount()
        );


        payment.setStatus(
                "REFUNDED"
        );


        return paymentRepository.save(
                payment
        );
    }


    // =====================================================
    // UPDATE PAYMENT STATUS
    // WEBHOOK
    // =====================================================

    public Payment updateStatus(
            String razorpayOrderId,
            String status,
            String razorpayPaymentId) {

        Payment payment =
                paymentRepository
                        .findByRazorpayOrderId(
                                razorpayOrderId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Payment not found for Razorpay order: "
                                                + razorpayOrderId
                                )
                        );


        payment.setStatus(
                status
        );


        if (razorpayPaymentId != null &&
                !razorpayPaymentId.isBlank()) {

            payment.setRazorpayPaymentId(
                    razorpayPaymentId
            );

            payment.setPaymentId(
                    razorpayPaymentId
            );
        }


        if (("VERIFIED".equalsIgnoreCase(status) ||
                "PAID".equalsIgnoreCase(status)) &&
                payment.getVerifiedAt() == null) {

            payment.setVerifiedAt(
                    LocalDateTime.now()
            );
        }


        Payment savedPayment =
                paymentRepository.save(
                        payment
                );


        // =================================================
        // SUCCESSFUL PAYMENT
        // =================================================

        if ("VERIFIED".equalsIgnoreCase(status) ||
                "PAID".equalsIgnoreCase(status)) {

            try {

                updateOrderAfterPayment(
                        savedPayment
                );

            } catch (Exception e) {

                System.out.println(
                        "Order update failed: "
                                + e.getMessage()
                );
            }
        }


        // =================================================
        // FAILED PAYMENT
        // =================================================

        if ("FAILED".equalsIgnoreCase(status) ||
                "EXPIRED".equalsIgnoreCase(status)) {

            try {

                updateOrderAsPaymentFailed(
                        savedPayment
                );

            } catch (Exception e) {

                System.out.println(
                        "Order update failed: "
                                + e.getMessage()
                );
            }
        }


        return savedPayment;
    }


    // =====================================================
    // GET ALL PAYMENTS
    // =====================================================

    public List<Payment> getAllPayments() {

        return paymentRepository.findAll();
    }


    // =====================================================
    // GET PAYMENT BY DATABASE ID
    // =====================================================

    public Optional<Payment> getPaymentById(
            Long id) {

        return paymentRepository.findById(
                id
        );
    }


    // =====================================================
    // GET PAYMENT BY ORDER ID
    // =====================================================

    public Optional<Payment> getPaymentByOrderId(
            String orderId) {

        return paymentRepository.findByOrderId(
                orderId
        );
    }


    // =====================================================
    // GET PAYMENTS BY USER
    // =====================================================

    public List<Payment> getPaymentsByUser(
            String userId) {

        return paymentRepository.findByUserId(
                userId
        );
    }


    // =====================================================
    // GET PAYMENTS BY STATUS
    // =====================================================

    public List<Payment> getPaymentsByStatus(
            String status) {

        return paymentRepository.findByStatus(
                status
        );
    }
}