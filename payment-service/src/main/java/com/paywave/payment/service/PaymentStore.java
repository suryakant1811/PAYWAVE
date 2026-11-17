package com.paywave.payment.service;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.messaging.EventTypes;
import com.paywave.common.money.Money;
import com.paywave.payment.domain.PayeeType;
import com.paywave.payment.domain.Payment;
import com.paywave.payment.domain.PaymentRefund;
import com.paywave.payment.domain.PaymentStates;
import com.paywave.payment.domain.PaymentStatus;
import com.paywave.payment.domain.PaymentType;
import com.paywave.payment.domain.SagaStep;
import com.paywave.payment.repo.PaymentRefundRepository;
import com.paywave.payment.repo.PaymentRepository;
import com.paywave.payment.repo.SagaStepRepository;
import com.paywave.web.outbox.OutboxWriter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentStore {

    private final PaymentRepository payments;
    private final SagaStepRepository steps;
    private final PaymentRefundRepository refunds;
    private final OutboxWriter outboxWriter;

    public PaymentStore(PaymentRepository payments, SagaStepRepository steps, PaymentRefundRepository refunds, OutboxWriter outboxWriter) {
        this.payments = payments;
        this.steps = steps;
        this.refunds = refunds;
        this.outboxWriter = outboxWriter;
    }

    @Transactional
    public Payment createIfAbsent(UUID payerId, String idempotencyKey, UUID payeeId, PayeeType payeeType, PaymentType type,
                                  BigDecimal amount, BigDecimal fee, BigDecimal net, String note, UUID notifyUserId, String counterpartyName) {
        return payments.findByPayerIdAndIdempotencyKey(payerId, idempotencyKey).orElseGet(() -> {
            try {
                return payments.saveAndFlush(Payment.create(payerId, idempotencyKey, payeeId, payeeType, type, amount, fee, net, note, notifyUserId, counterpartyName));
            } catch (DataIntegrityViolationException ex) {
                return payments.findByPayerIdAndIdempotencyKey(payerId, idempotencyKey).orElseThrow(() -> ex);
            }
        });
    }

    @Transactional
    public boolean claim(UUID id) {
        Payment payment = lock(id);
        if (payment.getStatus() != PaymentStatus.CREATED) {
            return false;
        }
        PaymentStates.assertCanMove(payment.getStatus(), PaymentStatus.PENDING);
        payment.setStatus(PaymentStatus.PENDING);
        return true;
    }

    @Transactional(readOnly = true)
    public Payment get(UUID id) {
        return payments.findById(id).orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Payment not found"));
    }

    @Transactional
    public Payment saveGateway(UUID id, String reference, String failureReason) {
        Payment payment = lock(id);
        if (reference != null) {
            payment.setGatewayReference(reference);
        }
        if (failureReason != null) {
            payment.setFailureReason(failureReason);
        }
        return payment;
    }

    @Transactional
    public Payment markFailed(UUID id, String reason, Map<String, Object> event) {
        Payment payment = lock(id);
        if (payment.getStatus() == PaymentStatus.FAILED) {
            return payment;
        }
        PaymentStates.assertCanMove(payment.getStatus(), PaymentStatus.FAILED);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(trim(reason));
        outboxWriter.enqueue("Payment", id, EventTypes.PAYMENT_FAILED, event);
        return payment;
    }

    @Transactional
    public Payment markSuccess(UUID id, Map<String, Object> event) {
        Payment payment = lock(id);
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }
        PaymentStates.assertCanMove(payment.getStatus(), PaymentStatus.SUCCESS);
        payment.setStatus(PaymentStatus.SUCCESS);
        outboxWriter.enqueue("Payment", id, EventTypes.PAYMENT_COMPLETED, event);
        return payment;
    }

    @Transactional
    public void step(UUID paymentId, String name, String status, String detail) {
        steps.save(SagaStep.of(paymentId, name, status, detail));
    }

    @Transactional
    public void markHistory(UUID id) {
        lock(id).setHistoryRecorded(true);
    }

    @Transactional
    public Payment applyRefund(UUID paymentId, UUID refundId, BigDecimal amount) {
        if (refunds.existsById(refundId)) {
            return get(paymentId);
        }
        Payment payment = lock(paymentId);
        if (payment.isSettled()) {
            throw new PaywaveException(ErrorCode.INVALID_STATE, "Settled payments cannot be refunded");
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new PaywaveException(ErrorCode.INVALID_STATE, "Only successful payments can be refunded");
        }
        BigDecimal next = payment.getRefundedAmount().add(amount);
        if (next.compareTo(payment.getAmount()) > 0) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "Refund exceeds the remaining payment amount");
        }
        PaymentStatus target = next.compareTo(payment.getAmount()) == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        PaymentStates.assertCanMove(payment.getStatus(), target);
        payment.setRefundedAmount(Money.scale(next));
        payment.setStatus(target);
        refunds.save(PaymentRefund.of(refundId, paymentId, amount));
        return payment;
    }

    @Transactional(readOnly = true)
    public List<Payment> unsettled(UUID merchantId, Instant before) {
        return payments.findUnsettled(merchantId, PaymentType.MERCHANT, List.of(PaymentStatus.SUCCESS, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED), before);
    }

    @Transactional
    public int markSettled(Collection<UUID> paymentIds, UUID settlementId) {
        int count = 0;
        for (UUID id : paymentIds) {
            Payment payment = lock(id);
            if (!payment.isSettled()) {
                payment.setSettled(true);
                payment.setSettlementId(settlementId);
                payment.setSettledAt(Instant.now());
                count++;
            }
        }
        return count;
    }

    private Payment lock(UUID id) {
        return payments.findByIdForUpdate(id).orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Payment not found"));
    }

    private String trim(String reason) {
        if (reason == null) {
            return "FAILED";
        }
        return reason.substring(0, Math.min(reason.length(), 255));
    }
}
