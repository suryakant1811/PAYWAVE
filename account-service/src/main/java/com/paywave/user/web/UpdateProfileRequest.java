package com.paywave.user.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 120) String fullName,
        LocalDate dateOfBirth,
        @Size(max = 255) String addressLine,
        @Size(max = 80) String city,
        @Size(max = 16) String postalCode
) {
}
