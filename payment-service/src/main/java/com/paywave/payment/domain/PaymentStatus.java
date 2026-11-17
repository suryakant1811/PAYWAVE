package com.paywave.payment.domain;

public enum PaymentStatus {
    CREATED,
    PENDING,
    SUCCESS,
    FAILED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
