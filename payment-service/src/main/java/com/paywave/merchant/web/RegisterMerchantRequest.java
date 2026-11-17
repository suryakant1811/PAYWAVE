package com.paywave.merchant.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterMerchantRequest(
        @NotBlank @Size(max = 160) String businessName,
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Email String contactEmail,
        @NotBlank @Pattern(regexp = "^[0-9]{8,20}$") String settlementAccountNumber
) {}
