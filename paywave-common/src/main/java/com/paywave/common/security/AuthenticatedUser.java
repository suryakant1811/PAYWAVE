package com.paywave.common.security;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, Set<String> roles) {
}
