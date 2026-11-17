package com.paywave.transaction.web;

import com.paywave.transaction.domain.TransactionRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId, UUID paymentId, UUID ownerId, UUID counterpartyId, String counterpartyName,
        String direction, String type, BigDecimal amount, String currency, String status, String description,
        String referenceType, Instant occurredAt
) {
    public static TransactionResponse from(TransactionRecord record) {
        return new TransactionResponse(record.getId(), record.getPaymentId(), record.getOwnerId(), record.getCounterpartyId(),
                record.getCounterpartyName(), record.getDirection(), record.getType(), record.getAmount(), record.getCurrency(),
                record.getStatus(), record.getDescription(), record.getReferenceType(), record.getOccurredAt());
    }
}
