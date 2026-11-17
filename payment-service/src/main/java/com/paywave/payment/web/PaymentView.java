package com.paywave.payment.web;

import com.paywave.payment.domain.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentView(
        UUID paymentId,
        UUID payerId,
        UUID payeeId,
        String payeeType,
        String type,
        BigDecimal amount,
        BigDecimal feeAmount,
        BigDecimal netAmount,
        BigDecimal refundedAmount,
        String currency,
        String status,
        String failureReason,
        String gatewayReference,
        boolean settled,
        Instant createdAt
) {
    public static PaymentView from(Payment payment) {
        return new PaymentView(payment.getId(), payment.getPayerId(), payment.getPayeeId(), payment.getPayeeType().name(),
                payment.getType().name(), payment.getAmount(), payment.getFeeAmount(), payment.getNetAmount(),
                payment.getRefundedAmount(), payment.getCurrency(), payment.getStatus().name(), payment.getFailureReason(),
                payment.getGatewayReference(), payment.isSettled(), payment.getCreatedAt());
    }
}
