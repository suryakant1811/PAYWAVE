package com.paywave.transaction.web;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequest(
        UUID paymentId,
        UUID ownerId,
        UUID counterpartyId,
        String counterpartyName,
        String direction,
        String type,
        BigDecimal amount,
        String status,
        String description,
        String referenceType
) {}
