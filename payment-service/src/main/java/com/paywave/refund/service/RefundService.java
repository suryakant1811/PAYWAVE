package com.paywave.refund.service;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.ledger.PlatformAccounts;
import com.paywave.common.messaging.EventTypes;
import com.paywave.common.money.Money;
import com.paywave.refund.client.RefundClients;
import com.paywave.refund.domain.Refund;
import com.paywave.refund.domain.RefundStatus;
import com.paywave.refund.repo.RefundRepository;
import com.paywave.web.outbox.OutboxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RefundService {
    private static final Logger log = LoggerFactory.getLogger(RefundService.class);
    private final RefundRepository refunds;
    private final RefundClients clients;
    private final OutboxWriter outbox;
    private final BigDecimal autoApproveThreshold;

    public RefundService(RefundRepository refunds, RefundClients clients, OutboxWriter outbox,
                         @Value("${paywave.refunds.auto-approve-threshold:5000.00}") BigDecimal autoApproveThreshold) {
        this.refunds = refunds;
        this.clients = clients;
        this.outbox = outbox;
        this.autoApproveThreshold = autoApproveThreshold;
    }

    public Refund request(UUID requesterId, UUID paymentId, BigDecimal rawAmount, String reason, String idempotencyKey, boolean admin) {
        Refund existing = refunds.findByRequesterIdAndIdempotencyKey(requesterId, idempotencyKey).orElse(null);
        if (existing != null) {
            if (existing.getAmount().compareTo(Money.normalize(rawAmount)) != 0) {
                throw new PaywaveException(ErrorCode.DUPLICATE_REQUEST, "Idempotency key was already used with a different amount");
            }
            return existing.getStatus() == RefundStatus.REQUESTED ? existing : existing;
        }
        RefundClients.PaymentSnapshot payment = clients.payment(paymentId);
        authorize(requesterId, payment, admin);
        if (payment.settled()) throw new PaywaveException(ErrorCode.INVALID_STATE, "Settled payments cannot be refunded");
        if (!"SUCCESS".equals(payment.status()) && !"PARTIALLY_REFUNDED".equals(payment.status())) {
            throw new PaywaveException(ErrorCode.INVALID_STATE, "Only a successful payment can be refunded");
        }
        BigDecimal amount = Money.normalize(rawAmount);
        BigDecimal remaining = payment.amount().subtract(payment.refundedAmount());
        if (amount.compareTo(remaining) > 0) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "Refund exceeds the remaining amount of " + remaining);
        }
        BigDecimal fee = Money.proportional(amount, payment.amount(), payment.feeAmount());
        BigDecimal net = Money.scale(amount.subtract(fee));
        UUID notify = payment.payerId();
        if ("MERCHANT".equals(payment.payeeType())) {
            notify = clients.merchant(payment.payeeId()).ownerUserId();
        }
        Refund refund;
        try {
            refund = refunds.saveAndFlush(Refund.request(paymentId, requesterId, idempotencyKey, amount, fee, net, reason,
                    payment.type(), payment.payerId(), payment.payeeId(), notify));
        } catch (DataIntegrityViolationException ex) {
            return refunds.findByRequesterIdAndIdempotencyKey(requesterId, idempotencyKey).orElseThrow(() -> ex);
        }
        if (amount.compareTo(autoApproveThreshold) <= 0) {
            return process(refund.getId());
        }
        return refund;
    }

    public Refund approve(UUID refundId, UUID actorId, boolean admin) {
        Refund refund = require(refundId);
        if (!admin && !actorId.equals(refund.getPayerId()) && (refund.getNotifyUserId() == null || !actorId.equals(refund.getNotifyUserId()))) {
            throw new PaywaveException(ErrorCode.FORBIDDEN, "You cannot approve this refund");
        }
        if (refund.getStatus() != RefundStatus.REQUESTED) return refund;
        refund.setStatus(RefundStatus.APPROVED);
        refunds.save(refund);
        return process(refundId);
    }

    @Transactional
    public Refund reject(UUID refundId, UUID actorId, boolean admin, String reason) {
        Refund refund = refunds.findByIdForUpdate(refundId).orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Refund not found"));
        if (!admin && (refund.getNotifyUserId() == null || !actorId.equals(refund.getNotifyUserId()))) {
            throw new PaywaveException(ErrorCode.FORBIDDEN, "You cannot reject this refund");
        }
        if (refund.getStatus() != RefundStatus.REQUESTED) return refund;
        refund.setStatus(RefundStatus.REJECTED);
        refund.setFailureReason(reason);
        return refund;
    }

    public Refund get(UUID id) { return require(id); }

    private Refund process(UUID refundId) {
        Refund refund = require(refundId);
        if (refund.getStatus() == RefundStatus.SUCCESS || refund.getStatus() == RefundStatus.REJECTED) return refund;
        try {
            moveMoney(refund);
            clients.applyRefund(refund.getPaymentId(), refund.getId(), refund.getAmount());
            clients.history(history(refund));
            refund.setStatus(RefundStatus.SUCCESS);
            refunds.save(refund);
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("refundId", refund.getId());
            event.put("paymentId", refund.getPaymentId());
            event.put("amount", refund.getAmount());
            event.put("currency", "INR");
            event.put("userIds", notify(refund));
            outbox.enqueue("Refund", refund.getId(), EventTypes.REFUND_COMPLETED, event);
            return refund;
        } catch (RuntimeException ex) {
            log.warn("Refund {} failed: {}", refund.getId(), ex.getMessage());
            refund.setStatus(RefundStatus.FAILED);
            refund.setFailureReason(ex.getMessage());
            refunds.save(refund);
            if (ex instanceof PaywaveException paywaveException) throw paywaveException;
            throw new PaywaveException(ErrorCode.UPSTREAM, "Refund could not be completed");
        }
    }

    private void moveMoney(Refund refund) {
        String key = "refund:" + refund.getId();
        switch (refund.getPaymentType()) {
            case "TOPUP" -> {
                clients.wallet("/internal/wallets/debit", refund.getPayerId(), "CUSTOMER", refund.getAmount(), key + ":debit", "REFUND", refund.getId(), refund.getPayerId());
                clients.journal(key + ":ledger", refund.getId(), "Top-up refund", List.of(
                        RefundClients.line(refund.getPayerId(), PlatformAccounts.CUSTOMER_WALLET, "DEBIT", refund.getAmount()),
                        RefundClients.line(PlatformAccounts.GATEWAY_CLEARING, PlatformAccounts.GATEWAY_CLEARING_TYPE, "CREDIT", refund.getAmount())));
            }
            case "TRANSFER" -> {
                clients.wallet("/internal/wallets/debit", refund.getPayeeId(), "CUSTOMER", refund.getAmount(), key + ":debit", "REFUND", refund.getId(), refund.getPayeeId());
                try {
                    clients.journal(key + ":ledger", refund.getId(), "Transfer refund", List.of(
                            RefundClients.line(refund.getPayeeId(), PlatformAccounts.CUSTOMER_WALLET, "DEBIT", refund.getAmount()),
                            RefundClients.line(refund.getPayerId(), PlatformAccounts.CUSTOMER_WALLET, "CREDIT", refund.getAmount())));
                    clients.wallet("/internal/wallets/credit", refund.getPayerId(), "CUSTOMER", refund.getAmount(), key + ":credit", "REFUND", refund.getId(), refund.getPayerId());
                } catch (RuntimeException ex) {
                    clients.wallet("/internal/wallets/credit", refund.getPayeeId(), "CUSTOMER", refund.getAmount(), key + ":debit:rev", "REFUND_REVERSAL", refund.getId(), refund.getPayeeId());
                    throw ex;
                }
            }
            case "MERCHANT" -> {
                if (refund.getNetComponent().compareTo(BigDecimal.ZERO) > 0) {
                    clients.wallet("/internal/wallets/debit", refund.getPayeeId(), "MERCHANT", refund.getNetComponent(), key + ":debit", "REFUND", refund.getId(), refund.getNotifyUserId());
                }
                try {
                    List<Map<String, Object>> lines = new ArrayList<>();
                    if (refund.getNetComponent().compareTo(BigDecimal.ZERO) > 0) {
                        lines.add(RefundClients.line(refund.getPayeeId(), PlatformAccounts.MERCHANT_PAYABLE, "DEBIT", refund.getNetComponent()));
                    }
                    if (refund.getFeeComponent().compareTo(BigDecimal.ZERO) > 0) {
                        lines.add(RefundClients.line(PlatformAccounts.FEE_INCOME, PlatformAccounts.FEE_INCOME_TYPE, "DEBIT", refund.getFeeComponent()));
                    }
                    lines.add(RefundClients.line(refund.getPayerId(), PlatformAccounts.CUSTOMER_WALLET, "CREDIT", refund.getAmount()));
                    clients.journal(key + ":ledger", refund.getId(), "Merchant refund", lines);
                    clients.wallet("/internal/wallets/credit", refund.getPayerId(), "CUSTOMER", refund.getAmount(), key + ":credit", "REFUND", refund.getId(), refund.getPayerId());
                } catch (RuntimeException ex) {
                    if (refund.getNetComponent().compareTo(BigDecimal.ZERO) > 0) {
                        clients.wallet("/internal/wallets/credit", refund.getPayeeId(), "MERCHANT", refund.getNetComponent(), key + ":debit:rev", "REFUND_REVERSAL", refund.getId(), refund.getNotifyUserId());
                    }
                    throw ex;
                }
            }
            default -> throw new PaywaveException(ErrorCode.INVALID_STATE, "Unsupported payment type");
        }
    }

    private List<Map<String, Object>> history(Refund refund) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(entry(refund, refund.getPayerId(), "CREDIT", refund.getAmount(), "Refund received"));
        BigDecimal debitAmount = "MERCHANT".equals(refund.getPaymentType()) ? refund.getNetComponent() : refund.getAmount();
        if (!"TOPUP".equals(refund.getPaymentType()) && debitAmount.compareTo(BigDecimal.ZERO) > 0) {
            rows.add(entry(refund, refund.getPayeeId(), "DEBIT", debitAmount, "Refund sent"));
        }
        return rows;
    }

    private Map<String, Object> entry(Refund refund, UUID ownerId, String direction, BigDecimal amount, String description) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("paymentId", refund.getId());
        row.put("ownerId", ownerId);
        row.put("counterpartyId", refund.getPaymentId());
        row.put("counterpartyName", "Refund");
        row.put("direction", direction);
        row.put("type", "REFUND");
        row.put("amount", amount);
        row.put("status", "SUCCESS");
        row.put("description", description);
        row.put("referenceType", "REFUND");
        return row;
    }

    private List<UUID> notify(Refund refund) {
        if (refund.getNotifyUserId() != null && !refund.getNotifyUserId().equals(refund.getPayerId())) {
            return List.of(refund.getPayerId(), refund.getNotifyUserId());
        }
        return List.of(refund.getPayerId());
    }

    private void authorize(UUID requesterId, RefundClients.PaymentSnapshot payment, boolean admin) {
        if (admin || requesterId.equals(payment.payerId())) return;
        if ("MERCHANT".equals(payment.payeeType())) {
            RefundClients.MerchantSnapshot merchant = clients.merchant(payment.payeeId());
            if (requesterId.equals(merchant.ownerUserId())) return;
        }
        throw new PaywaveException(ErrorCode.FORBIDDEN, "You cannot refund this payment");
    }

    private Refund require(UUID id) {
        return refunds.findById(id).orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Refund not found"));
    }
}
