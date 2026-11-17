package com.paywave.refund.web;

import com.paywave.common.api.ApiResponse;
import com.paywave.refund.domain.Refund;
import com.paywave.refund.service.RefundService;
import com.paywave.web.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
public class RefundController {
    private final RefundService refunds;
    public RefundController(RefundService refunds) { this.refunds = refunds; }

    @io.swagger.v3.oas.annotations.Operation(summary = "Request a refund. Amounts up to the auto-approve threshold are processed immediately.")
    @PostMapping("/api/v1/refunds")
    public ApiResponse<RefundResponse> request(@Valid @RequestBody RefundRequest request) {
        return ApiResponse.ok(RefundResponse.from(refunds.request(SecurityUtils.currentUserId(), request.paymentId(), request.amount(),
                request.reason(), request.idempotencyKey(), SecurityUtils.hasRole("ADMIN"))));
    }

    @GetMapping("/api/v1/refunds/{id}")
    public ApiResponse<RefundResponse> get(@PathVariable UUID id) {
        Refund refund = refunds.get(id);
        UUID userId = SecurityUtils.currentUserId();
        if (!userId.equals(refund.getRequesterId()) && !userId.equals(refund.getPayerId()) && !SecurityUtils.hasRole("ADMIN")) {
            throw new com.paywave.common.error.PaywaveException(com.paywave.common.error.ErrorCode.FORBIDDEN, "You cannot view this refund");
        }
        return ApiResponse.ok(RefundResponse.from(refund));
    }

    @PostMapping("/api/v1/refunds/{id}/approve")
    public ApiResponse<RefundResponse> approve(@PathVariable UUID id) {
        return ApiResponse.ok(RefundResponse.from(refunds.approve(id, SecurityUtils.currentUserId(), SecurityUtils.hasRole("ADMIN"))));
    }

    @PostMapping("/api/v1/refunds/{id}/reject")
    public ApiResponse<RefundResponse> reject(@PathVariable UUID id, @RequestBody java.util.Map<String, String> body) {
        return ApiResponse.ok(RefundResponse.from(refunds.reject(id, SecurityUtils.currentUserId(), SecurityUtils.hasRole("ADMIN"), body.getOrDefault("reason", "Rejected"))));
    }

    public record RefundRequest(@NotNull UUID paymentId, @NotNull BigDecimal amount, @NotBlank @Size(max = 255) String reason,
                                @NotBlank @Size(max = 80) String idempotencyKey) {}

    public record RefundResponse(UUID refundId, UUID paymentId, BigDecimal amount, String status, String reason, String failureReason, Instant createdAt) {
        static RefundResponse from(Refund refund) {
            return new RefundResponse(refund.getId(), refund.getPaymentId(), refund.getAmount(), refund.getStatus().name(),
                    refund.getReason(), refund.getFailureReason(), refund.getCreatedAt());
        }
    }
}
