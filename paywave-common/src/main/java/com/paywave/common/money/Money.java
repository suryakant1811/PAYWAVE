package com.paywave.common.money;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final int SCALE = 2;
    public static final String CURRENCY = "INR";
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_EVEN);

    private Money() {
    }

    public static BigDecimal zero() {
        return ZERO;
    }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "Amount is required");
        }
        BigDecimal scaled = amount.setScale(SCALE, RoundingMode.HALF_EVEN);
        if (scaled.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "Amount must be greater than zero");
        }
        return scaled;
    }

    public static BigDecimal scale(BigDecimal amount) {
        if (amount == null) {
            return zero();
        }
        return amount.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal fee(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal proportional(BigDecimal part, BigDecimal whole, BigDecimal component) {
        if (whole == null || whole.compareTo(BigDecimal.ZERO) == 0) {
            return zero();
        }
        return component.multiply(part).divide(whole, SCALE, RoundingMode.HALF_EVEN);
    }

    public static void requireInr(String currency) {
        if (currency != null && !currency.isBlank() && !CURRENCY.equalsIgnoreCase(currency)) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "Only INR is supported");
        }
    }
}
