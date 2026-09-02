package com.internship.paymentservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    // =====================================================
    // DATABASE ID
    // =====================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =====================================================
    // PAYMENT INFORMATION
    // =====================================================

    private String paymentId;

    private String orderId;

    private String razorpayOrderId;

    private String razorpayPaymentId;


    // =====================================================
    // USER
    // =====================================================

    private String userId;


    // =====================================================
    // AMOUNT
    // =====================================================

    private Double amount;


    // =====================================================
    // STATUS
    // =====================================================

    private String status;


    // =====================================================
    // IDEMPOTENCY
    // =====================================================

    private String idempotencyKey;


    // =====================================================
    // REFUND
    // =====================================================

    private String refundId;

    private Double refundAmount;


    // =====================================================
    // TIMESTAMPS
    // =====================================================

    private LocalDateTime createdAt;

    private LocalDateTime verifiedAt;


    // =====================================================
    // GET DATABASE ID
    // =====================================================

    public Long getId() {
        return id;
    }


    // =====================================================
    // PAYMENT ID
    // =====================================================

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(
            String paymentId) {

        this.paymentId = paymentId;
    }


    // =====================================================
    // ORDER ID
    // =====================================================

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(
            String orderId) {

        this.orderId = orderId;
    }


    // =====================================================
    // RAZORPAY ORDER ID
    // =====================================================

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(
            String razorpayOrderId) {

        this.razorpayOrderId =
                razorpayOrderId;
    }


    // =====================================================
    // RAZORPAY PAYMENT ID
    // =====================================================

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(
            String razorpayPaymentId) {

        this.razorpayPaymentId =
                razorpayPaymentId;
    }


    // =====================================================
    // USER ID
    // =====================================================

    public String getUserId() {
        return userId;
    }

    public void setUserId(
            String userId) {

        this.userId = userId;
    }


    // =====================================================
    // AMOUNT
    // =====================================================

    public Double getAmount() {
        return amount;
    }

    public void setAmount(
            Double amount) {

        this.amount = amount;
    }


    // =====================================================
    // STATUS
    // =====================================================

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status) {

        this.status = status;
    }


    // =====================================================
    // IDEMPOTENCY KEY
    // =====================================================

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(
            String idempotencyKey) {

        this.idempotencyKey =
                idempotencyKey;
    }


    // =====================================================
    // REFUND ID
    // =====================================================

    public String getRefundId() {
        return refundId;
    }

    public void setRefundId(
            String refundId) {

        this.refundId = refundId;
    }


    // =====================================================
    // REFUND AMOUNT
    // =====================================================

    public Double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(
            Double refundAmount) {

        this.refundAmount =
                refundAmount;
    }


    // =====================================================
    // CREATED AT
    // =====================================================

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt = createdAt;
    }


    // =====================================================
    // VERIFIED AT
    // =====================================================

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(
            LocalDateTime verifiedAt) {

        this.verifiedAt = verifiedAt;
    }
}