package com.paywave.ledger.service;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.money.Money;
import com.paywave.ledger.domain.AccountType;
import com.paywave.ledger.domain.Direction;
import com.paywave.ledger.domain.JournalEntry;
import com.paywave.ledger.domain.JournalLine;
import com.paywave.ledger.domain.LedgerAccount;
import com.paywave.ledger.domain.NormalSide;
import com.paywave.ledger.repo.JournalEntryRepository;
import com.paywave.ledger.repo.JournalLineRepository;
import com.paywave.ledger.repo.LedgerAccountRepository;
import com.paywave.ledger.web.JournalResponse;
import com.paywave.ledger.web.PostJournalRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {

    private final LedgerAccountRepository accounts;
    private final JournalEntryRepository journals;
    private final JournalLineRepository lines;

    public LedgerService(LedgerAccountRepository accounts, JournalEntryRepository journals, JournalLineRepository lines) {
        this.accounts = accounts;
        this.journals = journals;
        this.lines = lines;
    }

    @Transactional
    public LedgerAccount openAccount(UUID ownerId, AccountType type, String currency) {
        Money.requireInr(currency);
        if (type == AccountType.GATEWAY_CLEARING || type == AccountType.FEE_INCOME || type == AccountType.PAYOUT_CLEARING) {
            throw new PaywaveException(ErrorCode.FORBIDDEN, "Platform accounts are created by migration");
        }
        return accounts.findByOwnerIdAndAccountTypeAndCurrency(ownerId, type, Money.CURRENCY)
                .orElseGet(() -> accounts.save(LedgerAccount.open(ownerId, type, Money.CURRENCY)));
    }

    @Transactional
    public JournalResponse post(PostJournalRequest request) {
        var existing = journals.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return JournalResponse.from(existing.get(), lines.findByJournalEntryId(existing.get().getId()), accounts);
        }
        if (request.lines() == null || request.lines().size() < 2) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "A journal needs at least two lines");
        }
        BigDecimal debits = Money.zero();
        BigDecimal credits = Money.zero();
        List<PreparedLine> prepared = new ArrayList<>();
        for (PostJournalRequest.Line line : request.lines()) {
            BigDecimal amount = Money.normalize(line.amount());
            Money.requireInr(line.currency());
            Direction direction = Direction.valueOf(line.direction());
            AccountType type = AccountType.valueOf(line.accountType());
            LedgerAccount account = accounts.findByOwnerIdAndAccountTypeAndCurrency(line.ownerId(), type, Money.CURRENCY)
                    .orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Ledger account " + type + " was not found for " + line.ownerId()));
            prepared.add(new PreparedLine(account, direction, amount));
            if (direction == Direction.DEBIT) {
                debits = debits.add(amount);
            } else {
                credits = credits.add(amount);
            }
        }
        if (debits.compareTo(credits) != 0) {
            throw new PaywaveException(ErrorCode.INVALID_STATE, "Journal does not balance: debits " + debits + " credits " + credits);
        }
        JournalEntry entry = JournalEntry.post(request.idempotencyKey(), request.referenceType(), request.referenceId(), null,
                request.description() == null ? request.referenceType() : request.description());
        try {
            journals.saveAndFlush(entry);
        } catch (DataIntegrityViolationException ex) {
            JournalEntry winner = journals.findByIdempotencyKey(request.idempotencyKey()).orElseThrow(() -> ex);
            return JournalResponse.from(winner, lines.findByJournalEntryId(winner.getId()), accounts);
        }
        List<JournalLine> stored = new ArrayList<>();
        for (PreparedLine line : prepared) {
            stored.add(lines.save(JournalLine.of(entry.getId(), line.account().getId(), line.direction(), line.amount(), Money.CURRENCY)));
        }
        return JournalResponse.from(entry, stored, accounts);
    }

    @Transactional
    public JournalResponse reverse(UUID journalId, String idempotencyKey, String description) {
        var existing = journals.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return JournalResponse.from(existing.get(), lines.findByJournalEntryId(existing.get().getId()), accounts);
        }
        JournalEntry original = journals.findById(journalId)
                .orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Journal not found"));
        List<JournalLine> originalLines = lines.findByJournalEntryId(original.getId());
        JournalEntry reversal = JournalEntry.post(idempotencyKey, original.getReferenceType(), original.getReferenceId(), original.getId(),
                description == null ? "Reversal of " + original.getId() : description);
        journals.saveAndFlush(reversal);
        List<JournalLine> stored = new ArrayList<>();
        for (JournalLine line : originalLines) {
            stored.add(lines.save(JournalLine.of(reversal.getId(), line.getAccountId(), line.getDirection().opposite(), line.getAmount(), line.getCurrency())));
        }
        return JournalResponse.from(reversal, stored, accounts);
    }

    @Transactional(readOnly = true)
    public BigDecimal balance(UUID ownerId, AccountType type) {
        LedgerAccount account = accounts.findByOwnerIdAndAccountTypeAndCurrency(ownerId, type, Money.CURRENCY)
                .orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Ledger account not found"));
        return balanceOf(account);
    }

    @Transactional(readOnly = true)
    public Reconciliation reconcile(UUID ownerId, AccountType type, BigDecimal reportedBalance) {
        BigDecimal ledgerBalance = balance(ownerId, type);
        BigDecimal reported = Money.scale(reportedBalance);
        BigDecimal difference = reported.subtract(ledgerBalance);
        return new Reconciliation(ownerId, type.name(), ledgerBalance, reported, difference, difference.compareTo(BigDecimal.ZERO) == 0);
    }

    @Transactional(readOnly = true)
    public List<JournalResponse> byReference(String referenceType, UUID referenceId) {
        return journals.findByReferenceTypeAndReferenceIdOrderByPostedAtAsc(referenceType, referenceId).stream()
                .map(entry -> JournalResponse.from(entry, lines.findByJournalEntryId(entry.getId()), accounts))
                .toList();
    }

    private BigDecimal balanceOf(LedgerAccount account) {
        List<Object[]> rows = lines.sumSides(account.getId().toString());
        BigDecimal debits = Money.zero();
        BigDecimal credits = Money.zero();
        if (!rows.isEmpty() && rows.get(0) != null) {
            Object[] row = rows.get(0);
            debits = Money.scale(new BigDecimal(row[0].toString()));
            credits = Money.scale(new BigDecimal(row[1].toString()));
        }
        BigDecimal raw = account.getNormalSide() == NormalSide.CREDIT ? credits.subtract(debits) : debits.subtract(credits);
        return Money.scale(raw);
    }

    public record Reconciliation(UUID ownerId, String accountType, BigDecimal ledgerBalance, BigDecimal reportedBalance,
                                 BigDecimal difference, boolean matched) {
    }

    private record PreparedLine(LedgerAccount account, Direction direction, BigDecimal amount) {
    }
}
