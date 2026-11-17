package com.paywave.auth.web;

import com.paywave.auth.domain.UserAccount;
import com.paywave.auth.service.AuthService;
import com.paywave.common.api.ApiResponse;
import com.paywave.web.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Register a customer, profile, and wallet", security = {})
    @PostMapping("/api/v1/auth/register")
    public org.springframework.http.ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.register(request)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Login with email and password", security = {})
    @PostMapping("/api/v1/auth/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Rotate a refresh token", security = {})
    @PostMapping("/api/v1/auth/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Revoke a refresh token", security = {})
    @PostMapping("/api/v1/auth/logout")
    public ApiResponse<Map<String, String>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.ok(Map.of("status", "LOGGED_OUT"));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Current authenticated user")
    @GetMapping("/api/v1/auth/me")
    public ApiResponse<Map<String, Object>> me() {
        UUID userId = SecurityUtils.currentUserId();
        UserAccount user = authService.current(userId);
        return ApiResponse.ok(Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "phone", user.getPhone(),
                "roles", Set.copyOf(user.getRoles()),
                "status", user.getStatus().name()
        ));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Grant a role to a user. Service-to-service only.")
    @PostMapping("/internal/users/{id}/roles")
    public ApiResponse<Map<String, Object>> addRole(@PathVariable UUID id, @Valid @RequestBody AddRoleRequest request) {
        UserAccount user = authService.addRole(id, request.role());
        return ApiResponse.ok(Map.of("userId", user.getId(), "roles", Set.copyOf(user.getRoles())));
    }
}
