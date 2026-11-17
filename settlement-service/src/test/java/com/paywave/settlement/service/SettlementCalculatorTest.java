package com.paywave.settlement.service;

import com.paywave.settlement.client.SettlementClients;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementCalculatorTest {
    @Test
    void netsFeesAndPartialRefunds() {
        var payment = new SettlementClients.PaymentSnapshot(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "MERCHANT", "MERCHANT",
                new BigDecimal("100.00"), new BigDecimal("1.50"), new BigDecimal("98.50"), new BigDecimal("50.00"),
                "INR", "PARTIALLY_REFUNDED", null, null, false, Instant.now());
        var totals = SettlementCalculator.calculate(List.of(payment));
        assertThat(totals.gross()).isEqualByComparingTo("100.00");
        assertThat(totals.fees()).isEqualByComparingTo("1.50");
        assertThat(totals.refunds()).isEqualByComparingTo("50.00");
        assertThat(totals.net()).isEqualByComparingTo("49.25");
    }
}
