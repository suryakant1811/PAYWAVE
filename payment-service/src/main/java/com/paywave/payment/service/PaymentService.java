package com.paywave.payment.service;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.ledger.PlatformAccounts;
import com.paywave.common.money.Money;
import com.paywave.payment.client.LedgerClient;
import com.paywave.payment.client.MerchantClient;
import com.paywave.payment.client.TransactionClient;
import com.paywave.payment.client.WalletClient;
import com.paywave.payment.domain.PayeeType;
import com.paywave.payment.domain.Payment;
import com.paywave.payment.domain.PaymentStatus;
import com.paywave.payment.domain.PaymentType;
import com.paywave.payment.web.PaymentView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentStore store;
    private final GatewaySimulator gateway;
    private final WalletClient wallets;
    private final LedgerClient ledger;
    private final TransactionClient transactions;
    private final MerchantClient merchants;
    private final PaymentProperties properties;

    public PaymentService(PaymentStore store, GatewaySimulator gateway, WalletClient wallets, LedgerClient ledger,
                          TransactionClient transactions, MerchantClient merchants, PaymentProperties properties) {
        this.store = store;
        this.gateway = gateway;
        this.wallets = wallets;
        this.ledger = ledger;
        this.transactions = transactions;
        this.merchants = merchants;
        this.properties = properties;
    }

    public PaymentView topup(UUID payerId, BigDecimal rawAmount, String idempotencyKey, String note) {
        BigDecimal amount = checkedAmount(rawAmount);
        Payment payment = store.createIfAbsent(payerId, idempotencyKey, payerId, PayeeType.PLATFORM, PaymentType.TOPUP,
                amount, Money.zero(), amount, note, payerId, "Add money");
        requireSameAmount(payment, amount);
        if (!store.claim(payment.getId())) {
            return finish(store.get(payment.getId()));
        }
        GatewaySimulator.Result result = gateway.charge(amount);
        store.saveGateway(payment.getId(), result.reference(), result.reason());
        store.step(payment.getId(), "GATEWAY", result.status().name(), result.reason());
        if (result.status() == GatewaySimulator.Status.FAILED) {
            return PaymentView.from(store.markFailed(payment.getId(), result.reason(), event(store.get(payment.getId()))));
        }
        if (result.status() == GatewaySimulator.Status.TIMEOUT) {
            return PaymentView.from(store.get(payment.getId()));
        }
        return completeMoney(store.get(payment.getId()));
    }

    public PaymentView transfer(UUID payerId, UUID payeeId, BigDecimal rawAmount, String idempotencyKey, String note) {
        if (payerId.equals(payeeId)) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "You cannot transfer money to yourself");
        }
        BigDecimal amount = checkedAmount(rawAmount);
        Payment payment = store.createIfAbsent(payerId, idempotencyKey, payeeId, PayeeType.CUSTOMER, PaymentType.TRANSFER,
                amount, Money.zero(), amount, note, payeeId, "Customer");
        requireSameAmount(payment, amount);
        if (!store.claim(payment.getId())) {
            return finish(store.get(payment.getId()));
        }
        return completeMoney(store.get(payment.getId()));
    }

    public PaymentView payMerchant(UUID payerId, UUID merchantId, BigDecimal rawAmount, String idempotencyKey, String note) {
        BigDecimal amount = checkedAmount(rawAmount);
        MerchantClient.MerchantView merchant = merchants.get(merchantId);
        BigDecimal fee = Money.fee(amount, properties.getMerchantFeeRate());
        BigDecimal net = Money.scale(amount.subtract(fee));
        if (net.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "Amount is too small after the merchant fee");
        }
        Payment payment = store.createIfAbsent(payerId, idempotencyKey, merchantId, PayeeType.MERCHANT, PaymentType.MERCHANT,
                amount, fee, net, note, merchant.ownerUserId(), merchant.businessName());
        requireSameAmount(payment, amount);
        if (!store.claim(payment.getId())) {
            return finish(store.get(payment.getId()));
        }
        return completeMoney(store.get(payment.getId()));
    }

    public PaymentView get(UUID requesterId, UUID paymentId) {
        Payment payment = store.get(paymentId);
        if (!payment.getPayerId().equals(requesterId) && !payment.getPayeeId().equals(requesterId)) {
            throw new PaywaveException(ErrorCode.FORBIDDEN, "You cannot view this payment");
        }
        return finish(payment);
    }

    public PaymentView reconcile(UUID requesterId, UUID paymentId) {
        Payment payment = store.get(paymentId);
        if (!payment.getPayerId().equals(requesterId)) {
            throw new PaywaveException(ErrorCode.FORBIDDEN, "Only the payer can reconcile this payment");
        }
        if (payment.getStatus() == PaymentStatus.CREATED && store.claim(payment.getId())) {
            payment = store.get(payment.getId());
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return finish(payment);
        }
        if (payment.getType() == PaymentType.TOPUP && payment.getGatewayReference() == null) {
            if ("SIMULATED_TIMEOUT".equals(payment.getFailureReason())) {
                return PaymentView.from(payment);
            }
            GatewaySimulator.Result result = gateway.charge(payment.getAmount());
            store.saveGateway(payment.getId(), result.reference(), result.reason());
            if (result.status() == GatewaySimulator.Status.FAILED) {
                return PaymentView.from(store.markFailed(payment.getId(), result.reason(), event(store.get(payment.getId()))));
            }
            if (result.status() == GatewaySimulator.Status.TIMEOUT) {
                return PaymentView.from(store.get(payment.getId()));
            }
        }
        return completeMoney(store.get(payment.getId()));
    }

    public Payment getInternal(UUID id) {
        return store.get(id);
    }

    public Payment applyRefund(UUID paymentId, UUID refundId, BigDecimal amount) {
        return store.applyRefund(paymentId, refundId, Money.normalize(amount));
    }

    public List<Payment> unsettled(UUID merchantId, LocalDate through) {
        Instant before = through.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return store.unsettled(merchantId, before);
    }

    public int markSettled(List<UUID> paymentIds, UUID settlementId) {
        return store.markSettled(paymentIds, settlementId);
    }

    private PaymentView completeMoney(Payment payment) {
        try {
            switch (payment.getType()) {
                case TOPUP -> completeTopup(payment);
                case TRANSFER -> completeTransfer(payment);
                case MERCHANT -> completeMerchant(payment);
            }
        } catch (PaywaveException ex) {
            log.warn("Payment {} failed: {}", payment.getId(), ex.getMessage());
            return PaymentView.from(store.markFailed(payment.getId(), ex.getMessage(), event(store.get(payment.getId()))));
        }
        recordHistory(store.get(payment.getId()));
        return PaymentView.from(store.markSuccess(payment.getId(), event(store.get(payment.getId()))));
    }

    private void completeTopup(Payment payment) {
        UUID journalId = ledger.post(key(payment, "ledger"), "PAYMENT", payment.getId(), "Add money", List.of(
                new LedgerClient.Line(PlatformAccounts.GATEWAY_CLEARING, PlatformAccounts.GATEWAY_CLEARING_TYPE, "DEBIT", payment.getAmount(), "INR"),
                new LedgerClient.Line(payment.getPayerId(), PlatformAccounts.CUSTOMER_WALLET, "CREDIT", payment.getAmount(), "INR")
        ));
        store.step(payment.getId(), "LEDGER_POST", "SUCCESS", journalId.toString());
        try {
            wallets.credit(mutation(payment.getPayerId(), "CUSTOMER", payment.getAmount(), key(payment, "credit"), "TOPUP", payment, payment.getPayerId()));
            store.step(payment.getId(), "WALLET_CREDIT", "SUCCESS", null);
        } catch (RuntimeException ex) {
            ledger.reverse(journalId, key(payment, "ledger:rev"), "Reverse add money");
            store.step(payment.getId(), "LEDGER_REVERSE", "SUCCESS", ex.getMessage());
            throw ex;
        }
    }

    private void completeTransfer(Payment payment) {
        wallets.debit(mutation(payment.getPayerId(), "CUSTOMER", payment.getAmount(), key(payment, "debit"), "TRANSFER", payment, payment.getPayerId()));
        store.step(payment.getId(), "WALLET_DEBIT", "SUCCESS", null);
        UUID journalId;
        try {
            journalId = ledger.post(key(payment, "ledger"), "PAYMENT", payment.getId(), "Customer transfer", List.of(
                    new LedgerClient.Line(payment.getPayerId(), PlatformAccounts.CUSTOMER_WALLET, "DEBIT", payment.getAmount(), "INR"),
                    new LedgerClient.Line(payment.getPayeeId(), PlatformAccounts.CUSTOMER_WALLET, "CREDIT", payment.getAmount(), "INR")
            ));
            store.step(payment.getId(), "LEDGER_POST", "SUCCESS", journalId.toString());
        } catch (RuntimeException ex) {
            wallets.credit(mutation(payment.getPayerId(), "CUSTOMER", payment.getAmount(), key(payment, "debit:rev"), "TRANSFER_REVERSAL", payment, payment.getPayerId()));
            store.step(payment.getId(), "WALLET_DEBIT_REVERSE", "SUCCESS", ex.getMessage());
            throw ex;
        }
        try {
            wallets.credit(mutation(payment.getPayeeId(), "CUSTOMER", payment.getAmount(), key(payment, "credit"), "TRANSFER", payment, payment.getPayeeId()));
            store.step(payment.getId(), "WALLET_CREDIT", "SUCCESS", null);
        } catch (RuntimeException ex) {
            ledger.reverse(journalId, key(payment, "ledger:rev"), "Reverse transfer");
            wallets.credit(mutation(payment.getPayerId(), "CUSTOMER", payment.getAmount(), key(payment, "debit:rev"), "TRANSFER_REVERSAL", payment, payment.getPayerId()));
            store.step(payment.getId(), "TRANSFER_COMPENSATE", "SUCCESS", ex.getMessage());
            throw ex;
        }
    }

    private void completeMerchant(Payment payment) {
        wallets.debit(mutation(payment.getPayerId(), "CUSTOMER", payment.getAmount(), key(payment, "debit"), "MERCHANT_PAYMENT", payment, payment.getPayerId()));
        store.step(payment.getId(), "WALLET_DEBIT", "SUCCESS", null);
        List<LedgerClient.Line> journal = new ArrayList<>();
        journal.add(new LedgerClient.Line(payment.getPayerId(), PlatformAccounts.CUSTOMER_WALLET, "DEBIT", payment.getAmount(), "INR"));
        journal.add(new LedgerClient.Line(payment.getPayeeId(), PlatformAccounts.MERCHANT_PAYABLE, "CREDIT", payment.getNetAmount(), "INR"));
        if (payment.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            journal.add(new LedgerClient.Line(PlatformAccounts.FEE_INCOME, PlatformAccounts.FEE_INCOME_TYPE, "CREDIT", payment.getFeeAmount(), "INR"));
        }
        UUID journalId;
        try {
            journalId = ledger.post(key(payment, "ledger"), "PAYMENT", payment.getId(), "Merchant payment", journal);
            store.step(payment.getId(), "LEDGER_POST", "SUCCESS", journalId.toString());
        } catch (RuntimeException ex) {
            wallets.credit(mutation(payment.getPayerId(), "CUSTOMER", payment.getAmount(), key(payment, "debit:rev"), "MERCHANT_PAYMENT_REVERSAL", payment, payment.getPayerId()));
            throw ex;
        }
        try {
            wallets.credit(mutation(payment.getPayeeId(), "MERCHANT", payment.getNetAmount(), key(payment, "credit"), "MERCHANT_PAYMENT", payment, payment.getNotifyUserId()));
            store.step(payment.getId(), "WALLET_CREDIT", "SUCCESS", null);
        } catch (RuntimeException ex) {
            ledger.reverse(journalId, key(payment, "ledger:rev"), "Reverse merchant payment");
            wallets.credit(mutation(payment.getPayerId(), "CUSTOMER", payment.getAmount(), key(payment, "debit:rev"), "MERCHANT_PAYMENT_REVERSAL", payment, payment.getPayerId()));
            throw ex;
        }
    }

    private void recordHistory(Payment payment) {
        if (payment.isHistoryRecorded()) {
            return;
        }
        try {
            transactions.record(history(payment));
            store.markHistory(payment.getId());
        } catch (RuntimeException ex) {
            log.error("Payment {} succeeded but history was not recorded: {}", payment.getId(), ex.getMessage());
        }
    }

    private List<TransactionClient.Entry> history(Payment payment) {
        return switch (payment.getType()) {
            case TOPUP -> List.of(entry(payment, payment.getPayerId(), payment.getPayerId(), "Add money", "CREDIT", payment.getAmount(), "Wallet top up"));
            case TRANSFER -> List.of(
                    entry(payment, payment.getPayerId(), payment.getPayeeId(), "Customer", "DEBIT", payment.getAmount(), "Transfer sent"),
                    entry(payment, payment.getPayeeId(), payment.getPayerId(), "Customer", "CREDIT", payment.getAmount(), "Transfer received"));
            case MERCHANT -> List.of(
                    entry(payment, payment.getPayerId(), payment.getPayeeId(), payment.getCounterpartyName(), "DEBIT", payment.getAmount(), "Paid " + payment.getCounterpartyName()),
                    entry(payment, payment.getPayeeId(), payment.getPayerId(), "Customer", "CREDIT", payment.getNetAmount(), "Customer payment"));
        };
    }

    private TransactionClient.Entry entry(Payment payment, UUID ownerId, UUID counterpartyId, String name, String direction, BigDecimal amount, String description) {
        return new TransactionClient.Entry(payment.getId(), ownerId, counterpartyId, name, direction, payment.getType().name(),
                amount, "SUCCESS", description, "PAYMENT");
    }

    private PaymentView finish(Payment payment) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            recordHistory(payment);
        }
        return PaymentView.from(store.get(payment.getId()));
    }

    private WalletClient.Mutation mutation(UUID ownerId, String ownerType, BigDecimal amount, String key, String referenceType, Payment payment, UUID notifyUserId) {
        return new WalletClient.Mutation(ownerId, ownerType, amount, key, referenceType, payment.getId(), "INR", notifyUserId);
    }

    private Map<String, Object> event(Payment payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("payerId", payment.getPayerId());
        payload.put("payeeId", payment.getPayeeId());
        payload.put("type", payment.getType().name());
        payload.put("amount", payment.getAmount());
        payload.put("currency", "INR");
        payload.put("userIds", userIds(payment));
        return payload;
    }

    private List<UUID> userIds(Payment payment) {
        if (payment.getType() == PaymentType.TOPUP) {
            return List.of(payment.getPayerId());
        }
        if (payment.getNotifyUserId() != null && !payment.getNotifyUserId().equals(payment.getPayerId())) {
            return List.of(payment.getPayerId(), payment.getNotifyUserId());
        }
        if (payment.getPayeeType() == PayeeType.CUSTOMER) {
            return List.of(payment.getPayerId(), payment.getPayeeId());
        }
        return List.of(payment.getPayerId());
    }

    private BigDecimal checkedAmount(BigDecimal raw) {
        BigDecimal amount = Money.normalize(raw);
        if (amount.compareTo(properties.getMinAmount()) < 0 || amount.compareTo(properties.getMaxAmount()) > 0) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "Amount must be between " + properties.getMinAmount() + " and " + properties.getMaxAmount());
        }
        return amount;
    }

    private void requireSameAmount(Payment payment, BigDecimal amount) {
        if (payment.getAmount().compareTo(amount) != 0) {
            throw new PaywaveException(ErrorCode.DUPLICATE_REQUEST, "Idempotency key was already used with a different amount");
        }
    }

    static String key(Payment payment, String step) {
        return "payment:" + payment.getId() + ":" + step;
    }
}
