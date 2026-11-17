package com.paywave.wallet.web;

import com.paywave.wallet.domain.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletView(
        UUID walletId,
        UUID ownerId,
        String ownerType,
        String currency,
        BigDecimal balance,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        BigDecimal perTxnLimit,
        String status
) {
    public static WalletView from(Wallet wallet) {
        return new WalletView(wallet.getId(), wallet.getOwnerId(), wallet.getOwnerType().name(), wallet.getCurrency(),
                wallet.getBalance(), wallet.getDailyLimit(), wallet.getMonthlyLimit(), wallet.getPerTxnLimit(),
                wallet.getStatus().name());
    }
}
