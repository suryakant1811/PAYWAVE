package com.paywave.user.web;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.user.domain.AccountStatus;
import com.paywave.user.service.UserProfileService;
import com.paywave.web.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class UserController {

    private final UserProfileService profiles;

    public UserController(UserProfileService profiles) {
        this.profiles = profiles;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get the authenticated customer profile")
    @GetMapping("/api/v1/users/me")
    public ApiResponse<ProfileResponse> me() {
        return ApiResponse.ok(profiles.get(SecurityUtils.currentUserId()));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Update name and address. Email and phone are fixed at registration.")
    @PutMapping("/api/v1/users/me")
    public ApiResponse<ProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(profiles.update(SecurityUtils.currentUserId(), request));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Simulated KYC. A 12+ character alphanumeric document is verified.")
    @PostMapping("/api/v1/users/me/kyc")
    public ApiResponse<ProfileResponse> kyc(@Valid @RequestBody KycRequest request) {
        return ApiResponse.ok(profiles.submitKyc(SecurityUtils.currentUserId(), request));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get a customer profile. Self or ADMIN.")
    @GetMapping("/api/v1/users/{id}")
    public ApiResponse<ProfileResponse> get(@PathVariable UUID id) {
        requireSelfOrAdmin(id);
        return ApiResponse.ok(profiles.get(id));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Change account status. ADMIN only.")
    @PatchMapping("/api/v1/users/{id}/status")
    public ApiResponse<ProfileResponse> status(@PathVariable UUID id, @RequestBody java.util.Map<String, String> body) {
        SecurityUtils.requireRole("ADMIN");
        AccountStatus status = AccountStatus.valueOf(body.getOrDefault("status", ""));
        return ApiResponse.ok(profiles.updateStatus(id, status));
    }

    @PostMapping("/internal/profiles")
    public ApiResponse<ProfileResponse> create(@RequestBody ProfileRequest request) {
        if (request.userId() == null || request.fullName() == null || request.email() == null || request.phone() == null) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "userId, fullName, email and phone are required");
        }
        return ApiResponse.ok(profiles.create(request));
    }

    @GetMapping("/internal/profiles/{id}")
    public ApiResponse<ProfileResponse> internalGet(@PathVariable UUID id) {
        return ApiResponse.ok(profiles.get(id));
    }

    @DeleteMapping("/internal/profiles/{id}")
    public ApiResponse<java.util.Map<String, String>> delete(@PathVariable UUID id) {
        profiles.delete(id);
        return ApiResponse.ok(java.util.Map.of("status", "DELETED"));
    }

    private void requireSelfOrAdmin(UUID id) {
        if (!id.equals(SecurityUtils.currentUserId()) && !SecurityUtils.hasRole("ADMIN")) {
            throw new PaywaveException(ErrorCode.FORBIDDEN, "You can only view your own profile");
        }
    }
}
