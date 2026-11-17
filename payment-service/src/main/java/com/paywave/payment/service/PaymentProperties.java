package com.paywave.payment.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "paywave.payments")
public class PaymentProperties {
    private BigDecimal minAmount = new BigDecimal("1.00");
    private BigDecimal maxAmount = new BigDecimal("100000.00");
    private BigDecimal merchantFeeRate = new BigDecimal("0.015");
    private int declineCents = 13;
    private int timeoutCents = 14;

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
    public BigDecimal getMerchantFeeRate() { return merchantFeeRate; }
    public void setMerchantFeeRate(BigDecimal merchantFeeRate) { this.merchantFeeRate = merchantFeeRate; }
    public int getDeclineCents() { return declineCents; }
    public void setDeclineCents(int declineCents) { this.declineCents = declineCents; }
    public int getTimeoutCents() { return timeoutCents; }
    public void setTimeoutCents(int timeoutCents) { this.timeoutCents = timeoutCents; }
}
