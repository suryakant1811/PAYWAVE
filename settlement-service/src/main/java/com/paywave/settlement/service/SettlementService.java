package com.paywave.settlement.service;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.messaging.EventTypes;
import com.paywave.settlement.client.SettlementClients;
import com.paywave.settlement.domain.Settlement;
import com.paywave.settlement.domain.SettlementLine;
import com.paywave.settlement.domain.SettlementStatus;
import com.paywave.settlement.repo.SettlementLineRepository;
import com.paywave.settlement.repo.SettlementRepository;
import com.paywave.web.outbox.OutboxWriter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SettlementService {
    private final SettlementRepository settlements;
    private final SettlementLineRepository lines;
    private final SettlementClients clients;
    private final OutboxWriter outbox;

    public SettlementService(SettlementRepository settlements, SettlementLineRepository lines, SettlementClients clients, OutboxWriter outbox) {
        this.settlements = settlements;
        this.lines = lines;
        this.clients = clients;
        this.outbox = outbox;
    }

    public Settlement run(UUID actorId, LocalDate periodDate, boolean admin, UUID merchantId) {
        SettlementClients.MerchantSnapshot merchant = merchantId == null || !admin
                ? clients.merchantForOwner(actorId)
                : clients.merchant(merchantId);
        if (!admin && !merchant.ownerUserId().equals(actorId)) {
            throw new PaywaveException(ErrorCode.FORBIDDEN, "You can settle only your own merchant account");
        }
        if (!"ACTIVE".equals(merchant.status())) {
            throw new PaywaveException(ErrorCode.ACCOUNT_INACTIVE, "Merchant is not active");
        }
        Settlement existing = settlements.findByMerchantIdAndPeriodDate(merchant.merchantId(), periodDate).orElse(null);
        if (existing != null && existing.getStatus() == SettlementStatus.COMPLETED) {
            return existing;
        }
        List<SettlementClients.PaymentSnapshot> payments = clients.unsettled(merchant.merchantId(), periodDate);
        SettlementCalculator.Totals totals = SettlementCalculator.calculate(payments);
        Settlement settlement = existing != null ? existing : settlements.save(Settlement.start(
                merchant.merchantId(), merchant.ownerUserId(), periodDate, totals.gross(), totals.fees(), totals.refunds(), totals.net()));
        if (existing == null) {
            for (SettlementClients.PaymentSnapshot payment : payments) {
                BigDecimal refundedNet = com.paywave.common.money.Money.proportional(payment.refundedAmount(), payment.amount(), payment.netAmount());
                lines.save(SettlementLine.of(settlement.getId(), payment.paymentId(), payment.amount(), payment.feeAmount(),
                        payment.refundedAmount(), payment.netAmount().subtract(refundedNet)));
            }
        }
        try {
            if (settlement.getNetAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (!settlement.isLedgerPosted()) {
                    clients.journal("settlement:" + settlement.getId() + ":ledger", settlement.getId(), settlement.getMerchantId(), settlement.getNetAmount());
                    settlement.setLedgerPosted(true);
                    settlements.save(settlement);
                }
                if (!settlement.isWalletDebited()) {
                    clients.debitWallet(settlement.getMerchantId(), settlement.getNetAmount(), "settlement:" + settlement.getId() + ":debit",
                            settlement.getId(), settlement.getOwnerUserId());
                    settlement.setWalletDebited(true);
                    settlements.save(settlement);
                }
                clients.history(settlement.getId(), settlement.getMerchantId(), settlement.getNetAmount());
            }
            clients.markSettled(settlement.getId(), payments.stream().map(SettlementClients.PaymentSnapshot::paymentId).toList());
            settlement.setStatus(SettlementStatus.COMPLETED);
            settlement.setFailureReason(null);
            settlements.save(settlement);
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("settlementId", settlement.getId());
            event.put("merchantId", settlement.getMerchantId());
            event.put("amount", settlement.getNetAmount());
            event.put("currency", "INR");
            event.put("userIds", List.of(settlement.getOwnerUserId()));
            outbox.enqueue("Settlement", settlement.getId(), EventTypes.SETTLEMENT_COMPLETED, event);
            return settlement;
        } catch (RuntimeException ex) {
            settlement.setStatus(SettlementStatus.FAILED);
            settlement.setFailureReason(ex.getMessage());
            settlements.save(settlement);
            if (ex instanceof PaywaveException paywaveException) throw paywaveException;
            throw new PaywaveException(ErrorCode.UPSTREAM, "Settlement could not be completed");
        }
    }

    public Settlement get(UUID id) {
        return settlements.findById(id).orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Settlement not found"));
    }

    public List<Settlement> history(UUID merchantId) {
        return settlements.findByMerchantIdOrderByPeriodDateDesc(merchantId);
    }

    public List<SettlementLine> reportLines(UUID settlementId) {
        return lines.findBySettlementId(settlementId);
    }
}
