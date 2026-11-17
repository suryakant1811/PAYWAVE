package com.paywave.wallet.service;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.messaging.EventTypes;
import com.paywave.common.money.Money;
import com.paywave.wallet.client.LedgerClient;
import com.paywave.wallet.config.WalletProperties;
import com.paywave.wallet.domain.EntryType;
import com.paywave.wallet.domain.OwnerType;
import com.paywave.wallet.domain.Wallet;
import com.paywave.wallet.domain.WalletEntry;
import com.paywave.wallet.domain.WalletStatus;
import com.paywave.wallet.repo.WalletEntryRepository;
import com.paywave.wallet.repo.WalletRepository;
import com.paywave.wallet.web.WalletMutationRequest;
import com.paywave.wallet.web.WalletMutationResponse;
import com.paywave.wallet.web.WalletView;
import com.paywave.web.outbox.OutboxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository wallets;
    private final WalletEntryRepository entries;
    private final WalletProperties properties;
    private final LedgerClient ledgerClient;
    private final OutboxWriter outboxWriter;

    public WalletService(WalletRepository wallets,
                         WalletEntryRepository entries,
                         WalletProperties properties,
                         LedgerClient ledgerClient,
                         OutboxWriter outboxWriter) {
        this.wallets = wallets;
        this.entries = entries;
        this.properties = properties;
        this.ledgerClient = ledgerClient;
        this.outboxWriter = outboxWriter;
    }

    @Transactional
    public WalletView create(UUID ownerId, String ownerType, String currency, UUID notifyUserId) {
        Money.requireInr(currency);
        return wallets.findByOwnerId(ownerId)
                .map(WalletView::from)
                .orElseGet(() -> {
                    OwnerType type = OwnerType.valueOf(ownerType);
                    Wallet wallet = wallets.save(Wallet.open(ownerId, type, Money.CURRENCY,
                            properties.getDailyLimit(), properties.getMonthlyLimit(), properties.getPerTxnLimit()));
                    ledgerClient.openAccount(ownerId, ownerType, Money.CURRENCY);
                    log.info("Opened {} wallet {} for {}", type, wallet.getId(), ownerId);
                    return WalletView.from(wallet);
                });
    }

    @Transactional(readOnly = true)
    public WalletView getByOwner(UUID ownerId) {
        return WalletView.from(require(ownerId));
    }

    @Transactional
    public WalletMutationResponse credit(WalletMutationRequest request) {
        return mutate(request, EntryType.CREDIT);
    }

    @Transactional
    public WalletMutationResponse debit(WalletMutationRequest request) {
        return mutate(request, EntryType.DEBIT);
    }

    @Transactional(readOnly = true)
    public Page<WalletEntry> entries(UUID ownerId, Pageable pageable) {
        Wallet wallet = require(ownerId);
        return entries.findByWalletId(wallet.getId(), pageable);
    }

    @Transactional
    public void deleteIfEmpty(UUID ownerId) {
        wallets.findByOwnerId(ownerId).ifPresent(wallet -> {
            if (entries.countByWalletId(wallet.getId()) > 0 || wallet.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                throw new PaywaveException(ErrorCode.CONFLICT, "Wallet cannot be removed after it has been used");
            }
            wallets.delete(wallet);
        });
    }

    private WalletMutationResponse mutate(WalletMutationRequest request, EntryType type) {
        BigDecimal amount = Money.normalize(request.amount());
        Money.requireInr(request.currency());
        var existing = entries.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            WalletEntry entry = existing.get();
            if (entry.getType() != type || entry.getAmount().compareTo(amount) != 0) {
                throw new PaywaveException(ErrorCode.DUPLICATE_REQUEST, "Idempotency key was already used with a different payload");
            }
            return WalletMutationResponse.from(entry);
        }
        Wallet wallet = wallets.findByOwnerIdForUpdate(request.ownerId())
                .orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Wallet not found"));
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new PaywaveException(ErrorCode.ACCOUNT_INACTIVE, "Wallet is not active");
        }
        if (amount.compareTo(wallet.getPerTxnLimit()) > 0) {
            throw new PaywaveException(ErrorCode.LIMIT_EXCEEDED, "Amount exceeds the per-transaction limit");
        }
        BigDecimal updated = type == EntryType.CREDIT
                ? wallet.getBalance().add(amount)
                : wallet.getBalance().subtract(amount);
        if (type == EntryType.DEBIT && updated.compareTo(BigDecimal.ZERO) < 0) {
            throw new PaywaveException(ErrorCode.INSUFFICIENT_BALANCE, "Insufficient wallet balance");
        }
        enforcePeriodLimit(wallet, type, amount, request.referenceType());
        wallet.setBalance(Money.scale(updated));
        WalletEntry entry = WalletEntry.of(wallet.getId(), request.idempotencyKey(), type, amount,
                wallet.getBalance(), request.referenceType(), request.referenceId());
        try {
            entries.saveAndFlush(entry);
        } catch (DataIntegrityViolationException ex) {
            return entries.findByIdempotencyKey(request.idempotencyKey())
                    .map(WalletMutationResponse::from)
                    .orElseThrow(() -> ex);
        }
        publish(wallet, entry, request.notifyUserId() == null ? wallet.getOwnerId() : request.notifyUserId());
        log.info("{} wallet {} amount {} balance {}", type, wallet.getId(), amount, wallet.getBalance());
        return WalletMutationResponse.from(entry);
    }

    private void enforcePeriodLimit(Wallet wallet, EntryType type, BigDecimal amount, String referenceType) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (type == EntryType.DEBIT) {
            Instant start = today.atStartOfDay().toInstant(ZoneOffset.UTC);
            BigDecimal spent = entries.sumSince(wallet.getId(), EntryType.DEBIT, start);
            if (spent.add(amount).compareTo(wallet.getDailyLimit()) > 0) {
                throw new PaywaveException(ErrorCode.LIMIT_EXCEEDED, "Daily spend limit exceeded");
            }
        }
        if (type == EntryType.CREDIT && "TOPUP".equals(referenceType)) {
            Instant start = today.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            BigDecimal topped = entries.sumSinceForReference(wallet.getId(), EntryType.CREDIT, "TOPUP", start);
            if (topped.add(amount).compareTo(wallet.getMonthlyLimit()) > 0) {
                throw new PaywaveException(ErrorCode.LIMIT_EXCEEDED, "Monthly add-money limit exceeded");
            }
        }
    }

    private void publish(Wallet wallet, WalletEntry entry, UUID notifyUserId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("walletId", wallet.getId());
        payload.put("ownerId", wallet.getOwnerId());
        payload.put("amount", entry.getAmount());
        payload.put("currency", wallet.getCurrency());
        payload.put("balanceAfter", entry.getBalanceAfter());
        payload.put("referenceType", entry.getReferenceType());
        payload.put("referenceId", entry.getReferenceId());
        payload.put("userIds", List.of(notifyUserId));
        String event = entry.getType() == EntryType.CREDIT ? EventTypes.WALLET_CREDITED : EventTypes.WALLET_DEBITED;
        outboxWriter.enqueue("Wallet", wallet.getId(), event, payload);
    }

    private Wallet require(UUID ownerId) {
        return wallets.findByOwnerId(ownerId)
                .orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Wallet not found"));
    }
}
