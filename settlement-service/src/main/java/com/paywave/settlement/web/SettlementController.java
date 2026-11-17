package com.paywave.settlement.web;

import com.paywave.common.api.ApiResponse;
import com.paywave.settlement.client.SettlementClients;
import com.paywave.settlement.domain.Settlement;
import com.paywave.settlement.domain.SettlementLine;
import com.paywave.settlement.service.SettlementService;
import com.paywave.web.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import java.util.UUID;

@RestController
public class SettlementController {
    private final SettlementService settlements;
    private final SettlementClients clients;
    public SettlementController(SettlementService settlements, SettlementClients clients) {
        this.settlements = settlements;
        this.clients = clients;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Calculate and pay out unsettled merchant funds through the period date")
    @PostMapping("/api/v1/settlements/run")
    public ApiResponse<SettlementResponse> run(@Valid @RequestBody RunRequest request) {
        return ApiResponse.ok(SettlementResponse.from(settlements.run(SecurityUtils.currentUserId(), request.periodDate(),
                SecurityUtils.hasRole("ADMIN"), request.merchantId())));
    }

    @GetMapping("/api/v1/settlements")
    public ApiResponse<List<SettlementResponse>> list() {
        UUID merchantId = clients.merchantForOwner(SecurityUtils.currentUserId()).merchantId();
        return ApiResponse.ok(settlements.history(merchantId).stream().map(SettlementResponse::from).toList());
    }

    @GetMapping("/api/v1/settlements/{id}")
    public ApiResponse<SettlementResponse> get(@PathVariable UUID id) {
        Settlement settlement = owned(id);
        return ApiResponse.ok(SettlementResponse.from(settlement));
    }

    @GetMapping("/api/v1/settlements/{id}/report")
    public ApiResponse<Report> report(@PathVariable UUID id) {
        Settlement settlement = owned(id);
        return ApiResponse.ok(new Report(SettlementResponse.from(settlement),
                settlements.reportLines(id).stream().map(Line::from).toList()));
    }

    private Settlement owned(UUID id) {
        Settlement settlement = settlements.get(id);
        if (!SecurityUtils.hasRole("ADMIN")) {
            UUID merchantId = clients.merchantForOwner(SecurityUtils.currentUserId()).merchantId();
            if (!settlement.getMerchantId().equals(merchantId)) {
                throw new com.paywave.common.error.PaywaveException(com.paywave.common.error.ErrorCode.FORBIDDEN, "You cannot view this settlement");
            }
        }
        return settlement;
    }

    public record RunRequest(@NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodDate, UUID merchantId) {}
    public record SettlementResponse(UUID settlementId, UUID merchantId, LocalDate periodDate, BigDecimal grossAmount,
                                     BigDecimal feeAmount, BigDecimal refundAmount, BigDecimal netAmount, String status, String failureReason) {
        static SettlementResponse from(Settlement settlement) {
            return new SettlementResponse(settlement.getId(), settlement.getMerchantId(), settlement.getPeriodDate(),
                    settlement.getGrossAmount(), settlement.getFeeAmount(), settlement.getRefundAmount(), settlement.getNetAmount(),
                    settlement.getStatus().name(), settlement.getFailureReason());
        }
    }
    public record Line(UUID paymentId, BigDecimal grossAmount, BigDecimal feeAmount, BigDecimal refundAmount, BigDecimal netAmount) {
        static Line from(SettlementLine line) {
            return new Line(line.getPaymentId(), line.getGrossAmount(), line.getFeeAmount(), line.getRefundAmount(), line.getNetAmount());
        }
    }
    public record Report(SettlementResponse settlement, List<Line> lines) {}
}
