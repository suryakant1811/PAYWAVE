package com.paywave.payment.client;

import com.paywave.transaction.service.TransactionService;
import com.paywave.transaction.web.TransactionRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class TransactionClient {

    private final TransactionService transactions;

    public TransactionClient(TransactionService transactions) {
        this.transactions = transactions;
    }

    public void record(List<Entry> entries) {
        transactions.recordAll(entries.stream()
                .map(entry -> new TransactionRequest(entry.paymentId(), entry.ownerId(), entry.counterpartyId(),
                        entry.counterpartyName(), entry.direction(), entry.type(), entry.amount(), entry.status(),
                        entry.description(), entry.referenceType()))
                .toList());
    }

    public record Entry(UUID paymentId, UUID ownerId, UUID counterpartyId, String counterpartyName, String direction,
                        String type, BigDecimal amount, String status, String description, String referenceType) {
    }
}
