package com.paywave.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddRoleRequest(
        @NotBlank @Pattern(regexp = "MERCHANT|ADMIN") String role
) {
}
