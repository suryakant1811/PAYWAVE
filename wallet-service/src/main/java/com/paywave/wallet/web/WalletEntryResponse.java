package com.paywave.wallet.web;

import com.paywave.wallet.domain.WalletEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletEntryResponse(
        UUID entryId,
        String type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String referenceType,
        UUID referenceId,
        Instant createdAt
) {
    public static WalletEntryResponse from(WalletEntry entry) {
        return new WalletEntryResponse(entry.getId(), entry.getType().name(), entry.getAmount(), entry.getBalanceAfter(),
                entry.getReferenceType(), entry.getReferenceId(), entry.getCreatedAt());
    }
}
