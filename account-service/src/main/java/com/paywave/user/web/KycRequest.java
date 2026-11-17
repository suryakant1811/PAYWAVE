package com.paywave.user.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KycRequest(
        @NotBlank @Size(max = 32) String documentType,
        @NotBlank @Size(min = 4, max = 32) String documentNumber
) {
}
