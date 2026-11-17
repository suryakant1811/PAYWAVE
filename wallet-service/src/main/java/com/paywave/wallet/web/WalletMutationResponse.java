package com.paywave.wallet.web;

import com.paywave.wallet.domain.WalletEntry;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletMutationResponse(
        UUID walletId,
        UUID entryId,
        String idempotencyKey,
        String type,
        BigDecimal amount,
        BigDecimal balanceAfter
) {
    public static WalletMutationResponse from(WalletEntry entry) {
        return new WalletMutationResponse(entry.getWalletId(), entry.getId(), entry.getIdempotencyKey(),
                entry.getType().name(), entry.getAmount(), entry.getBalanceAfter());
    }
}
