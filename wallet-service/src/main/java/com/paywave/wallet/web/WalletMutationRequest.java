package com.paywave.wallet.web;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletMutationRequest(
        UUID ownerId,
        String ownerType,
        BigDecimal amount,
        String idempotencyKey,
        String referenceType,
        UUID referenceId,
        String currency,
        UUID notifyUserId
) {
}
