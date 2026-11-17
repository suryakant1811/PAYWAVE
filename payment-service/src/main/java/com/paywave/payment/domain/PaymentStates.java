package com.paywave.payment.domain;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;

public final class PaymentStates {
    private PaymentStates() {
    }

    public static void assertCanMove(PaymentStatus from, PaymentStatus to) {
        boolean ok = switch (from) {
            case CREATED -> to == PaymentStatus.PENDING || to == PaymentStatus.FAILED;
            case PENDING -> to == PaymentStatus.SUCCESS || to == PaymentStatus.FAILED;
            case SUCCESS -> to == PaymentStatus.PARTIALLY_REFUNDED || to == PaymentStatus.REFUNDED;
            case PARTIALLY_REFUNDED -> to == PaymentStatus.PARTIALLY_REFUNDED || to == PaymentStatus.REFUNDED;
            case FAILED, REFUNDED -> false;
        };
        if (!ok) {
            throw new PaywaveException(ErrorCode.INVALID_STATE, "Cannot move payment from " + from + " to " + to);
        }
    }
}
