package com.paywave.common.security;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService("dev-only-paywave-jwt-secret-change-me-32b", 15);

    @Test
    void roundTripsAccessToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.createAccessToken(userId, "a@paywave.test", Set.of("CUSTOMER"));
        AuthenticatedUser user = jwtService.parseAccessToken(token);
        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.email()).isEqualTo("a@paywave.test");
        assertThat(user.roles()).containsExactly("CUSTOMER");
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 15)).isInstanceOf(IllegalArgumentException.class);
    }
}
