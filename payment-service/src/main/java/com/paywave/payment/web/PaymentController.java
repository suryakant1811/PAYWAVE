package com.paywave.payment.web;

import com.paywave.common.api.ApiResponse;
import com.paywave.payment.service.PaymentService;
import com.paywave.web.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Add money through the simulated gateway. Amounts ending .13 decline and .14 time out.")
    @PostMapping("/api/v1/payments/topup")
    public ApiResponse<PaymentView> topup(@Valid @RequestBody TopupRequest request) {
        return ApiResponse.ok(payments.topup(SecurityUtils.currentUserId(), request.amount(), request.idempotencyKey(), request.note()));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Transfer money to another customer")
    @PostMapping("/api/v1/payments/transfer")
    public ApiResponse<PaymentView> transfer(@Valid @RequestBody TransferRequest request) {
        return ApiResponse.ok(payments.transfer(SecurityUtils.currentUserId(), request.toUserId(), request.amount(), request.idempotencyKey(), request.note()));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Pay a merchant. A 1.5% fee is withheld from the merchant credit.")
    @PostMapping("/api/v1/payments/merchant")
    public ApiResponse<PaymentView> merchant(@Valid @RequestBody MerchantPayRequest request) {
        return ApiResponse.ok(payments.payMerchant(SecurityUtils.currentUserId(), request.merchantId(), request.amount(), request.idempotencyKey(), request.note()));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get a payment")
    @GetMapping("/api/v1/payments/{id}")
    public ApiResponse<PaymentView> get(@PathVariable UUID id) {
        return ApiResponse.ok(payments.get(SecurityUtils.currentUserId(), id));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Resume a pending payment. Wallet and ledger calls are idempotent.")
    @PostMapping("/api/v1/payments/{id}/reconcile")
    public ApiResponse<PaymentView> reconcile(@PathVariable UUID id) {
        return ApiResponse.ok(payments.reconcile(SecurityUtils.currentUserId(), id));
    }

    @GetMapping("/internal/payments/{id}")
    public ApiResponse<PaymentView> internalGet(@PathVariable UUID id) {
        return ApiResponse.ok(PaymentView.from(payments.getInternal(id)));
    }

    @PostMapping("/internal/payments/{id}/refunds")
    public ApiResponse<PaymentView> applyRefund(@PathVariable UUID id, @RequestBody RefundBody body) {
        return ApiResponse.ok(PaymentView.from(payments.applyRefund(id, body.refundId(), body.amount())));
    }

    @GetMapping("/internal/payments/unsettled")
    public ApiResponse<List<PaymentView>> unsettled(@RequestParam UUID merchantId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate through) {
        return ApiResponse.ok(payments.unsettled(merchantId, through).stream().map(PaymentView::from).toList());
    }

    @PostMapping("/internal/payments/settlements")
    public ApiResponse<Map<String, Integer>> settle(@RequestBody SettleBody body) {
        return ApiResponse.ok(Map.of("updated", payments.markSettled(body.paymentIds(), body.settlementId())));
    }

    public record RefundBody(UUID refundId, BigDecimal amount) {}
    public record SettleBody(UUID settlementId, List<UUID> paymentIds) {}
}
