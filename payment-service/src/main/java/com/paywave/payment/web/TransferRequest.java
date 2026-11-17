package com.paywave.payment.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(@NotNull UUID toUserId, @NotNull BigDecimal amount, @NotBlank @Size(max = 80) String idempotencyKey, @Size(max = 255) String note) {
}
