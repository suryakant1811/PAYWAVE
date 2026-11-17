package com.paywave.ledger.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PostJournalRequest(
        String idempotencyKey,
        String referenceType,
        UUID referenceId,
        String description,
        List<Line> lines
) {
    public record Line(UUID ownerId, String accountType, String direction, BigDecimal amount, String currency) {
    }
}
