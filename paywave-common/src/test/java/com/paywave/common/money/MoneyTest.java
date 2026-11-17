package com.paywave.common.money;

import com.paywave.common.error.PaywaveException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void normalizesScaleAndRejectsNonPositiveAmounts() {
        assertThat(Money.normalize(new BigDecimal("10"))).isEqualByComparingTo("10.00");
        assertThat(Money.normalize(new BigDecimal("10.126"))).isEqualByComparingTo("10.13");
        assertThatThrownBy(() -> Money.normalize(new BigDecimal("0"))).isInstanceOf(PaywaveException.class);
        assertThatThrownBy(() -> Money.normalize(null)).isInstanceOf(PaywaveException.class);
    }

    @Test
    void computesFeeAndProportionalComponent() {
        BigDecimal fee = Money.fee(new BigDecimal("100.00"), new BigDecimal("0.015"));
        assertThat(fee).isEqualByComparingTo("1.50");
        assertThat(Money.proportional(new BigDecimal("50.00"), new BigDecimal("100.00"), fee)).isEqualByComparingTo("0.75");
    }

    @Test
    void rejectsNonInrCurrency() {
        assertThatThrownBy(() -> Money.requireInr("USD")).isInstanceOf(PaywaveException.class);
    }
}
