package com.paywave.ledger.web;

import com.paywave.ledger.domain.JournalEntry;
import com.paywave.ledger.domain.JournalLine;
import com.paywave.ledger.repo.LedgerAccountRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JournalResponse(
        UUID journalId,
        String idempotencyKey,
        String referenceType,
        UUID referenceId,
        UUID reversalOf,
        String description,
        Instant postedAt,
        List<LineView> lines
) {
    public record LineView(UUID accountId, UUID ownerId, String accountType, String direction, BigDecimal amount) {
    }

    public static JournalResponse from(JournalEntry entry, List<JournalLine> lines, LedgerAccountRepository accounts) {
        List<LineView> views = lines.stream().map(line -> {
            var account = accounts.findById(line.getAccountId()).orElseThrow();
            return new LineView(account.getId(), account.getOwnerId(), account.getAccountType().name(), line.getDirection().name(), line.getAmount());
        }).toList();
        return new JournalResponse(entry.getId(), entry.getIdempotencyKey(), entry.getReferenceType(), entry.getReferenceId(),
                entry.getReversalOf(), entry.getDescription(), entry.getPostedAt(), views);
    }
}
