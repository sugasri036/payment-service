package com.internship.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RefundPaymentRequest {

    @NotNull
    @Positive
    private Double amount;

    public RefundPaymentRequest() {
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}