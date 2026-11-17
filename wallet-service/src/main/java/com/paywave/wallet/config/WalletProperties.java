package com.paywave.wallet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "paywave.wallet")
public class WalletProperties {
    private BigDecimal dailyLimit = new BigDecimal("100000.00");
    private BigDecimal monthlyLimit = new BigDecimal("500000.00");
    private BigDecimal perTxnLimit = new BigDecimal("50000.00");

    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(BigDecimal monthlyLimit) { this.monthlyLimit = monthlyLimit; }
    public BigDecimal getPerTxnLimit() { return perTxnLimit; }
    public void setPerTxnLimit(BigDecimal perTxnLimit) { this.perTxnLimit = perTxnLimit; }
}
