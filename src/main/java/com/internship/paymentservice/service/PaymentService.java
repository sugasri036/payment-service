package com.internship.paymentservice.service;

import com.internship.paymentservice.dto.CreatePaymentRequest;
import com.internship.paymentservice.entity.Payment;
import com.internship.paymentservice.repository.PaymentRepository;

import com.razorpay.Order;
import com.razorpay.Refund;
import com.razorpay.RazorpayClient;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;


    @Value("${razorpay.key.id}")
    private String keyId;


    @Value("${razorpay.key.secret}")
    private String keySecret;


    public PaymentService(
            PaymentRepository paymentRepository) {

        this.paymentRepository = paymentRepository;
    }


    // =====================================================
    // CREATE PAYMENT
    // =====================================================

    public Payment createPayment(
            CreatePaymentRequest request)
            throws Exception {

        RazorpayClient razorpay =
                new RazorpayClient(
                        keyId,
                        keySecret
                );


        // -------------------------------------------------
        // Convert ₹ amount to paise
        // ₹500 = 50000 paise
        // -------------------------------------------------

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
                "receipt_"
                        + System.currentTimeMillis()
        );


        // -------------------------------------------------
        // Create Razorpay order
        // -------------------------------------------------

        Order razorpayOrder =
                razorpay.orders.create(
                        orderRequest
                );


        // -------------------------------------------------
        // Save payment in database
        // -------------------------------------------------

        Payment payment =
                new Payment();


        payment.setOrderId(
                razorpayOrder.get("id")
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
                paymentRepository
                        .findByOrderId(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found for order: "
                                                + orderId
                                )
                        );


        payment.setStatus(status);


        // -------------------------------------------------
        // Save Razorpay payment ID
        // -------------------------------------------------

        if (paymentId != null) {

            payment.setPaymentId(
                    paymentId
            );
        }


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

        return paymentRepository
                .findByOrderId(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found for order: "
                                        + orderId
                        )
                );
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


        // -------------------------------------------------
        // Find payment in database
        // -------------------------------------------------

        Payment payment =
                paymentRepository
                        .findByOrderId(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found for order: "
                                                + orderId
                                )
                        );


        // -------------------------------------------------
        // Payment must be PAID
        // -------------------------------------------------

        if (!"PAID".equalsIgnoreCase(
                payment.getStatus())) {

            throw new RuntimeException(
                    "Only PAID payments can be refunded"
            );
        }


        // -------------------------------------------------
        // Payment ID must exist
        // -------------------------------------------------

        if (payment.getPaymentId() == null
                || payment.getPaymentId().isBlank()) {

            throw new RuntimeException(
                    "Razorpay payment ID not found"
            );
        }


        // -------------------------------------------------
        // Prevent duplicate refund
        // -------------------------------------------------

        if ("REFUNDED".equalsIgnoreCase(
                payment.getStatus())) {

            throw new RuntimeException(
                    "Payment has already been refunded"
            );
        }


        // -------------------------------------------------
        // Create Razorpay client
        // -------------------------------------------------

        RazorpayClient razorpay =
                new RazorpayClient(
                        keyId,
                        keySecret
                );


        // -------------------------------------------------
        // Create refund request
        // -------------------------------------------------

        JSONObject refundRequest =
                new JSONObject();


        int refundAmountInPaise =
                (int) Math.round(
                        payment.getAmount() * 100
                );


        refundRequest.put(
                "amount",
                refundAmountInPaise
        );


        // -------------------------------------------------
        // Create Razorpay refund
        // IMPORTANT:
        // payments is lowercase
        // -------------------------------------------------

        Refund refund =
                razorpay.payments.refund(
                        payment.getPaymentId(),
                        refundRequest
                );


        // -------------------------------------------------
        // Get Razorpay refund ID
        // -------------------------------------------------

        String refundId =
                refund.get("id");


        // -------------------------------------------------
        // Get refund amount
        // -------------------------------------------------

        int actualRefundAmountInPaise =
                refund.get("amount");


        double actualRefundAmount =
                actualRefundAmountInPaise
                        / 100.0;


        // -------------------------------------------------
        // Save refund information
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
        // Console output
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

            RazorpayClient razorpay =
                    new RazorpayClient(
                            keyId,
                            keySecret
                    );


            // -------------------------------------------------
            // Find CREATED payments
            // -------------------------------------------------

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

                    // =========================================
                    // EXPIRE AFTER 10 MINUTES
                    // =========================================

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


                    // =========================================
                    // CHECK RAZORPAY ORDER
                    // =========================================

                    Order razorpayOrder =
                            razorpay.orders.fetch(
                                    payment.getOrderId()
                            );


                    String razorpayStatus =
                            razorpayOrder.get(
                                    "status"
                            );


                    System.out.println(
                            "Order: "
                                    + payment.getOrderId()
                                    + " | Razorpay Status: "
                                    + razorpayStatus
                    );


                    // =========================================
                    // PAID
                    // =========================================

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
                    }


                    // =========================================
                    // ATTEMPTED
                    // =========================================

                    else if ("attempted"
                            .equalsIgnoreCase(
                                    razorpayStatus)) {


                        System.out.println(
                                "PAYMENT ATTEMPTED: "
                                        + payment.getOrderId()
                        );
                    }


                    // =========================================
                    // STILL CREATED
                    // =========================================

                    else {


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