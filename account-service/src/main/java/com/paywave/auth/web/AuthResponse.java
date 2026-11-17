package com.paywave.auth.web;

import com.paywave.auth.domain.UserAccount;
import com.paywave.auth.service.TokenService;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        Set<String> roles,
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds
) {
    public static AuthResponse from(UserAccount user, TokenService.IssuedTokens tokens) {
        return new AuthResponse(user.getId(), user.getEmail(), Set.copyOf(user.getRoles()),
                tokens.accessToken(), tokens.refreshToken(), tokens.accessTokenExpiresInSeconds());
    }
}
