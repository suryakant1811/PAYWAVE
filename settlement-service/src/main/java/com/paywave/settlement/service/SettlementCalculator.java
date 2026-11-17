package com.paywave.settlement.service;

import com.paywave.common.money.Money;
import com.paywave.settlement.client.SettlementClients;

import java.math.BigDecimal;
import java.util.List;

public final class SettlementCalculator {
    private SettlementCalculator() {}

    public record Totals(BigDecimal gross, BigDecimal fees, BigDecimal refunds, BigDecimal net) {}

    public static Totals calculate(List<SettlementClients.PaymentSnapshot> payments) {
        BigDecimal gross = Money.zero();
        BigDecimal fees = Money.zero();
        BigDecimal refunds = Money.zero();
        BigDecimal net = Money.zero();
        for (SettlementClients.PaymentSnapshot payment : payments) {
            gross = gross.add(payment.amount());
            fees = fees.add(payment.feeAmount());
            refunds = refunds.add(payment.refundedAmount());
            BigDecimal refundedNet = Money.proportional(payment.refundedAmount(), payment.amount(), payment.netAmount());
            net = net.add(payment.netAmount().subtract(refundedNet));
        }
        return new Totals(Money.scale(gross), Money.scale(fees), Money.scale(refunds), Money.scale(net));
    }
}
